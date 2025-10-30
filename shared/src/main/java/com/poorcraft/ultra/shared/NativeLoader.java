package com.poorcraft.ultra.shared;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class NativeLoader {

    private NativeLoader() {
    }

    public static void extractNatives(Path targetDir) throws IOException {
        if (targetDir == null) {
            throw new IllegalArgumentException("targetDir must not be null");
        }
        if (Files.notExists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        System.out.println("NativeLoader.extractNatives() called (stub implementation)");
    }

    public static void loadNative(String libraryName) {
        if (libraryName == null || libraryName.isBlank()) {
            throw new IllegalArgumentException("libraryName must not be null or blank");
        }
        System.out.println("NativeLoader.loadNative() called for: " + libraryName + " (stub implementation)");
    }
}
