plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    api(project(":modules:evaluation"))
    api(project(":modules:experiment"))
}
