# hellogsm-infra

hellogsm-server-26 **상용(production)** AWS 인프라를 관리하는 Pulumi(TypeScript) 프로젝트.
State는 Pulumi Cloud(`gsmthemoment-gmail-com/hellogsm-infra/prod`)에 저장한다. 이 스코프는 Production
환경만 다루며, Stage/Monitoring 환경과 `hellogsm-prod-ci.yml`, stage 워크플로는 포함하지 않는다.

## 현재 상태 (중요) — 보존 인프라 vs 운영 사이클 인프라 2분할

2027학년도 입학 전형이 끝나고 상용 인프라를 "정리"하면서, 이 스택을 **상시 보존 인프라**와
**매 운영 사이클(전형 시즌)마다 `pulumi up`/`pulumi destroy --exclude-protected`로 뜨고 내려가는
인프라**로 명확히 나눴다. 전자는 비용이 거의 없거나(네트워크, IAM, S3 스토리지, 로그그룹, ECR 등)
혹은 재생성이 위험/번거로운(Bastion+NAT) 리소스이고, 후자는 실제 전형 기간에만 필요한 과금
컴퓨트(EC2, ALB)다. **RDS(`hello-prod-mysql`)는 이번 정리 범위에서 완전히 제외** — 데이터/스냅샷
처리는 별도 일정으로 진행하며 지금은 그대로 `protect:true` + `deletionProtection:true`로 운영 유지.

### 상시 보존 인프라 — 더 이상 Pulumi가 생성/수정/삭제하지 않고 데이터 소스로만 참조

이 리소스들은 전부 2024년부터 그대로 운영되어온 것들이거나, CI/CD·모니터링처럼 시즌과 무관하게
상시 필요한 것들이다. **과거에는 `pulumi import` + `protect:true`로 "관리하되 보호"하는 방식이었지만,
이제는 Pulumi state에서 완전히 빼고 읽기 전용 데이터 소스(`aws.*.get*Output`)로만 참조한다** —
매 시즌 `pulumi destroy --exclude-protected`를 돌려도 아예 그래프에 없으므로 건드릴 일이 없다.

- `network.ts`: VPC `hello-vpc`, 서브넷 4개, IGW `hellogsm-igw`, 라우트테이블 4개 — 전부 `tag:Name`
  필터 기반 데이터 소스 조회. `hello-pub-rtb`(2b 퍼블릭 라우트테이블)는 stage 환경과 공유되므로
  애초에 이 스택이 손댈 일이 없어야 한다는 점에서도 더 안전해졌다.
- `compute/bastionNat.ts`: Bastion+NAT 인스턴스 `hello-prod-nat` + EIP `hello-prod-eip` — 데이터
  소스 조회. 이 인스턴스는 DB/배치 작업용 SSH 터널 등 운영 전반에 상시 쓰이는 Bastion이라 시즌과
  무관하게 항상 떠 있어야 한다.
- `securityGroups.ts`의 `bastionSg`(`hellogsm-nat-sg`) — 데이터 소스 조회.
- `s3.ts`: 배포 아티팩트 버킷 / 앱 자산 버킷 — 데이터 소스 조회 (스토리지 비용만 발생).
- `monitoring.ts`의 CloudWatch 로그 그룹 `hellogsm-prod-log` — 이름만 상수로 참조 (실제 가치가
  로그 데이터 자체에 있어 삭제 시 전부 유실되므로 애초에 손댈 이유가 없다).

### protect:true로 보존하지만 데이터 소스 전환이 불가능/불필요한 것들

- `codeDeploy.ts`의 애플리케이션/배포그룹 — AWS 프로바이더에 CodeDeploy용 데이터 소스가 없어
  (`getApplication`/`getDeploymentGroup` 미지원) 기존처럼 `protect:true` 관리 리소스로 유지.
- `securityGroups.ts`의 `rdsSg` — RDS가 이번 범위에서 제외되어 계속 운영 중이므로, 함께 destroy되면
  RDS가 보안그룹을 잃는 장애로 이어진다. 그래서 새로 `protect:true`를 추가했다.
- `iam.ts`의 GitHub OIDC provider — AWS 계정당 동일 URL로 1개만 허용되고 비용이 전혀 없어
  `protect:true`로 전환. (Role/Policy 자체는 ephemeral로 유지 - 이름이 고정이라 재생성해도 ARN 동일)
- `monitoring.ts`의 SNS 알람 토픽/이메일 구독 — 비용이 거의 없고, 매 시즌 재생성하면 이메일 구독자가
  컨펌을 다시 눌러야 하는 불편이 생겨 `protect:true`로 전환. (MetricAlarm 자체는 매 시즌 새
  인스턴스/ALB를 가리켜야 해서 ephemeral로 유지)
- `ecr.ts`의 ECR 저장소 `hello-prod-server` — CI/CD가 시즌과 무관하게 계속 이미지를 push하는
  상시 자원이라 `protect:true`로 전환. (이미지 보관 비용은 lifecycle policy로 최근 20개만 유지)

### 운영 사이클마다 뜨고 내려가는 인프라 (ephemeral, `pulumi up`/`pulumi destroy --exclude-protected` 대상)

ALB(+리스너 2개+타겟그룹+어태치먼트), Route53 A레코드, ACM 인증서(+DNS 검증), Spring Boot EC2 2대,
Redis EC2, 이들의 보안그룹(albSg/springbootSg/redisSg), IAM 롤(Spring Boot 인스턴스 프로파일 /
CodeDeploy 서비스 롤 / GitHub Actions 배포 롤), CloudWatch MetricAlarm들.

### 이번 정리 작업 전에 반드시 먼저 해야 하는 것 — 기존 state 정리

지금 실제 Pulumi stack state에는 위 "상시 보존" 리소스들이 **과거 코드 기준으로 이미
`pulumi import`되어 관리 리소스로 들어가 있다.** 코드만 이렇게 바꾸고 바로 `pulumi up`을 돌리면
Pulumi는 "코드에서 사라진 관리 리소스"로 인식해 **삭제를 시도**하는데, `protect:true`가 걸려있던
리소스라도 이번 코드 변경으로 보호 설정 자체가 없어졌으므로 실제로 삭제될 위험이 있다.
**따라서 `pulumi up`/`preview`를 돌리기 전에, 아래처럼 해당 리소스들을 state에서만 제거(`pulumi
state rm`)해 실제 AWS 리소스는 그대로 두고 Pulumi의 관리 대상에서만 빼야 한다.**

```bash
cd infra
pulumi stack select prod

# network.ts
pulumi state rm 'urn:pulumi:prod::hellogsm-infra::...:aws:ec2/vpc:Vpc::hello-vpc'
# (정확한 URN은 `pulumi stack --show-urns` 또는 `pulumi stack export`로 먼저 확인할 것 -
#  아래는 리소스 이름만 나열한 목록이다)
# hello-vpc, hellogsm-igw,
# hello-prod-public-2a, hello-public-subnet-2b, hello-prod-private-2a, hello-private-subnet-2b,
# hello-prod-pub-rtb-a(+ igw-route), hello-pub-rtb(+ igw-route),
# hello-prod-priv-rtb-a(+ nat-route), hello-prod-priv-rtb-b(+ nat-route),
# hello-prod-public-2a-assoc, hello-public-subnet-2b-assoc,
# hello-prod-private-2a-assoc, hello-private-subnet-2b-assoc

# compute/bastionNat.ts + securityGroups.ts
# hello-prod-nat, hello-prod-eip, hellogsm-nat-sg

# s3.ts
# deploymentBucket, appAssetsBucket

# monitoring.ts
# hellogsm-prod-log (LogGroup 리소스만 - SNS Topic/Subscription은 그대로 유지, 삭제 금지)
```

`pulumi state rm`은 **state에서만** 빼는 동작이라 실제 AWS 리소스는 전혀 건드리지 않는다(AWS API를
호출하지 않음). 전부 제거한 뒤 `pulumi preview`를 돌려 diff가 0 delete / (ephemeral 리소스만)
update인지 반드시 확인한다. 예상 밖의 `delete`나 `replace`가 보이면 **절대 `pulumi up`하지 말고**
먼저 원인을 파악한다.

### 운영 사이클 명령

```bash
# 전형 시즌 시작 - 과금 컴퓨트(ALB/EC2 등) 신규 생성
pulumi up

# 전형 시즌 종료 - ephemeral 리소스만 정리, 보존 인프라/RDS는 그대로 유지
pulumi destroy --exclude-protected
```

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

새 Pulumi 조직/프로젝트로 마이그레이션하는 등 state를 처음부터 다시 구성해야 한다면, 리소스
종류에 따라 절차가 다르다.

**상시 보존 인프라(데이터 소스로만 참조하는 것들)는 import가 필요 없다** — network.ts /
compute/bastionNat.ts / securityGroups.ts의 bastionSg / s3.ts가 참조하는 VPC·서브넷·IGW·
라우트테이블·NAT 인스턴스·EIP·`hellogsm-nat-sg`·S3 버킷 2개는 AWS 계정에 해당 `tag:Name`(또는
버킷명)으로 **이미 존재하기만 하면** 데이터 소스 조회가 바로 성공한다. 존재하지 않는다면 그건
이 스택의 책무가 아니라 해당 리소스를 실제로 복구해야 하는 더 큰 문제다.

여전히 Pulumi가 **생성/관리**하는 아래 리소스들만 기존 동일 이름이 AWS에 남아있는 경우
`pulumi up` 대신 먼저 import해야 충돌하지 않는다. 특히 OIDC provider는 계정당 URL별 1개만
허용되므로 반드시 먼저 확인할 것.

```bash
# 같은 URL의 OIDC provider가 계정에 이미 있으면 EntityAlreadyExists로 실패한다
aws iam list-open-id-connect-providers
pulumi import aws:iam/openIdConnectProvider:OpenIdConnectProvider github-actions-oidc <provider-arn>

pulumi import aws:iam/role:Role hello-prod-springboot-ec2-role hello-prod-springboot-ec2-role
pulumi import aws:iam/role:Role hello-prod-codedeploy-service-role hello-prod-codedeploy-service-role
pulumi import aws:iam/role:Role hello-prod-github-actions-deploy-role hello-prod-github-actions-deploy-role

pulumi import aws:rds/subnetGroup:SubnetGroup hello-prod-rds-subnet-group hello-prod-rds-subnet-group
pulumi import aws:ecr/repository:Repository hello-prod-server-ecr hello-prod-server
pulumi import aws:codedeploy/application:Application hellogsm-prod-codedeploy hellogsm-prod-codedeploy
pulumi import aws:codedeploy/deploymentGroup:DeploymentGroup api-prod-hellogsm-kr 'hellogsm-prod-codedeploy:api-prod-hellogsm-kr'
pulumi import aws:sns/topic:Topic hello-prod-alerts <topic-arn>
pulumi import aws:ec2/securityGroup:SecurityGroup hello-prod-rds-sg <sg-id>   # RDS가 이번 범위 제외라 기존 SG 유지 시에만 필요
pulumi import aws:lb/loadBalancer:LoadBalancer hello-prod-alb <alb-arn>   # ephemeral이지만 이름 고정이라 동일 이름 잔재가 있으면 필요
pulumi import aws:lb/targetGroup:TargetGroup hello-prod-tg <target-group-arn>
```

각 import 후 `pulumi preview`로 diff를 확인하고, **`replace`/`delete-create`가 뜨면 절대 `pulumi up`하지 말고**
코드의 해당 속성(특히 `description`처럼 변경 시 교체가 발생하는 불변 필드)을 실제 값에 맞게 고친다.

`infra/userdata/bastion-nat.sh`는 어떤 코드에서도 참조되지 않는다 - `compute/bastionNat.ts`는
기존 NAT 인스턴스를 데이터 소스로 조회만 할 뿐 userData를 지정하지 않는다(지정하면 stop/start가
발생해 오히려 위험하므로 의도적). DR 절차로 NAT 인스턴스를 **새로** 만드는 경우에만 이 스크립트를
수동으로 적용해야 한다 - 안 하면 iptables MASQUERADE가 설정되지 않아 프라이빗 서브넷에
라우트는 있어도 실제 인터넷 접근이 안 된다.

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

RDS(`hello-prod-mysql`)는 `protect:true` 외에 `deletionProtection:true`도 걸려있어, 의도적으로
지우려면 `pulumi state unprotect`로 protect를 해제한 뒤 **먼저 `deletionProtection: false`로
코드/config를 바꿔 `pulumi up`을 한 번 돌려야** 실제 삭제가 가능하다(AWS API 레벨 보호라
Pulumi만으로는 우회할 수 없음). `finalSnapshotIdentifier`가 고정값이라 이전에 같은 이름
스냅샷을 남긴 적이 있다면 삭제 전에 그 스냅샷부터 지워야 한다.

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
5. main 브랜치 push 또는 `workflow_dispatch`로 CD 워크플로 실행 → 첫 배포. OIDC trust policy의
   `sub` 조건이 `ref:refs/heads/main`으로 고정되어 있으므로, `workflow_dispatch`도 반드시
   main 브랜치를 대상으로 실행할 것 — 다른 브랜치로 실행하면 `AssumeRoleWithWebIdentity`가 거부된다
6. **정적 액세스키 삭제는 stage CD가 OIDC로 전환된 뒤에만 진행한다** — `hellogsm-stage-cd.yml`이
   아직 같은 `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` Secret을 쓰고 있어서, 이 PR의 검증만
   보고 지금 삭제하면 stage CD가 즉시 깨진다. stage도 OIDC로 전환한 뒤 GitHub UI에서 수동 삭제할 것

## 검증

- `pulumi preview`/`pulumi up`이 clean하게 끝나는지 확인 (replace/delete 없이 create/update만)
- Bastion EIP로 SSH 후 `ssh -J`로 Spring Boot/Redis 프라이빗 인스턴스 접근 확인, 프라이빗 인스턴스에서 아웃바운드 인터넷(NAT 경유) 동작 확인
- `mysql -h <rdsEndpoint> -u <dbUsername> -p` 접속 확인
- Redis 인스턴스에서 `docker ps` 확인, Spring Boot 인스턴스에서 `redis-cli -h <redisPrivateIp> ping` → `PONG`
- CD 워크플로 실행 후 `aws deploy get-deployment --deployment-id <id>` 상태 `Succeeded`
- `aws elbv2 describe-target-health --target-group-arn <arn>` → `healthy`
- `curl -I https://api-prod.hellogsm.kr<actuatorBasePath>/health/liveness` → `200` (ALB 타겟그룹이 보는 것과 동일한 경로)
- `curl -I http://api-prod.hellogsm.kr` → `301`
- `aws logs describe-log-streams --log-group-name hellogsm-prod-log`로 로그 유입 확인
- Spring Boot 컨테이너를 의도적으로 중지해 ALB Unhealthy 알람이 SNS로 발행되는지 확인 후 재기동
- GitHub Actions 로그에서 정적 키가 아닌 `role-to-assume`(OIDC) 방식으로 인증되는지 확인

## 알려진 스코프 제외 사항

- Discord 웹훅 연동(CloudWatch → Discord)은 레포에 메커니즘이 없어 이번 범위에서 제외 — SNS Topic까지만 생성
- Stage/Monitoring 환경, `hellogsm-prod-ci.yml`, stage 워크플로는 별도 과제
- 앱의 `AWS_ACCESS_KEY`/`AWS_SECRET_KEY` 정적 키 → 인스턴스 프로파일 전환은 후속 과제로 남김
- RDS `multiAz: false` — 비용 절감을 위한 의도적 선택 (Multi-AZ 전환 시 RDS 비용 약 2배). 고가용성이 필요해지면 별도 논의 후 전환
