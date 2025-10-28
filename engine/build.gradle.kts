plugins {
    `java-library`
}

val versionJme: String by rootProject.extra
val versionSlf4j: String by rootProject.extra
val versionZayEs: String by rootProject.extra

dependencies {
    api(project(":shared"))

    api("org.jmonkeyengine:jme3-core:$versionJme")
    implementation("org.jmonkeyengine:jme3-desktop:$versionJme")
    implementation("org.jmonkeyengine:jme3-lwjgl3:$versionJme")
    implementation("org.jmonkeyengine:jme3-effects:$versionJme")
    implementation("org.jmonkeyengine:jme3-terrain:$versionJme")
    implementation("com.simsilica:zay-es:$versionZayEs")
    implementation("org.slf4j:slf4j-api:$versionSlf4j")

}
