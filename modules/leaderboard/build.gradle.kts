plugins {
    id("crypto.java-library-conventions")
}

dependencies {
    api(project(":modules:domain"))
    api(project(":modules:evaluation"))
    api(project(":modules:experiment"))
    testImplementation(platform(libs.spring.boot.dependencies))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
