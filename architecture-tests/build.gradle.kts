plugins {
    id("crypto.test-conventions")
}

tasks.test {
    systemProperty("repository.root", rootProject.projectDir.absolutePath)
}
