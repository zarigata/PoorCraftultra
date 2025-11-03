plugins {
    java
    application
}

group = "com.poorcraft.ultra"
version = "3.3.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // jMonkeyEngine 3.7.0-stable
    implementation("org.jmonkeyengine:jme3-core:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-desktop:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-lwjgl3:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-plugins:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-effects:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-jbullet:3.7.0-stable")
    
    // Lemur UI
    implementation("com.simsilica:lemur:1.16.0")
    implementation("com.simsilica:lemur-proto:1.13.0")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.11")
    
    // JSON parsing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.3")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

application {
    mainClass.set("com.poorcraft.ultra.app.Main")
    applicationDefaultJvmArgs = listOf(
        "-Xmx2G",
        "-Djava.awt.headless=false"
    )
}

tasks.test {
    useJUnitPlatform()
    dependsOn("generateAssets", "validateAssets")
}

tasks.register<Exec>("generateAssets") {
    group = "build"
    description = "Generate procedural game assets using Python scripts"
    
    workingDir = projectDir
    
    if (System.getProperty("os.name").lowercase().contains("windows")) {
        commandLine("cmd", "/c", "scripts\\dev\\gen-assets.bat")
    } else {
        commandLine("bash", "scripts/dev/gen-assets.sh")
    }
}

tasks.register<JavaExec>("validateAssets") {
    group = "verification"
    description = "Validate generated assets (size, hash)"
    
    dependsOn(tasks.classes)
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.poorcraft.ultra.tools.AssetValidator")
}

tasks.register<Test>("smokeTest") {
    group = "verification"
    description = "Run smoke tests in headless mode"
    
    useJUnitPlatform {
        includeTags("smoke")
    }
    
    systemProperty("java.awt.headless", "true")
    testLogging {
        events("passed", "skipped", "failed")
    }
    
    dependsOn("generateAssets", "validateAssets")
}

tasks.named("build") {
    dependsOn("generateAssets")
    dependsOn("validateAssets")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    workingDir = projectDir
    dependsOn("generateAssets", "validateAssets")
}
