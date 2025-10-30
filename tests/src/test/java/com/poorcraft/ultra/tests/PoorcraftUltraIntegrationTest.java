package com.poorcraft.ultra.tests;

import com.jme3.system.AppSettings;
import com.poorcraft.ultra.engine.EngineCore;
import com.poorcraft.ultra.shared.Config;
import com.poorcraft.ultra.shared.NativeLoader;
import com.poorcraft.ultra.ui.MainMenuState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Disabled("Requires active display and user interaction; enable manually for full integration run")
class PoorcraftUltraIntegrationTest {

    @Test
    void testFullStartupSequence(@TempDir Path tempDir) throws Exception {
        Config config = Config.load();
        NativeLoader.extractNatives(tempDir);

        ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
        ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(stdOut, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(stdErr, true, StandardCharsets.UTF_8));

        EngineCore engine = new EngineCore(config);
        AppSettings settings = new AppSettings(true);
        settings.setTitle(config.getString("window.title"));
        settings.setResolution(config.getInt("window.width"), config.getInt("window.height"));
        settings.setFullscreen(config.getBoolean("window.fullscreen"));
        settings.setVSync(config.getBoolean("window.vsync"));
        settings.setResizable(config.getBoolean("window.resizable"));
        engine.setSettings(settings);
        engine.setShowSettings(false);

        engine.getStateManager().attach(new MainMenuState());

        Thread gameThread = new Thread(engine::start, "integration-engine-thread");
        gameThread.start();
        Thread.sleep(3000);
        engine.stop();
        engine.awaitStop();
        gameThread.join(5000);

        System.setOut(originalOut);
        System.setErr(originalErr);

        String output = stdOut.toString(StandardCharsets.UTF_8);
        Assertions.assertThat(output)
                .contains("POORCRAFT ULTRA v0.1")
                .contains("Platform:")
                .contains("Configuration loaded")
                .contains("Launching engine")
                .contains("Application stopped");

        Assertions.assertThat(stdErr.toString(StandardCharsets.UTF_8)).isEmpty();
    }
}
