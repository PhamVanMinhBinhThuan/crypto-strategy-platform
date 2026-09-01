plugins {
    java
}

extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    "testImplementation"(platform("org.junit:junit-bom:5.12.2"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("debug", "false")
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
