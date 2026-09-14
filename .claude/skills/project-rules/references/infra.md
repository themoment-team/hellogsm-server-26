# Infrastructure Rules — Pulumi IaC

> Scope: `infra/` only — a **Pulumi (TypeScript) project, not a Gradle module**. It manages the **production** AWS infrastructure: VPC, RDS, ALB, EC2 (Spring Boot / Redis), CodeDeploy, the GitHub OIDC deploy role, and the `entrance-lambda` function + API Gateway. Stage is deliberately out of scope and stays un-IaC'd. Operational runbook, DR import list, and per-resource caveats: `infra/README.md` — read it before any `pulumi` command.

## State and execution model

State lives in **Pulumi Cloud** (`gsmthemoment-gmail-com/hellogsm-infra/prod`), not in the repo.

**There is no pipeline.** No workflow references Pulumi, and `infra/**` is not built or type-checked by CI. `pulumi up` is run by hand from a workstation, so:

- Merging an `infra/` change applies **nothing**. A merged PR and the live infrastructure are independent facts — never report infra work as "deployed" because it landed on `develop`.
- Conversely, live infrastructure can drift from `main` without any signal.
- Run `npx tsc --noEmit` in `infra/` before committing. Nothing else will catch a type error.

```bash
cd infra
pulumi login && npm install
pulumi stack select prod
pulumi preview    # read-only, always safe
pulumi up
```

## Hard rules (do not violate)

1. **`pulumi preview` first, every time — and stop if it reports `replace` or `delete`.** Long-lived prod resources (VPC, NAT, RDS, the CodeDeploy app, the log group holding ~1GB of real logs) are `protect: true` and several were adopted via `pulumi import`. A `replace` on one of them destroys production data. Joining the existing stack cleanly means **0 create / 0 replace**; anything else is drift to be explained before proceeding.
2. **Never `pulumi destroy` without `--exclude-protected`.** `protect: true` does not cause the rest to be skipped — it errors out partway, after unprotected resources are already gone.
3. **Lambda code belongs to CD, not to Pulumi.** `modules/lambda.ts` creates the function shell (runtime, handler, memory, env) with a placeholder archive and `ignoreChanges: ["code", "sourceCodeHash"]`. The real jar is pushed by `.github/workflows/entrance-lambda-prod-cd.yml`. **Removing that `ignoreChanges` makes the next `pulumi up` revert prod to the placeholder and take the scoring API down.**
4. **Secrets go through `pulumi config set --secret`,** stored as `secure:` values in `Pulumi.prod.yaml` (precedent: `dbPassword`, `entranceLambdaApiKey`). Never commit a plaintext credential, and never move one into a `cfg.get()` default.
5. **Resource names are pinned and load-bearing.** Modules pass `name:` explicitly (`hello-prod-*`) because those names are referenced by IAM ARNs built from config strings, by GitHub Secrets, and by the DR import list. Renaming a resource is a replace — treat it as a production change, not a refactor.
6. **Immutable fields must match reality exactly.** For imported resources, a mismatched immutable attribute (notably `description`) silently turns an update into a delete-create. `hellogsm-nat-sg` has already been hit by this once.

## Conventions

- One module per concern under `modules/`, exporting `createXxx(): XxxResult`. `index.ts` wires them and declares stack outputs; `config.ts` is the only place that reads `pulumi.Config`.
- Comments are in Korean and explain **why** a value or guard exists (which resource it protects, what breaks without it), not what the API call does. Match the density of the surrounding module.
- Build ARNs from config/name constants with `pulumi.interpolate` plus `aws.getCallerIdentityOutput()` rather than importing another module's resource, when doing so would create a circular dependency between modules (see `entranceLambdaArn` in `modules/iam.ts`).
- New prod resources are a cost and a blast-radius decision. Prefer letting AWS manage a side-effect resource (e.g. Lambda's auto-created log group) and record the trade-off under `infra/README.md` § "알려진 스코프 제외 사항" instead of adding it silently.

## Deploy auth — prod is OIDC

`hellogsm-prod-cd.yml` and `entrance-lambda-prod-cd.yml` assume `vars.AWS_PROD_DEPLOY_ROLE_ARN` (created in `modules/iam.ts`) and need `permissions: id-token: write`. The trust policy pins `sub` to `ref:refs/heads/main`, so **`workflow_dispatch` must target `main`** — any other branch is rejected at `AssumeRoleWithWebIdentity`.

Granting a workflow a new AWS action means editing the role policy in `modules/iam.ts` and running `pulumi up`. It is never a console change.

Stage CD (`hellogsm-stage-cd.yml`) still uses the static `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` secrets. Those keys cannot be deleted until stage is migrated too.

## entrance-lambda specifics

- Runtime **must** be `java25` — `entrance-lambda/build.gradle.kts` targets `JVM_25` (class file 69), so java21 and below fail with `UnsupportedClassVersionError`. `@pulumi/aws` 6.66 has no `java25` enum member; the string literal is intentional.
- API Gateway **must** be a REST API. The handler is `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>` — payload format 1.0 only — while HTTP API (v2) defaults to 2.0 and Lambda function URLs are 2.0-only.
- Auth is the `x-hg-api-key` header compared inside the handler. Do not enable API Gateway's API-key feature; it splits authentication across two places.
- A missing `X_HG_INTERNAL_API_KEY` env var makes the constructor `error()` out, so every request fails at init. **401 is healthy behaviour; 500 means the env var.**
- Environment is prod-only, matching the Go implementation it replaces. The scorer touches no database, so the stage server calling the prod function is safe by design.
