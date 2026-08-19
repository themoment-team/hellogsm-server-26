# hellogsm-infra

hellogsm-server-25 **상용(production)** AWS 인프라를 관리하는 Pulumi(TypeScript) 프로젝트.
State는 Pulumi Cloud(`gsmthemoment-gmail-com/hellogsm-infra/prod`)에 저장한다. 이 스코프는 Production
환경만 다루며, Stage/Monitoring 환경과 `hellogsm-prod-ci.yml`, stage 워크플로는 포함하지 않는다.

## 현재 상태 (중요)

이 프로젝트를 처음 설계할 때는 "상용 인프라가 전부 삭제됐다"는 전제로 전량 신규 생성을 계획했으나,
실제로는 아래 리소스들이 예전부터(2024년~) 그대로 남아 운영되고 있었다:

- VPC `hello-vpc`, 서브넷 4개(`hello-prod-public-2a`, `hello-public-subnet-2b`,
  `hello-prod-private-2a`, `hello-private-subnet-2b`), IGW `hellogsm-igw`, 라우트테이블 4개 + 연결
- Bastion+NAT 인스턴스 `hello-prod-nat`(운영 중, EIP `hello-prod-eip` 연결) + 보안그룹 `hellogsm-nat-sg`
- CodeDeploy 애플리케이션 `hellogsm-prod-codedeploy` / 배포그룹 `api-prod-hellogsm-kr` (실제 배포 이력 있음)
- CloudWatch 로그 그룹 `hellogsm-prod-log` (수백MB~1GB 이상의 실제 운영 로그 보유)
- S3 버킷 `hellogsm-cicd-bucket`(배포 아티팩트), `hello-26-prod-bucket`(앱 자산)

**이 리소스들은 전부 `pulumi import`로 흡수되어 있고, 코드에서 `protect: true`로 보호된다.**
`hello-pub-rtb`(2b 퍼블릭 라우트테이블)는 **stage 환경(`hello-dev-public-2a`)과 공유**되므로 특히 주의.

새로 생성/관리하는 것은 RDS MySQL, ALB(+ 리스너/타겟그룹), Spring Boot EC2, Redis EC2, 관련 보안그룹,
IAM 롤(Spring Boot 인스턴스 프로파일 / CodeDeploy 서비스 롤 / GitHub OIDC 배포 롤), ACM 인증서,
Route53 레코드, SNS 알람 뿐이다.

## 사전 준비

- Pulumi CLI, Node.js 20+, AWS CLI(자격증명 설정 완료)
- Pulumi Cloud 계정 (`pulumi login`)
- `hellogsm.kr` Route53 Hosted Zone이 같은 AWS 계정에 이미 존재 (데이터 소스로만 조회, 신규 생성 안 함)

## Bootstrap (기존 stack에 합류하는 경우)

```bash
cd infra
pulumi login
npm install
pulumi stack select prod
pulumi preview   # diff가 없어야 정상 (0 create / 0 replace)
```

## Config

`Pulumi.prod.yaml`에 실제 운영 값이 커밋되어 있다 (`dbPassword`만 Pulumi가 암호화한 `secure` 값).
값을 바꿔야 할 때만 아래처럼 갱신한다.

```bash
pulumi config set adminSshCidr <IP>/32
pulumi config set --secret dbPassword '<new-password>'
```

## 재해복구 — 스택을 처음부터 다시 만들어야 하는 경우

새 Pulumi 조직/프로젝트로 마이그레이션하는 등 state를 처음부터 다시 구성해야 한다면,
"현재 상태" 절에서 언급한 기존 리소스들을 **반드시 먼저 import**해야 한다 (안 하면 `pulumi up`이
동일 이름/CIDR의 VPC·NAT를 중복 생성하거나, 이미 존재하는 로그 그룹/CodeDeploy 앱 이름 충돌로 실패한다).

```bash
# VPC / IGW / 서브넷 / 라우트테이블 / 연결 / NAT / SG / EIP / 로그그룹 / CodeDeploy
# — 정확한 리소스 ID는 AWS 콘솔 또는 `aws ec2 describe-*` 로 먼저 조회할 것
pulumi import aws:ec2/vpc:Vpc hello-vpc <vpc-id>
pulumi import aws:ec2/internetGateway:InternetGateway hellogsm-igw <igw-id>
pulumi import aws:ec2/subnet:Subnet hello-prod-public-2a <subnet-id>
pulumi import aws:ec2/subnet:Subnet hello-public-subnet-2b <subnet-id>
pulumi import aws:ec2/subnet:Subnet hello-prod-private-2a <subnet-id>
pulumi import aws:ec2/subnet:Subnet hello-private-subnet-2b <subnet-id>
pulumi import aws:ec2/routeTable:RouteTable hello-prod-pub-rtb-a <rtb-id>
pulumi import aws:ec2/routeTable:RouteTable hello-pub-rtb <rtb-id>
pulumi import aws:ec2/routeTable:RouteTable hello-prod-priv-rtb-a <rtb-id>
pulumi import aws:ec2/routeTable:RouteTable hello-prod-priv-rtb-b <rtb-id>
pulumi import aws:ec2/routeTableAssociation:RouteTableAssociation hello-prod-public-2a-assoc <subnet-id>/<rtb-id>
pulumi import aws:ec2/routeTableAssociation:RouteTableAssociation hello-public-subnet-2b-assoc <subnet-id>/<rtb-id>
pulumi import aws:ec2/routeTableAssociation:RouteTableAssociation hello-prod-private-2a-assoc <subnet-id>/<rtb-id>
pulumi import aws:ec2/routeTableAssociation:RouteTableAssociation hello-private-subnet-2b-assoc <subnet-id>/<rtb-id>
pulumi import aws:ec2/securityGroup:SecurityGroup hellogsm-nat-sg <sg-id>
pulumi import aws:ec2/instance:Instance hello-prod-nat <instance-id>
pulumi import aws:ec2/eip:Eip hello-prod-eip <allocation-id>
pulumi import aws:ec2/route:Route hello-prod-priv-rtb-a-nat-route '<rtb-id>_0.0.0.0/0'
pulumi import aws:ec2/route:Route hello-prod-priv-rtb-b-nat-route '<rtb-id>_0.0.0.0/0'
pulumi import aws:cloudwatch/logGroup:LogGroup hellogsm-prod-log hellogsm-prod-log
pulumi import aws:codedeploy/application:Application hellogsm-prod-codedeploy hellogsm-prod-codedeploy
pulumi import aws:codedeploy/deploymentGroup:DeploymentGroup api-prod-hellogsm-kr 'hellogsm-prod-codedeploy:api-prod-hellogsm-kr'
pulumi import aws:s3/bucketV2:BucketV2 deploymentBucket hellogsm-cicd-bucket
pulumi import aws:s3/bucketV2:BucketV2 appAssetsBucket hello-26-prod-bucket
```

각 import 후 `pulumi preview`로 diff를 확인하고, **`replace`/`delete-create`가 뜨면 절대 `pulumi up`하지 말고**
코드의 해당 속성(특히 `description`처럼 변경 시 교체가 발생하는 불변 필드)을 실제 값에 맞게 고친다.
(`hellogsm-nat-sg`의 `description`이 이 문제로 한 번 걸렸던 전례가 있다 — 반드시 실제 값과 동일하게 맞출 것.)

## 배포

```bash
pulumi preview
pulumi up
```

새로 생성/갱신되는 리소스는 다음 순서로 자동 처리된다:
securityGroups/iam/s3 → database(RDS, 10~15분 소요) → compute(Spring Boot, Redis) →
dnsCert(ACM DNS 검증 대기) → alb → codeDeploy(설정 갱신) → monitoring

**`--exclude-protected` 없이 `pulumi destroy`를 실행하면 안 된다** — protect:true가 아닌 리소스만
지워지는 게 기본 동작이 아니라, protect:true 리소스를 만나면 에러로 막힐 뿐 나머지가 지워지는 걸
막지 못한다. 새로 만든 리소스만 걷어내려면 반드시 `pulumi destroy --exclude-protected`를 사용한다.

## 인프라 생성 후 운영 절차

1. `pulumi stack output rdsEndpointAddress`, `pulumi stack output redisPrivateIp` 확인
2. GitHub Secret `PROD_WEB_YML` 갱신:
   - `DB_URL=jdbc:mysql://<rdsEndpointAddress>:3306/<dbName>`
   - `DB_USERNAME` / `DB_PASSWORD` (Pulumi config와 동일 값)
   - `DB_CLASS_NAME=com.mysql.cj.jdbc.Driver`, `DB_PLATFORM=org.hibernate.dialect.MySQLDialect`
   - `REDIS_HOST=<redisPrivateIp>`
   - `ACTUATOR_BASE_PATH`는 Pulumi config의 `actuatorBasePath`(`/hello-management`)와 반드시 동일하게 유지
   - `AWS_BUCKET_NAME=hello-26-prod-bucket`, `AWS_REGION`/`AWS_SNS_REGION=ap-northeast-2`
3. GitHub Repository Variable `AWS_PROD_DEPLOY_ROLE_ARN` = `pulumi stack output githubActionsRoleArn`
4. `.github/workflows/hellogsm-prod-cd.yml`의 OIDC 전환이 머지되어 있는지 확인
5. main 브랜치 push 또는 `workflow_dispatch`로 CD 워크플로 실행 → 첫 배포
6. 검증 통과 후 `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` GitHub Secret을 GitHub UI에서 수동 삭제

## 검증

- `pulumi preview`/`pulumi up`이 clean하게 끝나는지 확인 (replace/delete 없이 create/update만)
- Bastion EIP로 SSH 후 `ssh -J`로 Spring Boot/Redis 프라이빗 인스턴스 접근 확인, 프라이빗 인스턴스에서 아웃바운드 인터넷(NAT 경유) 동작 확인
- `mysql -h <rdsEndpoint> -u <dbUsername> -p` 접속 확인
- Redis 인스턴스에서 `docker ps` 확인, Spring Boot 인스턴스에서 `redis-cli -h <redisPrivateIp> ping` → `PONG`
- CD 워크플로 실행 후 `aws deploy get-deployment --deployment-id <id>` 상태 `Succeeded`
- `aws elbv2 describe-target-health --target-group-arn <arn>` → `healthy`
- `curl -I https://api-prod.hellogsm.kr<actuatorBasePath>/health` → `200`
- `curl -I http://api-prod.hellogsm.kr` → `301`
- `aws logs describe-log-streams --log-group-name hellogsm-prod-log`로 로그 유입 확인
- Spring Boot 컨테이너를 의도적으로 중지해 ALB Unhealthy 알람이 SNS로 발행되는지 확인 후 재기동
- GitHub Actions 로그에서 정적 키가 아닌 `role-to-assume`(OIDC) 방식으로 인증되는지 확인

## 알려진 스코프 제외 사항

- Discord 웹훅 연동(CloudWatch → Discord)은 레포에 메커니즘이 없어 이번 범위에서 제외 — SNS Topic까지만 생성
- Stage/Monitoring 환경, `hellogsm-prod-ci.yml`, stage 워크플로는 별도 과제
- 앱의 `AWS_ACCESS_KEY`/`AWS_SECRET_KEY` 정적 키 → 인스턴스 프로파일 전환은 후속 과제로 남김
- `hellogsm-nat-sg`가 현재 0.0.0.0/0 전체 허용으로 다소 느슨하게 설정되어 있음 (기존 상태 그대로 흡수) — 보안 강화는 별도 논의 필요
