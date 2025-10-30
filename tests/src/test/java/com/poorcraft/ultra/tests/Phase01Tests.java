package com.poorcraft.ultra.tests;

import com.jme3.system.AppSettings;
import com.poorcraft.ultra.engine.EngineCore;
import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.NativeLoader;
import com.poorcraft.ultra.shared.PlatformInfo;
import com.poorcraft.ultra.ui.MainMenuState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

class Phase01Tests {

    private static final String JAVA_LIBRARY_PATH = "java.library.path";

    private String originalJavaLibraryPath;

    @BeforeEach
    void captureJavaLibraryPath() {
        originalJavaLibraryPath = System.getProperty(JAVA_LIBRARY_PATH);
    }

    @AfterEach
    void restoreJavaLibraryPath() {
        if (originalJavaLibraryPath != null) {
            System.setProperty(JAVA_LIBRARY_PATH, originalJavaLibraryPath);
        }
    }

    @Test
    void testNativeExtraction(@TempDir Path tempDir) throws IOException {
        NativeLoader.extractNatives(tempDir);
        Assertions.assertThat(tempDir)
                .isNotEmptyDirectory();
        Assertions.assertThat(Files.list(tempDir))
                .anySatisfy(path -> Assertions.assertThat(hasNativeExtension(path)).isTrue());
        String javaLibraryPath = System.getProperty(JAVA_LIBRARY_PATH);
        Assertions.assertThat(javaLibraryPath)
                .contains(tempDir.toAbsolutePath().toString());
    }

    @Test
    void testEngineConfigurationLoading() {
        Config config = Config.load();
        EngineCore engine = new EngineCore(config);
        Assertions.assertThat(engine).isNotNull();
        Assertions.assertThat(config.getString("window.title")).isNotBlank();
    }

    @Test
    void testMainMenuStateCreation() {
        MainMenuState mainMenuState = new MainMenuState();
        Assertions.assertThat(mainMenuState).isNotNull();
        Assertions.assertThat(mainMenuState.isInitialized()).isFalse();
    }

    @Test
    void testAppSettingsConfiguration() {
        Config config = Config.load();
        AppSettings settings = new AppSettings(true);
        settings.setTitle(config.getString("window.title"));
        settings.setResolution(config.getInt("window.width"), config.getInt("window.height"));
        settings.setFullscreen(config.getBoolean("window.fullscreen"));
        settings.setVSync(config.getBoolean("window.vsync"));
        settings.setResizable(config.getBoolean("window.resizable"));

        Assertions.assertThat(settings.getTitle()).isEqualTo("Poorcraft Ultra");
        Assertions.assertThat(settings.getWidth()).isEqualTo(1280);
        Assertions.assertThat(settings.getHeight()).isEqualTo(720);
        Assertions.assertThat(settings.isVSync()).isTrue();
    }

    @Test
    void testNativeClassifierDetection() {
        String classifier = invokeClassifier();
        PlatformInfo.OS os = PlatformInfo.getOS();
        PlatformInfo.Arch arch = PlatformInfo.getArch();

        switch (os) {
            case WINDOWS -> Assertions.assertThat(classifier).isIn("natives-windows", "natives-windows-x86");
            case LINUX -> Assertions.assertThat(classifier).isIn("natives-linux", "natives-linux-arm64");
            case MACOS -> Assertions.assertThat(classifier).isIn("natives-macos", "natives-macos-arm64");
            default -> Assertions.fail("Unsupported operating system: " + os);
        }

        if (arch == PlatformInfo.Arch.UNKNOWN) {
            Assertions.fail("Unsupported architecture: " + arch);
        }
    }

    @Test
    @Disabled("Requires graphics context; enable manually when display is available")
    void testApplicationLifecycle(@TempDir Path tempDir) throws Exception {
        NativeLoader.extractNatives(tempDir);
        Config config = Config.load();
        EngineCore engine = new EngineCore(config);
        AppSettings settings = new AppSettings(true);
        settings.setTitle(config.getString("window.title"));
        settings.setResolution(config.getInt("window.width"), config.getInt("window.height"));
        engine.setSettings(settings);
        engine.setShowSettings(false);
        engine.getStateManager().attach(new MainMenuState());

        Thread gameThread = new Thread(engine::start, "engine-test-thread");
        gameThread.start();

        Thread.sleep(2000);
        engine.stop();
        engine.awaitStop();
        gameThread.join(5000);

        Assertions.assertThat(gameThread.isAlive()).isFalse();
    }

    private boolean hasNativeExtension(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return filename.endsWith(".dll") || filename.endsWith(".so") || filename.endsWith(".dylib");
    }

    private String invokeClassifier() {
        try {
            var method = NativeLoader.class.getDeclaredMethod("getNativeClassifier");
            method.setAccessible(true);
            return (String) method.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to invoke getNativeClassifier", e);
        }
    }
}
