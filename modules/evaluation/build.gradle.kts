plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    api(project(":modules:backtesting"))
    api(project(":modules:experiment"))
}
