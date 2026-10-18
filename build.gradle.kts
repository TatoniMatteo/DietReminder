// Kotlin and AGP share a root classloader when building Android. Server-only
// builds do not resolve AGP or require an Android SDK.
buildscript {
    if (!providers.gradleProperty("backendOnly").map { it.isEmpty() || it.toBoolean() }.getOrElse(false)) {
        dependencies {
            classpath("com.android.tools.build:gradle:${libs.versions.agp.get()}")
        }
    }
}

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    kotlin("jvm") version libs.versions.kotlin.get() apply false
    kotlin("plugin.spring") version libs.versions.kotlin.get() apply false
    kotlin("plugin.jpa") version libs.versions.kotlin.get() apply false
}
