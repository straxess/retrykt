import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

group = "io.github.straxess"
version = "0.4.0"

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

        withJava()
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    androidNativeArm64()
    androidNativeX64()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    watchosDeviceArm64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosSimulatorArm64()

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

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    additionalEditorconfig.set(
        mapOf(
            "ktlint_code_style" to "intellij_idea",
            "ktlint_standard_no-wildcard-imports" to "disabled",
        ),
    )
}

tasks.named("check") {
    dependsOn("ktlintCheck")
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
            "A lightweight KMP library for retrying operations with configurable retry conditions, backoff, jitter, blocking and suspending APIs, and lifecycle observability."
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
