import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("java-library")
    id("application")
}

fun RepositoryHandler.jcenter() = maven("https://jcenter.bintray.com")

val versionJme = "3.7.0-stable"
val versionSlf4j = "2.0.9"
val versionLogback = "1.4.11"
val versionSnakeYaml = "2.2"
val versionJackson = "2.15.2"
val versionJunit = "5.10.1"
val versionMockito = "5.5.0"
val versionAssertJ = "3.24.2"
val versionSteamworks4j = "1.9.0"
val versionDiscordSdk = "0.6.5"
val versionVosk = "0.3.45"
val versionKryo = "5.5.0"
val versionBrigadier = "1.0.18"
val versionZayEs = "1.6.0"
val versionOpenSimplex2 = "0.0.1"
val versionZstdJni = "1.5.5-1"
val versionOkHttp = "4.11.0"
val versionLemur = "1.16.0"

extra["versionJme"] = versionJme
extra["versionSlf4j"] = versionSlf4j
extra["versionLogback"] = versionLogback
extra["versionSnakeYaml"] = versionSnakeYaml
extra["versionJackson"] = versionJackson
extra["versionJunit"] = versionJunit
extra["versionMockito"] = versionMockito
extra["versionAssertJ"] = versionAssertJ
extra["versionSteamworks4j"] = versionSteamworks4j
extra["versionDiscordSdk"] = versionDiscordSdk
extra["versionVosk"] = versionVosk
extra["versionKryo"] = versionKryo
extra["versionBrigadier"] = versionBrigadier
extra["versionZayEs"] = versionZayEs
extra["versionOpenSimplex2"] = versionOpenSimplex2
extra["versionZstdJni"] = versionZstdJni
extra["versionOkHttp"] = versionOkHttp
extra["versionLemur"] = versionLemur

subprojects {
    pluginManager.apply("java-library")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        }

        tasks.withType(JavaCompile::class.java).configureEach {
            options.encoding = "UTF-8"
        }
    }

    tasks.withType(org.gradle.api.tasks.javadoc.Javadoc::class.java).configureEach {
        options.encoding = "UTF-8"
    }

    tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
        useJUnitPlatform()
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
        failFast = true
    }

    dependencies {
        implementation("org.slf4j:slf4j-api:$versionSlf4j")
        testImplementation("org.junit.jupiter:junit-jupiter:$versionJunit")
    }
}
