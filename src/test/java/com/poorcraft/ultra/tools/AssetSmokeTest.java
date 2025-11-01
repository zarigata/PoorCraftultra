package com.poorcraft.ultra.tools;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetSmokeTest {

    @TempDir
    Path tempDir;

    @Test
    void generateAssetsEndToEnd() throws Exception {
        Optional<String> pythonCmd = resolvePythonCommand();
        Assumptions.assumeTrue(pythonCmd.isPresent(), "Python not available");

        Path projectRoot = Path.of(".").toAbsolutePath().normalize();
        Path outputBase = tempDir.resolve("assets");

        ProcessResult result = runGenerator(projectRoot, outputBase, Duration.ofMinutes(5), pythonCmd.get());
        assertTrue(result.exitCode == 0, () -> "Generator failed with exit code " + result.exitCode
                + "\nSTDOUT:\n" + result.stdout + "\nSTDERR:\n" + result.stderr);

        Path manifest = outputBase.resolve("manifest.json");
        assertTrue(Files.exists(manifest), "manifest.json should exist");

        Path blocksDir = outputBase.resolve("blocks");
        Path skinsDir = outputBase.resolve("skins");
        Path itemsDir = outputBase.resolve("items");

        assertTrue(Files.isDirectory(blocksDir), "blocks directory missing");
        assertTrue(Files.isDirectory(skinsDir), "skins directory missing");
        assertTrue(Files.isDirectory(itemsDir), "items directory missing");

        assertTrue(hasPng(blocksDir), "No PNG assets generated in blocks directory");
        assertTrue(hasPng(skinsDir), "No PNG assets generated in skins directory");
        assertTrue(hasPng(itemsDir), "No PNG assets generated in items directory");

        validateSample(blocksDir, 64, 64, "blocks");
        validateSample(itemsDir, 64, 64, "items");
        validateSample(skinsDir, 256, 256, "skins");

        AssetValidator validator = new AssetValidator();
        ValidationResult validationResult = validator.validate(outputBase);
        assertTrue(validationResult.valid(), () -> "Validation failed: " + validationResult.errors());
    }

    private Optional<String> resolvePythonCommand() throws IOException, InterruptedException {
        List<String> candidates = List.of("python", "python3", "py");
        for (String candidate : candidates) {
            ProcessBuilder builder = new ProcessBuilder(candidate, "--version");
            Process process;
            try {
                process = builder.start();
            } catch (IOException ex) {
                continue;
            }
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                continue;
            }
            if (process.exitValue() == 0) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private ProcessResult runGenerator(Path projectRoot, Path outputBase, Duration timeout, String pythonCmd) throws IOException, InterruptedException {
        String script = isWindows() ? "scripts\\dev\\gen-assets.bat" : "scripts/dev/gen-assets.sh";
        Path scriptPath = projectRoot.resolve(script);
        Assumptions.assumeTrue(Files.exists(scriptPath), "Asset generation script missing: " + script);

        List<String> command = new ArrayList<>();

        if (isWindows()) {
            command.add("cmd.exe");
            command.add("/c");
            command.add(scriptPath.toString());
        } else {
            command.add(scriptPath.toString());
        }
        command.add("--output-base");
        command.add(outputBase.toString());
        command.add("--seed");
        command.add("42");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectRoot.toFile());
        builder.redirectErrorStream(false);

        // Propagate preferred python interpreter when scripts consult PYTHON env var.
        builder.environment().putIfAbsent("PYTHON", pythonCmd);

        Process process = builder.start();
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Asset generation timed out after " + timeout.toSeconds() + " seconds");
        }

        int exitCode = process.exitValue();
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        return new ProcessResult(exitCode, stdout, stderr);
    }

    private boolean hasPng(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files.anyMatch(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"));
        }
    }

    private void validateSample(Path directory, int expectedWidth, int expectedHeight, String label) throws IOException {
        List<Path> pngFiles;
        try (Stream<Path> files = Files.list(directory)) {
            pngFiles = files.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .limit(3)
                    .collect(Collectors.toList());
        }

        assertFalse(pngFiles.isEmpty(), label + " directory should contain at least one PNG");

        for (Path png : pngFiles) {
            BufferedImage image = ImageIO.read(png.toFile());
            assertNotNull(image, "Failed to load PNG " + png);
            assertTrue(image.getWidth() == expectedWidth && image.getHeight() == expectedHeight,
                    () -> String.format("%s size mismatch for %s: expected %dx%d, got %dx%d",
                            label, png.getFileName(), expectedWidth, expectedHeight, image.getWidth(), image.getHeight()));
            assertTrue(Files.size(png) > 0, "PNG file is empty: " + png);
        }
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    }

    private String readStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
