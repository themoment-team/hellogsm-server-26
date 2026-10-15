import * as aws from "@pulumi/aws";

export interface EcrResult {
    repository: aws.ecr.Repository;
}

// 기존에는 배포 아티팩트(jar+Dockerfile)를 EC2로 보내 인스턴스 위에서 직접 docker build 했다.
// 인스턴스가 2대가 되면 한 대씩 교체(HalfAtATime)하는 동안 빌드 시간이 그대로 다운타임 창에
// 들어가 롤링 배포의 이점이 줄어든다. CI에서 미리 빌드해 ECR에 push하고, 인스턴스는
// pull + run만 하도록 분리한다.
export function createEcr(): EcrResult {
    const repository = new aws.ecr.Repository("hello-prod-server-ecr", {
        name: "hello-prod-server",
        // 태그는 git commit SHA지만, application-prod.yml(스케줄 등 운영 설정)은 jar 안에
        // 빌드 시점에 내장되고 GitHub Actions secret으로 커밋과 별개로 바뀔 수 있다.
        // IMMUTABLE이면 같은 커밋을 설정만 바꿔 재배포할 때 "tag already exists"로 push가
        // 막혀서, 재배포가 실제로는 과거 이미지를 재사용하는 조용한 버그로 이어진다.
        // MUTABLE로 두고 매 배포마다 항상 재빌드/재push한다 (CI 워크플로 참고).
        imageTagMutability: "MUTABLE",
        imageScanningConfiguration: { scanOnPush: true },
        tags: { Name: "hello-prod-server-ecr" },
    });

    new aws.ecr.LifecyclePolicy("hello-prod-server-ecr-lifecycle", {
        repository: repository.name,
        policy: JSON.stringify({
            rules: [
                {
                    rulePriority: 1,
                    description: "최근 20개 이미지만 보관",
                    selection: {
                        tagStatus: "any",
                        countType: "imageCountMoreThan",
                        countNumber: 20,
                    },
                    action: { type: "expire" },
                },
            ],
        }),
    });

    return { repository };
}
