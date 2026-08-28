plugins {
    id("crypto.test-conventions")
}

dependencies {
    testImplementation(libs.archunit.junit5)

    testImplementation(project(":apps:api"))
    testImplementation(project(":apps:worker"))
    testImplementation(project(":modules:domain"))
    testImplementation(project(":modules:contracts"))
    testImplementation(project(":modules:market-data"))
    testImplementation(project(":modules:strategy-core"))
    testImplementation(project(":modules:strategies"))
    testImplementation(project(":modules:combination"))
    testImplementation(project(":modules:backtesting"))
    testImplementation(project(":modules:evaluation"))
    testImplementation(project(":modules:experiment"))
    testImplementation(project(":modules:search"))
    testImplementation(project(":modules:leaderboard"))
    testImplementation(project(":modules:news"))
    testImplementation(project(":modules:persistence"))
}

tasks.test {
    systemProperty("repository.root", rootProject.projectDir.absolutePath)
}
