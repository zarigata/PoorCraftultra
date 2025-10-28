plugins {
    `java-library`
}

val versionDiscordSdk: String by rootProject.extra
val versionSlf4j: String by rootProject.extra

dependencies {
    api(project(":shared"))

    implementation("club.minnced:discord-game-sdk4j:$versionDiscordSdk")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")

    listOf(
        "club.minnced:discord-game-sdk4j:$versionDiscordSdk:natives-windows",
        "club.minnced:discord-game-sdk4j:$versionDiscordSdk:natives-linux",
        "club.minnced:discord-game-sdk4j:$versionDiscordSdk:natives-macos"
    ).forEach { runtimeOnly(it) }
}
