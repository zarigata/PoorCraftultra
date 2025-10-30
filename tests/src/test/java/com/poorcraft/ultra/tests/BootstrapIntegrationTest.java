package com.poorcraft.ultra.tests;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class BootstrapIntegrationTest {

    private static final Path LOG_FILE = Paths.get("logs", "poorcraft.log");

    @BeforeEach
    void cleanLogs() throws IOException {
        if (Files.exists(LOG_FILE)) {
            Files.delete(LOG_FILE);
        }
        Files.createDirectories(LOG_FILE.getParent());
    }

    @Test
    void testBootstrapExecution() throws Exception {
        ProcessResult result = runBootstrap();
        Assertions.assertThat(result.exitCode()).isEqualTo(0);
        String output = result.output();
        Assertions.assertThat(output).contains("POORCRAFT ULTRA");
        Assertions.assertThat(output).contains("Platform:");
        Assertions.assertThat(output).contains("Java:");
        Assertions.assertThat(output).contains("Configuration loaded");
        Assertions.assertThat(output).contains("CP v0.0 OK – Poorcraft Ultra – Java 17");

        Assertions.assertThat(LOG_FILE).exists();
        String logContent = Files.readString(LOG_FILE, StandardCharsets.UTF_8);
        Assertions.assertThat(logContent).contains("Platform:");
        Assertions.assertThat(logContent).contains("CP v0.0 OK – Poorcraft Ultra – Java 17");
    }

    @Test
    void testBootstrapLogsToFile() throws Exception {
        ProcessResult result = runBootstrap();
        Assertions.assertThat(result.exitCode()).isEqualTo(0);
        Assertions.assertThat(LOG_FILE).exists();
        List<String> logLines = Files.readAllLines(LOG_FILE, StandardCharsets.UTF_8);
        Assertions.assertThat(logLines).isNotEmpty();
        String firstLine = logLines.get(0);
        Assertions.assertThat(firstLine).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3} \\[[^]]+] \\w{4,5} .+");
        Assertions.assertThat(String.join("\n", logLines)).contains("CP v0.0 OK – Poorcraft Ultra – Java 17");
    }

    private ProcessResult runBootstrap() throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        Path javaBin = Paths.get(javaHome, "bin", isWindows ? "java.exe" : "java");
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder builder = new ProcessBuilder(
            javaBin.toString(),
            "-cp",
            classpath,
            "com.poorcraft.ultra.app.Bootstrap"
        );
        builder.directory(Paths.get("").toAbsolutePath().toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (InputStream inputStream = process.getInputStream()) {
            inputStream.transferTo(output);
        }
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, output.toString(StandardCharsets.UTF_8));
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
