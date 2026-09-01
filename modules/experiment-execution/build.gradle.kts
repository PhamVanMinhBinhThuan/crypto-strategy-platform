plugins { id("crypto.java-library-conventions") }

dependencies {
    api(project(":modules:backtesting"))
    api(project(":modules:evaluation"))
    api(project(":modules:leaderboard"))
    implementation(project(":modules:experiment"))
    implementation(project(":modules:strategy-core"))
    implementation(project(":modules:combination"))
}
