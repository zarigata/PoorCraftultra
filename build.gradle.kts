plugins {
    java
    application
    id("org.beryx.jlink") version "3.0.1"
}

group = "com.poorcraft"
version = "0.1.0-SNAPSHOT"

// Configure Java 17 toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io") // For discord-game-sdk4j
}

// Detect OS for LWJGL natives using System properties
val lwjglNatives = run {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    
    when {
        osName.contains("win") -> "natives-windows"
        osName.contains("linux") -> "natives-linux"
        osName.contains("mac") || osName.contains("darwin") -> {
            if (osArch.contains("aarch64") || osArch.contains("arm")) {
                "natives-macos-arm64"
            } else {
                "natives-macos"
            }
        }
        else -> throw GradleException("Unsupported OS: $osName")
    }
}

// Phase 0A: Asset generation helper variables
val isWindows = System.getProperty("os.name").lowercase().contains("win")
val pythonCmd = if (isWindows) "py" else "python3"
val assetsRoot = layout.projectDirectory.dir("assets")
val toolsAssets = layout.projectDirectory.dir("tools/assets")

dependencies {
    // jMonkeyEngine 3.7.0-stable (Oct 2024, Java 17 compatible)
    // jME pulls in LWJGL transitively; no explicit LWJGL BOM needed
    implementation("org.jmonkeyengine:jme3-core:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-desktop:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-lwjgl3:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-effects:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-terrain:3.7.0-stable")
    implementation("org.jmonkeyengine:jme3-plugins:3.7.0-stable")

    // Logging: SLF4J + Logback
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Configuration: Jackson YAML
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.3")

    // Testing: JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

application {
    mainClass.set("com.poorcraft.ultra.app.Main")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.poorcraft.ultra.app.Main",
            "Implementation-Title" to "Poorcraft Ultra",
            "Implementation-Version" to project.version
        )
    }
}

// Phase 0A: Asset generation and validation pipeline
// Python scripts generate procedural textures; Java validator enforces dimensions and manifest integrity.
// Build fails if assets invalid.

tasks.register<Exec>("generateAssets") {
    group = "assets"
    description = "Generate procedural assets via Python scripts"
    
    // Inputs: Python scripts
    inputs.files(toolsAssets.asFileTree)
    
    // Outputs: Generated assets
    outputs.dir(assetsRoot.dir("blocks"))
    outputs.dir(assetsRoot.dir("skins"))
    outputs.dir(assetsRoot.dir("items"))
    outputs.file(assetsRoot.file("manifest.json"))
    
    // Working directory: project root (not tools/assets)
    workingDir(layout.projectDirectory)
    
    // Command: run wrapper script from project root
    if (isWindows) {
        commandLine("cmd", "/c", "scripts\\dev\\gen-assets.bat")
    } else {
        commandLine("bash", "scripts/dev/gen-assets.sh")
    }
    
    doFirst {
        println("Generating procedural assets...")
    }
    
    doLast {
        println("Assets generated successfully")
    }
}

tasks.register<JavaExec>("validateAssets") {
    group = "assets"
    description = "Validate generated assets (dimensions, manifest)"
    
    dependsOn("generateAssets", "classes")
    
    mainClass.set("com.poorcraft.ultra.tools.AssetValidatorCLI")
    args(assetsRoot.asFile.absolutePath)
    classpath = sourceSets["main"].runtimeClasspath
    
    doFirst {
        println("Validating assets...")
    }
    
    doLast {
        println("Asset validation passed")
    }
}

// Wire validation into build
tasks.named("build") {
    dependsOn("validateAssets")
}

// Badass JLink configuration (stub for Phase 11 jpackage)
jlink {
    // Future: configure jlink modules, jpackage installers per OS
    // For now, just ensure plugin applies cleanly
}
