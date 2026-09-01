plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    api(project(":modules:market-data"))
    api(project(":modules:strategy-core"))
    implementation(project(":modules:combination"))
    api(project(":modules:experiment"))
}
