plugins {
    `java-library`
}

val versionSteamworks4j: String by rootProject.extra
val versionSlf4j: String by rootProject.extra

dependencies {
    api(project(":shared"))

    implementation("com.code-disaster.steamworks4j:steamworks4j:$versionSteamworks4j")
    implementation("com.code-disaster.steamworks4j:steamworks4j-server:$versionSteamworks4j")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")

    listOf(
        "com.code-disaster.steamworks4j:steamworks4j:$versionSteamworks4j:natives-windows",
        "com.code-disaster.steamworks4j:steamworks4j:$versionSteamworks4j:natives-linux",
        "com.code-disaster.steamworks4j:steamworks4j:$versionSteamworks4j:natives-macos"
    ).forEach { runtimeOnly(it) }
}
