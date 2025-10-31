dependencies {
    implementation(project(":shared"))
    implementation("com.fasterxml.jackson.core:jackson-databind:${rootProject.extra["jacksonVersion"]}")
    implementation("org.jmonkeyengine:jme3-core:${rootProject.extra["jmeVersion"]}")
    implementation("org.jmonkeyengine:jme3-jbullet:${rootProject.extra["jmeVersion"]}")
    implementation("com.simsilica:lemur:${rootProject.extra["lemurVersion"]}")
}
