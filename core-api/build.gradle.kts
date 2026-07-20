plugins {
    `java-library`
    `maven-publish`
}

val pluginName: String by project
val yuemiMavenSnapshot: String by project
val yuemiMavenRelease: String by project
val repoUrl: String by project
val developerId: String by project
val developerName: String by project

require(pluginName.isNotBlank())
require(yuemiMavenSnapshot.isNotBlank())
require(yuemiMavenRelease.isNotBlank())
require(repoUrl.isNotBlank())
require(developerId.isNotBlank())
require(developerName.isNotBlank())

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    archiveBaseName.set(pluginName)
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
}

tasks.named<Jar>("sourcesJar") {
    archiveBaseName.set(pluginName)
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("sources")
}

tasks.named<Jar>("javadocJar") {
    archiveBaseName.set(pluginName)
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            artifactId = "$pluginName-api"

            pom {
                name.set("$pluginName-api")
                description.set("Public API for $pluginName (PaperMC)")
                url.set(repoUrl)

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set(developerId)
                        name.set(developerName)
                    }
                }

                scm {
                    url.set(repoUrl)
                    connection.set("scm:git:$repoUrl.git")
                    developerConnection.set("scm:git:$repoUrl.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "YuemiMaven"
            val isSnapshot = project.version.toString().endsWith("SNAPSHOT")
            url = uri(if (isSnapshot) yuemiMavenSnapshot else yuemiMavenRelease)

            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
