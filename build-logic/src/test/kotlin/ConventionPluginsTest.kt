import java.nio.file.Files
import java.nio.file.Path
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ConventionPluginsTest {
    @TempDir
    lateinit var projectDirectory: Path

    @Test
    fun `java library convention pins Java 21 and exposes lifecycle tasks`() {
        val output = runFixture(
            pluginId = "crypto.java-library-conventions",
            extraBuild = """
                tasks.register("printConvention") {
                    doLast {
                        println("LANGUAGE_VERSION=" + java.toolchain.languageVersion.get())
                    }
                }
            """.trimIndent(),
            task = "printConvention",
        )

        assertTrue(output.contains("LANGUAGE_VERSION=21"))
        assertTrue(runTaskList("crypto.java-library-conventions").contains("sourcesJar"))
        assertTrue(runTaskList("crypto.java-library-conventions").contains("check"))
    }

    @Test
    fun `test convention exposes JUnit Platform lifecycle`() {
        val output = runTaskList("crypto.test-conventions")

        assertTrue(output.contains("test"))
        assertTrue(output.contains("check"))
    }

    @Test
    fun `Spring application convention exposes runnable Boot tasks`() {
        val output = runTaskList("crypto.spring-application-conventions")

        assertTrue(output.contains("bootRun"))
        assertTrue(output.contains("bootJar"))
        assertTrue(output.contains("check"))
    }

    private fun runTaskList(pluginId: String): String =
        runFixture(pluginId = pluginId, task = "tasks", extraArguments = arrayOf("--all"))

    private fun runFixture(
        pluginId: String,
        task: String,
        extraBuild: String = "",
        extraArguments: Array<String> = emptyArray(),
    ): String {
        Files.writeString(projectDirectory.resolve("settings.gradle.kts"), "rootProject.name = \"fixture\"\n")
        Files.writeString(
            projectDirectory.resolve("build.gradle.kts"),
            """
                plugins {
                    id("$pluginId")
                }

                $extraBuild
            """.trimIndent(),
        )

        return GradleRunner.create()
            .withProjectDir(projectDirectory.toFile())
            .withPluginClasspath()
            .withArguments("--stacktrace", task, *extraArguments)
            .build()
            .output
    }
}
