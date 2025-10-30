plugins {
    application
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":engine"))
    implementation(project(":ui"))
    implementation("ch.qos.logback:logback-classic:${rootProject.extra["logbackVersion"]}")
    implementation("ch.qos.logback:logback-core:${rootProject.extra["logbackVersion"]}")

    testImplementation("org.junit.jupiter:junit-jupiter:${rootProject.extra["junitVersion"]}")
}

application {
    mainClass.set("com.poorcraft.ultra.app.PoorcraftUltra")
}

tasks.named<JavaExec>("run") {
    jvmArgs = listOf("-Xmx1G", "-XX:+UseG1GC")
}
