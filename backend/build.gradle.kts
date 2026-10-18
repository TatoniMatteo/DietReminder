plugins {
    jacoco
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "it.matato.dietreminder"
version = libs.versions.versionName.get()

// Align the shared Kotlin contracts and PostgreSQL/Docker tooling with this build.
extra["testcontainers.version"] = libs.versions.testcontainers.get()
extra["kotlin.version"] = libs.versions.kotlin.get()
extra["kotlin-serialization.version"] = libs.versions.serialization.get()
extra["kotlin-coroutines.version"] = libs.versions.coroutines.get()
extra["flyway.version"] = libs.versions.flyway.get()

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
    implementation(project(":common"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.springdoc.openapi.ui)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.bootJar {
    archiveFileName.set("dietreminder-backend.jar")
}

// HTTP and database tests use the production schema on an isolated PostgreSQL container.
val integrationTest = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    "integrationTestImplementation"(project(":client"))
    "integrationTestImplementation"(libs.kotlinx.coroutines.core)
    "integrationTestImplementation"("org.springframework.boot:spring-boot-testcontainers")
    "integrationTestImplementation"("org.testcontainers:postgresql")
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
    description = "Tests the HTTP API and Flyway migrations against PostgreSQL in Docker."
    group = "verification"
    testClassesDirs = integrationTest.output.classesDirs
    classpath = integrationTest.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}
tasks.check { dependsOn(integrationTestTask) }

// Include the complete module; generated Kotlin/serialization code stays visible in the baseline.
tasks.jacocoTestReport {
    dependsOn(tasks.test, integrationTestTask)
    executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/*.exec") })
    reports { html.required.set(true); xml.required.set(true) }
}
tasks.check { dependsOn(tasks.jacocoTestReport) }

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    executionData.setFrom(fileTree(layout.buildDirectory) { include("jacoco/*.exec") })
    violationRules { rule { limit { counter = "LINE"; minimum = "0.85".toBigDecimal() } } }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
