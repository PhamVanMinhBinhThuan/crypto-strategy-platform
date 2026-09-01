plugins { id("crypto.java-library-conventions") }

dependencies {
    api(project(":modules:backtesting"))
    api(project(":modules:evaluation"))
    api(project(":modules:leaderboard"))
    implementation(project(":modules:experiment"))
    implementation(project(":modules:strategy-core"))
    implementation(project(":modules:combination"))
    implementation(platform(libs.spring.boot.dependencies))
    implementation(libs.spring.jdbc)

    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
