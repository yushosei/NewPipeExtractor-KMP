import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.kotlinx.serialization)
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.yushosei"
version = "1.0"

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    // publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()

    coordinates(group.toString(), "newpipe-extractor-kmp", version.toString())

    pom {
        name.set("NewPipeExtractor-KMP")
        description.set("A Kotlin Multiplatform-compatible library adapted from NewPipeExtractor for extracting YouTube videos and comments in Compose Multiplatform projects.")
        inceptionYear.set("2025")
        url.set("https://github.com/yushosei/NewPipeExtractor-KMP")

        licenses {
            license {
                name.set("GNU General Public License, Version 3.0")
                url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("yushosei")
                name.set("Yushosei")
                email.set("dev.yushosei@gmail.com")
                url.set("https://github.com/yushosei")
            }
        }

        scm {
            url.set("https://github.com/yushosei/NewPipeExtractor-KMP")
            connection.set("scm:git:git://github.com/yushosei/NewPipeExtractor-KMP.git")
            developerConnection.set("scm:git:ssh://git@github.com/yushosei/NewPipeExtractor-KMP.git")
        }
    }
}



kotlin {

// Target declarations - add or remove as needed below. These define
// which platforms this KMP module supports.
// See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    //explicitApi()

    androidLibrary {
        namespace = "com.yushosei.newpipe"
        compileSdk = 35
        minSdk = 24

        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {

        }
    }

// For iOS targets, this is also where you should
// configure native binary output. For more information, see:
// https://kotlinlang.org/docs/multiplatform-build-native-binaries.html#build-xcframeworks

// A step-by-step guide on how to include this library in an XCode
// project can be found here:
// https://developer.android.com/kotlin/multiplatform/migrate
    val xcfName = "newpipe-KMPKit"

    iosX64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = xcfName
        }
    }

// Source set declarations.
// Declaring a target automatically creates a source set with the same name. By default, the
// Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
// common to share sources between related targets.
// See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.io.core)

                implementation(libs.ktor.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.client.content.negotiation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.android)
                implementation(libs.kotlinx.coroutines.android)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
            }
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}