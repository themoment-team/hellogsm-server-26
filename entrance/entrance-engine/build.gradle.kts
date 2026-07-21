plugins {
    kotlin("jvm")
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
    api(project(":entrance-dsl"))
    testImplementation(kotlin("test"))
    testImplementation(project(":entrance-plans"))
}

tasks.test {
    useJUnitPlatform()
}
