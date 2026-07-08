import java.util.Properties

val localGradleConfig = Properties().apply {
    val file = file("gradle/config.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

gradle.beforeProject {
    localGradleConfig.forEach { (key, value) ->
        if (findProperty(key.toString()) == null) {
            extensions.extraProperties.set(key.toString(), value.toString())
        }
    }
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven { url = uri("https://maven.mozilla.org/maven2") }
    }
}

rootProject.name = "WebToApp"
include(":app")
include(":shell")
include(":clone-host")
