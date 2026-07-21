plugins {
    java
}

val paperApiVersion: String by project
val yuemiLibsApiVersion: String by project

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("org.yuemi:YueMiLibs-api:$yuemiLibsApiVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
    testImplementation("org.yuemi:YueMiLibs-api:$yuemiLibsApiVersion")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
