package com.poorcraft.ultra.tests;

import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.ConfigLoadException;
import com.poorcraft.ultra.shared.Logger;
import com.poorcraft.ultra.shared.NativeLoader;
import com.poorcraft.ultra.shared.PlatformInfo;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class Phase00Tests {

    private static Path tempDir;

    @BeforeAll
    static void setup() throws IOException {
        tempDir = Files.createTempDirectory("poorcraft-test");
    }

    @AfterAll
    static void cleanup() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                    }
                });
        }
    }

    @Test
    void testPlatformDetection() {
        Assertions.assertThat(PlatformInfo.getOS()).isNotEqualTo(PlatformInfo.OS.UNKNOWN);
        Assertions.assertThat(PlatformInfo.getArch()).isNotEqualTo(PlatformInfo.Arch.UNKNOWN);
        Assertions.assertThat(PlatformInfo.getOSName()).isNotBlank();
        Assertions.assertThat(PlatformInfo.getArchName()).isNotBlank();
        Assertions.assertThat(PlatformInfo.isWindows() || PlatformInfo.isLinux() || PlatformInfo.isMacOS())
            .isTrue();
    }

    @Test
    void testConfigLoading() {
        Config config = Config.load();
        Assertions.assertThat(config).isNotNull();
        Assertions.assertThat(config.hasPath("window.title")).isTrue();
        Assertions.assertThat(config.getString("window.title")).isEqualTo("Poorcraft Ultra");
        Assertions.assertThat(config.getInt("window.width")).isEqualTo(1280);
        Assertions.assertThat(config.getInt("window.height")).isEqualTo(720);
        Assertions.assertThat(config.getInt("render.distance")).isEqualTo(8);
        Assertions.assertThat(config.getConfig("window")).isNotNull();
    }

    @Test
    void testConfigMissingKey() {
        Config config = Config.load();
        Assertions.assertThat(config.hasPath("nonexistent.key")).isFalse();
        Assertions.assertThatThrownBy(() -> config.getString("nonexistent.key"))
            .isInstanceOf(ConfigLoadException.class);
    }

    @Test
    void testLoggerCreation() {
        Logger logger = Logger.getLogger(Phase00Tests.class);
        Assertions.assertThat(logger).isNotNull();
        logger.trace("Trace message");
        logger.debug("Debug message");
        logger.info("Info message");
        logger.warn("Warn message");
        logger.error("Error message");
        logger.error("Error with exception", new RuntimeException("test"));
    }

    @Test
    void testLoggerLevels() {
        Logger logger = Logger.getLogger("test");
        Assertions.assertThat(logger.isInfoEnabled()).isTrue();
        Assertions.assertThat(logger.isWarnEnabled()).isTrue();
        Assertions.assertThat(logger.isErrorEnabled()).isTrue();
    }

    @Test
    void testNativeLoaderStub() throws IOException {
        Path target = tempDir.resolve("natives");
        NativeLoader.extractNatives(target);
        Assertions.assertThat(Files.exists(target)).isTrue();
        NativeLoader.loadNative("test-lib");
    }

    @Test
    void testBannerLogging() {
        Logger logger = Logger.getLogger("bannerTest");
        logger.banner("CP v0.0 OK – Poorcraft Ultra – Java 17 – " + PlatformInfo.getOS() + " " + PlatformInfo.getArch());
    }
}
