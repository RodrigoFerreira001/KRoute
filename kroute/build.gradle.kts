plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "dev.catbit"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates(
        groupId = "dev.catbit",
        artifactId = "kroute",
        version = "1.0-SNAPSHOT"
    )
}
