plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "dev.catbit"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":kroute"))
    compileOnly(libs.google.cloud.functions)
    testImplementation(kotlin("test"))
    testImplementation(project(":kroute"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = "dev.catbit",
        artifactId = "kroute-google-cloud-extension",
        version = "1.0.0"
    )

    pom {
        name = "KRoute Google Cloud Extension"
        description = "Google Cloud Functions adapter for KRoute."
        url = "https://github.com/RodrigoFerreira001/KRoute"

        licenses {
            license {
                name = "Apache License 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }

        developers {
            developer {
                id = "RodrigoFerreira001"
                name = "Rodrigo Ferreira"
                url = "https://github.com/RodrigoFerreira001"
            }
        }

        scm {
            url = "https://github.com/RodrigoFerreira001/KRoute"
            connection = "scm:git:git://github.com/RodrigoFerreira001/KRoute.git"
            developerConnection = "scm:git:ssh://git@github.com/RodrigoFerreira001/KRoute.git"
        }
    }
}
