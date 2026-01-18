System.setProperty("KOTEST_NO_ASP_HELPER","true")
System.setProperty("TESTBALLOON_NO_ASP_HELPER","true")

rootProject.name = "testballoon-addons"

pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
        maven {
            url = uri("https://raw.githubusercontent.com/a-sit-plus/gradle-conventions-plugin/mvn/repo")
            name = "aspConventions"
        }
    }
}

include("commons")
include("internals")
include("freespec")
include("property")
include("datatest")
include("fixturegen")
include("fixturegen-freespec")
