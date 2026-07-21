plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
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
