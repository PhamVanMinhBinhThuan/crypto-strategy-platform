plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    implementation(platform(libs.spring.boot.dependencies))
    implementation(project(":modules:domain"))
    implementation(project(":modules:market-data"))
    implementation(project(":modules:strategy-core"))
    implementation(project(":modules:news"))
    implementation(project(":modules:experiment"))
    implementation(project(":modules:backtesting"))
    implementation(project(":modules:evaluation"))
    implementation(project(":modules:leaderboard"))
    implementation(project(":modules:experiment-execution"))
    implementation(libs.spring.jdbc)
    implementation(libs.jackson.databind)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.postgresql)
    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

val newsIntegrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[newsIntegrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[newsIntegrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

tasks.register<Test>("newsIntegrationTest") {
    group = "verification"
    description = "Runs F-008 News persistence verification against isolated PostgreSQL."
    testClassesDirs = newsIntegrationTest.output.classesDirs
    classpath = newsIntegrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    doFirst {
        val required = listOf("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD")
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        check(missing.isEmpty()) { "Missing local database configuration: ${missing.joinToString()}" }
    }
}

val marketDataIntegrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[marketDataIntegrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[marketDataIntegrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

tasks.register<Test>("marketDataIntegrationTest") {
    group = "verification"
    description = "Runs F-003 persistence verification against isolated local Supabase."
    testClassesDirs = marketDataIntegrationTest.output.classesDirs
    classpath = marketDataIntegrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    doFirst {
        val required = listOf("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD")
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        check(missing.isEmpty()) { "Missing local database configuration: ${missing.joinToString()}" }
    }
}

val strategyIntegrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[strategyIntegrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[strategyIntegrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

tasks.register<Test>("strategyIntegrationTest") {
    group = "verification"
    description = "Runs F-004 Strategy persistence verification against isolated local Supabase."
    testClassesDirs = strategyIntegrationTest.output.classesDirs
    classpath = strategyIntegrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    doFirst {
        val required = listOf("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD")
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        check(missing.isEmpty()) { "Missing local database configuration: ${missing.joinToString()}" }
    }
}

val experimentIntegrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[experimentIntegrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[experimentIntegrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

tasks.register<Test>("experimentIntegrationTest") {
    group = "verification"
    description = "Runs F-005 Experiment persistence and concurrency verification against isolated local Supabase."
    testClassesDirs = experimentIntegrationTest.output.classesDirs
    classpath = experimentIntegrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    doFirst {
        val required = listOf("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD")
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        check(missing.isEmpty()) { "Missing local database configuration: ${missing.joinToString()}" }
    }
}

val backtestEvaluationLeaderboardIntegrationTest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

configurations[backtestEvaluationLeaderboardIntegrationTest.implementationConfigurationName]
    .extendsFrom(configurations.testImplementation.get())
configurations[backtestEvaluationLeaderboardIntegrationTest.runtimeOnlyConfigurationName]
    .extendsFrom(configurations.testRuntimeOnly.get())

tasks.register<Test>("backtestEvaluationLeaderboardIntegrationTest") {
    group = "verification"
    description = "Runs F-006 persistence verification against an isolated test database."
    testClassesDirs = backtestEvaluationLeaderboardIntegrationTest.output.classesDirs
    classpath = backtestEvaluationLeaderboardIntegrationTest.runtimeClasspath
    shouldRunAfter(tasks.test)
    doFirst {
        val required = listOf("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD")
        val missing = required.filter { System.getenv(it).isNullOrBlank() }
        check(missing.isEmpty()) { "Missing isolated database configuration: ${missing.joinToString()}" }
    }
}
