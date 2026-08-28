import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
    id("org.springframework.boot")
    id("crypto.test-conventions")
}

group = "com.cryptostrategy.platform"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}
