import org.gradle.api.tasks.JavaExec

plugins {
    application
}

val versionLogback: String by rootProject.extra
val versionSnakeYaml: String by rootProject.extra
val versionJackson: String by rootProject.extra

application {
    mainClass.set("com.poorcraft.ultra.app.Main")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

dependencies {
    implementation(project(":engine"))
    implementation(project(":shared"))

    implementation("org.yaml:snakeyaml:$versionSnakeYaml")
    implementation("com.fasterxml.jackson.core:jackson-databind:$versionJackson")

    runtimeOnly("ch.qos.logback:logback-classic:$versionLogback")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    systemProperty("dev.mode", System.getenv("DEV_MODE") ?: "false")
}
