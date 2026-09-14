// 모의 성적 계산 API — AWS Lambda(API Gateway 프록시 통합)로 단독 배포된다.
// Spring 없이 aws-lambda-java-core 직접 구현: server 다운 시에도 동작해야 하는
// 가용성 요건 + 콜드 스타트 최소화가 목적이므로 프레임워크 부트스트랩 비용을 지지 않는다.
plugins {
    kotlin("jvm")
    id("com.gradleup.shadow") version "8.3.5"
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
    }
}

dependencies {
    implementation(project(":entrance-engine")) // entrance-dsl 을 api 로 전이
    implementation(project(":entrance-plans"))

    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

// 배포 산출물은 shadowJar(전 의존성 포함 fat jar) 하나면 된다 — 일반 jar 는 불필요.
tasks.named<Jar>("jar") { enabled = false }

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
