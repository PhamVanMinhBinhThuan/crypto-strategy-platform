plugins {
    id("crypto.spring-application-conventions")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(project(":modules:domain"))
    implementation(project(":modules:contracts"))
    implementation(project(":modules:market-data"))
    implementation(project(":modules:news"))
    implementation(project(":modules:persistence"))
    implementation(libs.jackson.databind)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.timelimiter)
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
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
        check(missing.isEmpty()) { "Missing required environment configuration: ${missing.joinToString()}" }
    }
}
