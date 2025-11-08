import at.asitplus.gradle.publishVersionCatalog
import at.asitplus.gradle.setupDokka
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

System.setProperty("KOTEST_NO_ASP_HELPER", "true")
System.setProperty("TESTBALLOON_NO_ASP_HELPER", "true")

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.agp)
    alias(libs.plugins.asp)
    alias(libs.plugins.testballoon)
    id("signing")
}
group = "at.asitplus.testballoon"

val artifactVersion: String by extra
version = artifactVersion

repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
}


publishVersionCatalog = false

kotlin {
    jvm()
    androidLibrary {
        namespace = "at.asitplus.testballoon.commons"
    }
    macosArm64()
    macosX64()
    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()
    //wasmWasi(nodeJs())
    listOf(
        js().apply { browser { testTask { enabled = false } } },
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs().apply { browser { testTask { enabled = false } } }
    ).forEach {
        it.nodejs()
        it.browser()
    }

    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        commonMain {
            dependencies {
                api("de.infix.testBalloon:testBalloon-framework-core:${libs.versions.testballoon.get()}")
            }
        }

        sourceSets.filterNot {
            it.name.startsWith("common") || it.name.startsWith("android") || it.name.startsWith("tv") || it.name.startsWith(
                "watch"
            ) || it.name.startsWith("ios") || it.name.startsWith("apple")|| it.name.startsWith("mac")
                    || it.name.startsWith("webMain")
                    || it.name.startsWith("linux")
                    || it.name.startsWith("min")
        }
            .filter { it.name.endsWith("Main") }.forEach { srcSet ->
                srcSet.kotlin.srcDir("$projectDir/src/nonAndroidMain/kotlin")
            }
    }
}

val javadocJar = setupDokka(
    baseUrl = "https://github.com/a-sit-plus/testballoon-addons/tree/main/",
    multiModuleDoc = true
)

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("TestBalloon Addons Commons")
                description.set("TestBalloon Addons common functionality")
                url.set("https://github.com/a-sit-plus/testballoon-addons")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("JesusMcCloud")
                        name.set("Bernd Prünster")
                        email.set("bernd.pruenster@a-sit.at")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:a-sit-plus/testballoon-addons.git")
                    developerConnection.set("scm:git:git@github.com:a-sit-plus/testballoon-addons.git")
                    url.set("https://github.com/a-sit-plus/testballoon-addons")
                }
            }
        }
    }
    repositories {
        mavenLocal {
            signing.isRequired = false
        }
        maven {
            url = uri(layout.projectDirectory.dir("..").dir("repo"))
            name = "local"
            signing.isRequired = false
        }
    }
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}