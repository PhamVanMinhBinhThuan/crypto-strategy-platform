plugins {
    base
}

group = "com.cryptostrategy.platform"
version = "0.1.0-SNAPSHOT"

tasks.named("check") {
    dependsOn(subprojects.filter { it.buildFile.isFile }.map { "${it.path}:check" })
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}

tasks.named("clean") {
    dependsOn(subprojects.filter { it.buildFile.isFile }.map { "${it.path}:clean" })
}

tasks.register("supabaseIntegrationTest") {
    group = "verification"
    description = "Runs explicit remote Supabase readiness verification for both runtimes."
    dependsOn(
        ":apps:api:supabaseIntegrationTest",
        ":apps:worker:supabaseIntegrationTest",
    )
}
