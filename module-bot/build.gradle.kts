plugins {
    java
}

val paperApiVersion: String by project
val yuemiLibsApiVersion: String by project

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("org.yuemi:YueMiLibs-api:$yuemiLibsApiVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
