plugins {
    id("crypto.spring-application-conventions")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":modules:domain"))
    implementation(project(":modules:contracts"))
    implementation(project(":modules:experiment"))
    implementation(project(":modules:backtesting"))
    implementation(project(":modules:market-data"))
    implementation(project(":modules:strategy-core"))
    implementation(project(":modules:strategies"))
    implementation(project(":modules:combination"))
    implementation(project(":modules:news"))
    implementation(project(":modules:evaluation"))
    implementation(project(":modules:leaderboard"))
    implementation(project(":modules:persistence"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(libs.spring.boot.starter.data.redis)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.named<Test>("test") {
    // Unit/contract tests exercise the consumer directly; Redis recovery runs in the
    // dedicated integration environment instead of opening localhost connections here.
    systemProperty("platform.realtime.streams.enabled", "false")
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
    description = "Runs API readiness verification against shared Supabase."
    testClassesDirs = supabaseIntegrationTest.output.classesDirs
    classpath = supabaseIntegrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    doFirst {
        val required = listOf(
            "DATABASE_URL",
            "DATABASE_USERNAME",
            "DATABASE_PASSWORD",
            "SUPABASE_JWT_ISSUER",
            "SUPABASE_JWT_JWKS_URI",
            "SUPABASE_JWT_AUDIENCE",
        )
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        check(missing.isEmpty()) { "Missing required environment configuration: ${missing.joinToString()}" }
    }
}
