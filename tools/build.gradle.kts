import java.io.File

dependencies {
    implementation(project(":shared"))
}

fun Project.findPythonCommand(): String {
    val candidates = listOf("python", "python3")
    for (candidate in candidates) {
        val result = exec {
            commandLine(candidate, "--version")
            isIgnoreExitValue = true
        }
        if (result.exitValue == 0) {
            return candidate
        }
    }
    throw GradleException("Python 3.7+ is required to generate assets. Please install Python and add it to PATH.")
}

tasks.register<Exec>("generateAssets") {
    group = "build"
    description = "Generates procedural block textures and player/NPC skins using Python"
    workingDir = rootProject.projectDir

    val pythonCommand = project.objects.property(String::class.java)

    inputs.files(
        rootProject.file("scripts/assets/generate_assets.py"),
        rootProject.file("scripts/assets/test_generate_assets.py"),
        rootProject.file("scripts/assets/requirements.txt"),
    )
    outputs.dirs(
        rootProject.file("assets/textures"),
        rootProject.file("assets/skins"),
    )

    doFirst {
        pythonCommand.set(project.findPythonCommand())
        val python = pythonCommand.get()
        logger.lifecycle("Installing Python dependencies (Pillow, jsonschema)...")
        project.exec {
            commandLine(python, "-m", "pip", "install", "--user", "-r", "scripts/assets/requirements.txt")
            isIgnoreExitValue = true
            workingDir = rootProject.projectDir
        }
        commandLine(python, "scripts/assets/generate_assets.py")
    }

    doLast {
        val python = pythonCommand.get()
        val testResult = project.exec {
            commandLine(python, "scripts/assets/test_generate_assets.py")
            workingDir = rootProject.projectDir
            isIgnoreExitValue = false
        }
        if (testResult.exitValue != 0) {
            throw GradleException("Asset validation tests failed.")
        }

        val expectedFiles = listOf(
            "assets/textures/blocks_atlas.png",
            "assets/textures/blocks_atlas.json",
            "assets/textures/manifest.json",
            "assets/skins/player.png",
            "assets/skins/npc_red.png",
            "assets/skins/npc_blue.png",
            "assets/skins/npc_green.png",
            "assets/skins/npc_yellow.png",
            "assets/skins/npc_purple.png",
        )

        val missing = expectedFiles
            .map { rootProject.file(it) }
            .filterNot(File::exists)

        if (missing.isNotEmpty()) {
            throw GradleException("Expected output files not found: ${missing.joinToString(", ") { it.relativeTo(rootProject.projectDir).path }}")
        }

        logger.lifecycle("Asset validation tests passed")
    }
}
