plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    implementation(project(":modules:domain"))
    implementation(project(":modules:strategy-core"))
}
