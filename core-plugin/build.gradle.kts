plugins {
    java
    id("com.gradleup.shadow") version "8.3.0" // Shadow plugin
}

val pluginName: String by project
val repoUrl: String by project
val developerId: String by project
val developerName: String by project
val apiVersion: String by project
val authors: String by project
val contributors: String by project
val pluginVersion: String = project.version.toString()
val paperApiVersion: String by project
val yuemiLibsApiVersion: String by project

tasks.processResources {
    val bstatsPluginId = project.findProperty("bstatsPluginId") as? String ?: ""
    val props = mapOf(
        "pluginName" to pluginName,
        "version" to pluginVersion,
        "apiVersion" to apiVersion,
        "authors" to authors,
        "contributors" to contributors,
        "bstatsPluginId" to bstatsPluginId
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}


dependencies {
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation("org.yuemi:mc-config-libs:1.0.1")
    implementation(project(":core-api"))
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("org.yuemi:YueMiLibs-api:$yuemiLibsApiVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    enabled = false
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set(pluginName)
    archiveVersion.set(pluginVersion)
    archiveClassifier.set("")

    configurations = listOf(project.configurations.runtimeClasspath.get())

    relocate("org.bstats", "${project.group}.libs.bstats")
    relocate("org.yuemi.config", "${project.group}.libs.config")

    manifest {
        attributes(
            "Implementation-Title" to pluginName,
            "Implementation-Version" to pluginVersion,
            "Implementation-Vendor" to developerName,
            "License" to "LGPL-3.0"
        )
    }
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}
