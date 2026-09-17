plugins {
    // Deliberately not `kotlin-dsl`: it pins the Gradle-embedded Kotlin and its 1.8 language level, which
    // collides with the Kotlin Gradle plugin version this build already puts on the classpath.
    // No version: the Kotlin Gradle plugin is already on this build's classpath.
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
    `maven-publish`
}

val artifactVersion: String by extra
version = artifactVersion
group = rootProject.group

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Typed access to AGP's variant API and DSL. `compileOnly`: consumers bring their own AGP, and projects
    // without it never load these classes -- every use sits behind a `withPlugin` guard.
    compileOnly(libs.agp.api)
}

gradlePlugin {
    website.set("https://github.com/a-sit-plus/testballoon-addons")
    vcsUrl.set("https://github.com/a-sit-plus/testballoon-addons.git")
    plugins {
        create("statusChannel") {
            id = "at.asitplus.testballoon.addons"
            implementationClass = "at.asitplus.testballoon.gradle.StatusChannelPlugin"
            displayName = "TestBalloon Addons status channel"
            description = "Opens a loopback listener so test progress bypasses the TeamCity report stream."
            tags.set(listOf("kotlin", "testing", "kotlin-multiplatform", "testballoon"))
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
