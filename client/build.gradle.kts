plugins {
    jacoco
    `java-library`
    kotlin("jvm")
}

group = "it.matato.dietreminder"
version = libs.versions.versionName.get()

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.coroutines.test)
    api(project(":common"))
    api(libs.retrofit)
    api(libs.okhttp)
    implementation(libs.retrofit.serialization)
    implementation(libs.kotlinx.serialization.json)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

// Include the complete module; generated Kotlin/serialization code stays visible in the baseline.
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/*.exec") })
    reports { html.required.set(true); xml.required.set(true) }
}
tasks.check { dependsOn(tasks.jacocoTestReport) }

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/*.exec") })
    violationRules { rule { limit { counter = "LINE"; minimum = "0.90".toBigDecimal() } } }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
