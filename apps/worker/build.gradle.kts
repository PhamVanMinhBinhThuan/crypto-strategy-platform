plugins {
    id("crypto.spring-application-conventions")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
}

val supabaseIntegrationTest by sourceSets.creating

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
