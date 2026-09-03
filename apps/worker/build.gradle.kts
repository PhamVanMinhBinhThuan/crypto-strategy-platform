plugins {
    id("crypto.spring-application-conventions")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.validation)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)

    implementation(project(":modules:contracts"))
    implementation(project(":modules:domain"))
    implementation(project(":modules:market-data"))
    implementation(project(":modules:news"))
    implementation(project(":modules:strategy-core"))
    implementation(project(":modules:combination"))
    implementation(project(":modules:strategies"))
    implementation(project(":modules:experiment"))
    implementation(project(":modules:backtesting"))
    implementation(project(":modules:evaluation"))
    implementation(project(":modules:experiment-execution"))
    implementation(project(":modules:search"))
    implementation(project(":modules:leaderboard"))
    implementation(project(":modules:persistence"))

    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.timelimiter)

    runtimeOnly(libs.postgresql)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation("org.assertj:assertj-core")
    testImplementation(libs.mockwebserver)
    testRuntimeOnly("com.h2database:h2")
}

val supabaseIntegrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[supabaseIntegrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[supabaseIntegrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

tasks.register<Test>("supabaseIntegrationTest") {
    group = "verification"
    description = "Runs Worker readiness verification against shared Supabase."
    testClassesDirs = supabaseIntegrationTest.output.classesDirs
    classpath = supabaseIntegrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    doFirst {
        val required = listOf("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD")
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        check(missing.isEmpty()) { "Missing required environment configuration: " }
    }
}
