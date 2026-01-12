import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import java.time.Duration

System.setProperty("KOTEST_NO_ASP_HELPER", "true")
System.setProperty("TESTBALLOON_NO_ASP_HELPER","true")
plugins {
    alias(libs.plugins.asp)
    alias(libs.plugins.agp) apply false
    alias(libs.plugins.kmp) apply false
}
group = "at.asitplus.testballoon"

//access dokka plugin from conventions plugin's classpath in root project → no need to specify version
apply(plugin = "org.jetbrains.dokka")
tasks.getByName("dokkaHtmlMultiModule") {
    (this as DokkaMultiModuleTask)
    outputDirectory.set(File("${buildDir}/dokka"))
    moduleName.set("TestBalloon-Addons")
}

subprojects { repositories {mavenLocal()} }

allprojects {
    apply(plugin = "org.jetbrains.dokka")
    group = rootProject.group
}
nexusPublishing {
    transitionCheckOptions {
        maxRetries.set(400)
        delayBetween.set(Duration.ofSeconds(20))
    }
    connectTimeout.set(Duration.ofMinutes(15))
    clientTimeout.set(Duration.ofMinutes(40))
}