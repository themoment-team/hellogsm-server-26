import * as aws from "@pulumi/aws";
import * as pulumi from "@pulumi/pulumi";
import { config } from "../config";

export interface EntranceLambdaResult {
    function: aws.lambda.Function;
    restApi: aws.apigateway.RestApi;
    stage: aws.apigateway.Stage;
}

const STAGE_NAME = "prod";

/**
 * entrance-lambda(모의 성적 계산 API)를 배포한다. `go-hellogsm-score-calculator`를 대체하며,
 * 기존 Go 함수는 이 스택에 import되어 있지 않으므로 별개 함수로 병행 운영된다
 * (전환 완료 후 기존 함수는 콘솔에서 수동 제거).
 *
 * 환경은 prod 단일이다 — stage 함수를 두지 않는다. 기존 Go 구현에도 stage가 없었고,
 * 점수 계산기는 DB를 쓰지 않는 순수 함수라 stage 서버가 이 함수를 함께 호출해도 안전하다.
 */
export function createEntranceLambda(): EntranceLambdaResult {
    const functionName = config.entranceLambdaFunctionName;

    const executionRole = new aws.iam.Role("hello-prod-entrance-lambda-role", {
        name: "hello-prod-entrance-lambda-role",
        assumeRolePolicy: JSON.stringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Effect: "Allow",
                    Principal: { Service: "lambda.amazonaws.com" },
                    Action: "sts:AssumeRole",
                },
            ],
        }),
    });

    // CloudWatch Logs 쓰기 권한만 필요하다. entrance-lambda는 persistence를 의존하지 않아
    // DB에 접근하지 않으므로 VPC에 넣지 않는다 - 넣으면 콜드 스타트만 늘고 얻는 게 없다.
    new aws.iam.RolePolicyAttachment("hello-prod-entrance-lambda-basic-execution", {
        role: executionRole.name,
        policyArn: "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole",
    });

    const entranceLambda = new aws.lambda.Function(
        "hello-prod-entrance-score-calculator",
        {
            name: functionName,
            role: executionRole.arn,
            // entrance-lambda/build.gradle.kts 가 jvmTarget=JVM_25 로 클래스 파일 버전 69를
            // 뽑으므로 java21 이하 런타임에서는 UnsupportedClassVersionError 로 즉시 실패한다.
            // @pulumi/aws 6.66 의 Runtime enum 에는 아직 java25 가 없어 문자열로 지정한다.
            runtime: "java25",
            handler: "kr.hellogsm.entrance.lambda.ScoreCalculatorHandler::handleRequest",
            // shadow jar 에 네이티브 의존성이 없어 arm64 로 동작하며 x86_64 보다 저렴하다.
            architectures: ["arm64"],
            // JVM 콜드 스타트를 고려한 값. 기존 Go 함수 기준(128MB)을 그대로 쓰면 안 된다.
            memorySize: config.entranceLambdaMemoryMb,
            // REST API Gateway 의 통합 타임아웃 상한이 29초다.
            timeout: config.entranceLambdaTimeoutSeconds,
            environment: {
                // 핸들러 생성자가 이 값을 읽어 없으면 error() 로 즉시 죽는다(401 이 아니라 초기화 실패).
                variables: { X_HG_INTERNAL_API_KEY: config.entranceLambdaApiKey },
            },
            // 실제 코드는 entrance-lambda-prod-cd.yml 이 update-function-code 로 올린다.
            // Pulumi 는 함수 "껍데기"만 만들고, 여기 둔 placeholder 는 최초 생성을 통과시키기
            // 위한 더미다. 이것만으로는 핸들러가 로드되지 않으므로 생성 직후 CD 를 한 번
            // 돌려야 한다.
            code: new pulumi.asset.AssetArchive({
                "placeholder.txt": new pulumi.asset.StringAsset(
                    "Placeholder. 실제 코드는 GitHub Actions(entrance-lambda-prod-cd.yml)가 배포한다.\n",
                ),
            }),
        },
        {
            // 이 두 속성을 무시하지 않으면 pulumi up 이 CD 가 올린 최신 코드를 위 placeholder 로
            // 되돌려 prod 함수를 망가뜨린다. IaC 는 함수 설정만, 코드는 CD 만 관리한다.
            ignoreChanges: ["code", "sourceCodeHash"],
        },
    );

    // ---- API Gateway (REST API + Lambda 프록시 통합) ----
    // HTTP API(v2)나 함수 URL 이 아니라 REST API 여야 한다. 핸들러 시그니처가
    // RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> 로
    // 페이로드 형식 1.0 전용인데, HTTP API 기본값은 2.0 이고 함수 URL 은 2.0 고정이다.
    const restApi = new aws.apigateway.RestApi("hello-prod-entrance-score-calculator-api", {
        name: `${functionName}-api`,
        description: "entrance-lambda 모의 성적 계산 API",
        endpointConfiguration: { types: "REGIONAL" },
    });

    // server 의 LambdaScoreCalculatorClient 가 @PostMapping 에 경로 없이 선언되어 설정된 URL
    // 루트로 POST 한다. 따라서 별도 경로 리소스를 두지 않고 루트 리소스에 메서드를 붙인다.
    const method = new aws.apigateway.Method("hello-prod-entrance-score-calculator-post", {
        restApi: restApi.id,
        resourceId: restApi.rootResourceId,
        httpMethod: "POST",
        // 인증은 핸들러가 x-hg-api-key 헤더를 직접 비교해 처리한다 - API Gateway 의 API 키
        // 기능을 켜면 인증 지점이 두 곳으로 쪼개진다.
        authorization: "NONE",
    });

    const integration = new aws.apigateway.Integration("hello-prod-entrance-score-calculator-integration", {
        restApi: restApi.id,
        resourceId: restApi.rootResourceId,
        httpMethod: method.httpMethod,
        // AWS_PROXY = Lambda 프록시 통합. 이게 아니면 핸들러가 요청 본문을 받지 못한다.
        type: "AWS_PROXY",
        // 프록시 통합의 integrationHttpMethod 는 Lambda 호출 규약상 항상 POST 다.
        integrationHttpMethod: "POST",
        uri: entranceLambda.invokeArn,
    });

    new aws.lambda.Permission("hello-prod-entrance-score-calculator-apigw-permission", {
        action: "lambda:InvokeFunction",
        function: entranceLambda.name,
        principal: "apigateway.amazonaws.com",
        sourceArn: pulumi.interpolate`${restApi.executionArn}/*/*`,
    });

    const deployment = new aws.apigateway.Deployment(
        "hello-prod-entrance-score-calculator-deployment",
        {
            restApi: restApi.id,
            // 메서드/통합이 바뀌어도 Deployment 를 새로 만들지 않으면 변경이 실제 엔드포인트에
            // 반영되지 않는다.
            triggers: {
                redeployment: pulumi.jsonStringify({
                    method: method.id,
                    integration: integration.id,
                }),
            },
        },
        { dependsOn: [method, integration] },
    );

    const stage = new aws.apigateway.Stage("hello-prod-entrance-score-calculator-stage", {
        restApi: restApi.id,
        deployment: deployment.id,
        stageName: STAGE_NAME,
    });

    return { function: entranceLambda, restApi, stage };
}
