subprojects {
    apply(plugin = "java")

    group = property("group") as String
    version = property("version") as String

    // Configure java extension safely
    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.yuemi.my.id/repository/maven-releases/")
    }
}
