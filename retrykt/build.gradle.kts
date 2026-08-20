import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.straxess"
version = "0.3.0"

kotlin {
    explicitApi()

    jvmToolchain(17)
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    androidLibrary {
        namespace = group.toString()
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosArm64()

    mingwX64()

    linuxX64()
    linuxArm64()

    js {
        nodejs()
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
        }

        commonTest.dependencies {
            implementation(libs.coroutines.test)
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "retrykt",
        version = version.toString(),
    )

    pom {
        name = "RetryKt"
        description =
            "A lightweight KMP library for retrying operations with configurable retry policies, backoff strategies, and jitter."
        url = "https://github.com/straxess/retrykt"
        inceptionYear = "2026"

        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "straxess"
                name = "Andrey Afanasyev"
                url = "https://github.com/straxess"
            }
        }

        scm {
            url = "https://github.com/straxess/retrykt"
            connection = "scm:git:https://github.com/straxess/retrykt.git"
            developerConnection = "scm:git:ssh://git@github.com/straxess/retrykt.git"
        }

        issueManagement {
            system = "GitHub"
            url = "https://github.com/straxess/retrykt/issues"
        }
    }
}
