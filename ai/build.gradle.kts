plugins {
    `java-library`
}

val versionSlf4j: String by rootProject.extra
val versionBrigadier: String by rootProject.extra
val versionVosk: String by rootProject.extra
val versionOkHttp: String by rootProject.extra
val versionJackson: String by rootProject.extra

dependencies {
    api(project(":shared"))
    api(project(":gameplay"))

    implementation("com.mojang:brigadier:$versionBrigadier")
    implementation("ai.picovoice:vosk:$versionVosk")
    implementation("com.squareup.okhttp3:okhttp:$versionOkHttp")
    implementation("com.fasterxml.jackson.core:jackson-databind:$versionJackson")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")

    listOf(
        "ai.picovoice:vosk:$versionVosk:natives-windows-amd64",
        "ai.picovoice:vosk:$versionVosk:natives-linux-amd64",
        "ai.picovoice:vosk:$versionVosk:natives-macos-x86_64",
        "ai.picovoice:vosk:$versionVosk:natives-macos-aarch64"
    ).forEach { runtimeOnly(it) }
}
