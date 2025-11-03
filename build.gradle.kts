import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import java.time.Duration

System.setProperty("KOTEST_NO_ASP_HELPER","true")

plugins {
    id("at.asitplus.gradle.conventions") version "20251023"
    kotlin("multiplatform") version libs.versions.kotlin.get() apply false
    kotlin("plugin.serialization") version libs.versions.kotlin.get() apply false
    id("com.android.kotlin.multiplatform.library") version "8.12.3" apply (false)
}
group = "at.asitplus.testballoon"

//access dokka plugin from conventions plugin's classpath in root project → no need to specify version
apply(plugin = "org.jetbrains.dokka")
tasks.getByName("dokkaHtmlMultiModule") {
    (this as DokkaMultiModuleTask)
    outputDirectory.set(File("${buildDir}/dokka"))
    moduleName.set("TestBalloon-Addons")
}

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