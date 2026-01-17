import at.asitplus.gradle.modulator.carrier
import at.asitplus.gradle.publishVersionCatalog
import at.asitplus.gradle.setupDokka
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

System.setProperty("KOTEST_NO_ASP_HELPER", "true")
System.setProperty("TESTBALLOON_NO_ASP_HELPER","true")

plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.agp)
    alias(libs.plugins.asp)
    alias(libs.plugins.testballoon)
    alias(libs.plugins.modulator)
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
        namespace = "at.asitplus.testballoon.fixturegen.freespec"
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
                implementation(project(":internals"))
                carrier(project(":freespec"))
                carrier(project(":fixturegen"))
            }
        }
        commonTest.dependencies {
            implementation("de.infix.testBalloon:testBalloon-integration-kotest-assertions:${libs.versions.testballoon.get()}")
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
                name.set("TestBalloon FixtureGen FreepSpec Style")
                description.set("TestBalloon FreeSpec Style <-> FixtureGen Bridge")
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