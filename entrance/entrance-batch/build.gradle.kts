plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
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
    // 엔진(순수) + 요강 데이터 + 공유 영속성. 이 세 개를 잇는 어댑터가 배치다.
    implementation(project(":entrance-engine")) // entrance-dsl 을 api 로 전이
    implementation(project(":entrance-plans"))
    implementation(project(":persistence")) // JPA 엔티티·spring-data-jpa 를 api 로 전이

    implementation("org.springframework.boot:spring-boot-starter") // CLI 배치 — web 불필요
    implementation(kotlin("reflect")) // Spring 의 Kotlin 빈 생성자 주입에 필요
    runtimeOnly("com.mysql:mysql-connector-j")

    testImplementation(kotlin("test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.6"))
    testImplementation("org.testcontainers:mysql")
    testImplementation("org.testcontainers:junit-jupiter")
}

// bootJar 산출(ops 가 CLI 로 실행). 라이브러리 jar 는 불필요.
tasks.named<Jar>("jar") { enabled = false }

tasks.test {
    useJUnitPlatform()
}
