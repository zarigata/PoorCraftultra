package com.poorcraft.ultra.tests;

import com.poorcraft.ultra.engine.PoorcraftApp;
import com.poorcraft.ultra.shared.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class Phase0SmokeTest {

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testJmeInitialization() {
        Config config = Config.createDefault();
        PoorcraftApp app = new PoorcraftApp(config, true);

        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                app.start(true);
            } finally {
                latch.countDown();
            }
        });

        assertDoesNotThrow(() -> latch.await(2, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> future.cancel(true));
    }
}
