plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "KRoute"

include(":kroute")
include(":kroute-google-cloud-extension")
