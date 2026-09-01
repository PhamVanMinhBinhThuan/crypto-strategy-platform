pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "crypto-strategy-platform"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(
    ":apps:api",
    ":apps:worker",
    ":modules:domain",
    ":modules:contracts",
    ":modules:market-data",
    ":modules:strategy-core",
    ":modules:strategies",
    ":modules:combination",
    ":modules:backtesting",
    ":modules:evaluation",
    ":modules:experiment",
    ":modules:experiment-execution",
    ":modules:search",
    ":modules:leaderboard",
    ":modules:news",
    ":modules:persistence",
    ":architecture-tests",
)
