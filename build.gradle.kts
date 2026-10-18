plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "local"
version = "1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-tree:9.7.1")
}

tasks.shadowJar {
    archiveFileName.set("local.jar")
    manifest {
        attributes["Main-Class"] = "Main"
    }
}
