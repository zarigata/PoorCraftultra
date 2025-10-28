package com.poorcraft.ultra.app;

import com.poorcraft.ultra.shared.util.Logger;

public final class PlatformCheck {

    public enum OS {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    private static final org.slf4j.Logger LOG = Logger.getLogger(PlatformCheck.class);

    private PlatformCheck() {
    }

    public static OS detectOperatingSystem() {
        String name = System.getProperty("os.name", "").toLowerCase();
        if (name.contains("win")) {
            return OS.WINDOWS;
        }
        if (name.contains("mac")) {
            return OS.MACOS;
        }
        if (name.contains("nux") || name.contains("nix")) {
            return OS.LINUX;
        }
        return OS.UNKNOWN;
    }

    public static int getJavaMajorVersion() {
        String version = System.getProperty("java.version", "17");
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int dotIndex = version.indexOf('.');
        if (dotIndex > -1) {
            version = version.substring(0, dotIndex);
        }
        int dashIndex = version.indexOf('-');
        if (dashIndex > -1) {
            version = version.substring(0, dashIndex);
        }
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException ex) {
            LOG.warn("Unable to parse Java version: {}", version);
            return 0;
        }
    }

    public static void validateJavaVersion() {
        int major = getJavaMajorVersion();
        if (major < 17) {
            throw new IllegalStateException("Java 17 or newer is required. Detected Java " + major);
        }
    }

    public static String getNativesPath() {
        OS os = detectOperatingSystem();
        return switch (os) {
            case WINDOWS -> "natives/windows-x64";
            case LINUX -> "natives/linux-x64";
            case MACOS -> isArm64() ? "natives/macos-arm64" : "natives/macos-x64";
            default -> "natives";
        };
    }

    private static boolean isArm64() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        return arch.contains("aarch64") || arch.contains("arm64");
    }

    public static void logEnvironment() {
        OS os = detectOperatingSystem();
        int javaVersion = getJavaMajorVersion();
        LOG.info("Detected operating system: {}", os);
        LOG.info("Detected Java version: {}", javaVersion);
        LOG.info("Selected natives path: {}", getNativesPath());
    }
}
