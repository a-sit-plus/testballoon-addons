plugins {
    // Deliberately not `kotlin-dsl`: it pins the Gradle-embedded Kotlin and its 1.8 language level, which
    // collides with the Kotlin Gradle plugin version this build already puts on the classpath.
    // No version: the Kotlin Gradle plugin is already on this build's classpath.
    id("org.jetbrains.kotlin.jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("signing")
}

val artifactVersion: String by extra
version = artifactVersion
group = rootProject.group

kotlin {
    jvmToolchain(17)
}

java {
    // Maven Central requires sources alongside the binary.
    withSourcesJar()
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

val javadocJar = tasks.register<Jar>("javadocRedirectJar") {
    archiveClassifier.set("javadoc")
    from(rootProject.rootDir.absolutePath + "/docs/javadoc")
}

publishing {
    publications {
        withType<MavenPublication> {
            // Only the real artifact carries jars. The plugin marker is a pom-only publication, and
            // attaching the *same* javadoc jar to both makes the marker's publish task consume the signature
            // file produced for this one, which Gradle rejects as an undeclared task dependency.
            if (name == "pluginMaven") artifact(javadocJar)
            pom {
                name.set("TestBalloon Addons Gradle Plugin")
                description.set(
                    "Out-of-band status reporting for TestBalloon Addons: real-time test progress on every target"
                )
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
