plugins { id("crypto.java-library-conventions") }

dependencies {
    implementation(project(":modules:contracts"))
    implementation(project(":modules:market-data"))
    api(project(":modules:backtesting"))
    api(project(":modules:evaluation"))
    api(project(":modules:leaderboard"))
    implementation(project(":modules:experiment"))
    api(project(":modules:search"))
    implementation(project(":modules:strategy-core"))
    implementation(project(":modules:combination"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.jdbc)
    implementation(libs.jackson.databind)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
