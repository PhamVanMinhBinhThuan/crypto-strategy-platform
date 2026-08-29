import org.gradle.api.tasks.compile.JavaCompile

plugins {
    `java-library`
    id("crypto.test-conventions")
}

group = "com.cryptostrategy.platform"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}
