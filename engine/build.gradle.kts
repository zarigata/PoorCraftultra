dependencies {
    implementation(project(":shared"))
    implementation("org.jmonkeyengine:jme3-core:${rootProject.extra["jmeVersion"]}")
    implementation("org.jmonkeyengine:jme3-desktop:${rootProject.extra["jmeVersion"]}")
    implementation("org.jmonkeyengine:jme3-lwjgl3:${rootProject.extra["jmeVersion"]}")
    implementation("org.jmonkeyengine:jme3-effects:${rootProject.extra["jmeVersion"]}")
    implementation("org.jmonkeyengine:jme3-plugins:${rootProject.extra["jmeVersion"]}")
}
