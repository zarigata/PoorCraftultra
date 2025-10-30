package com.poorcraft.ultra.shared;

import java.util.Locale;

public final class PlatformInfo {

    public enum OS {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    public enum Arch {
        X86_64,
        AARCH64,
        X86,
        UNKNOWN
    }

    private static final OS CURRENT_OS;
    private static final Arch CURRENT_ARCH;

    static {
        CURRENT_OS = detectOS();
        CURRENT_ARCH = detectArch();
        System.out.println("[PlatformInfo] Detected OS: " + CURRENT_OS + " (" + getOSName() + ")");
        System.out.println("[PlatformInfo] Detected Arch: " + CURRENT_ARCH + " (" + getArchName() + ")");
    }

    private PlatformInfo() {
    }

    private static OS detectOS() {
        String osName = getOSName().toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return OS.WINDOWS;
        }
        if (osName.contains("nux") || osName.contains("nix")) {
            return OS.LINUX;
        }
        if (osName.contains("freebsd")) {
            return OS.LINUX;
        }
        if (osName.contains("sunos") || osName.contains("solaris")) {
            return OS.LINUX;
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return OS.MACOS;
        }
        return OS.UNKNOWN;
    }

    private static Arch detectArch() {
        String archName = getArchName().toLowerCase(Locale.ROOT);
        if (archName.equals("amd64") || archName.equals("x86_64")) {
            return Arch.X86_64;
        }
        if (archName.equals("aarch64") || archName.equals("arm64")) {
            return Arch.AARCH64;
        }
        if (archName.equals("x86") || archName.equals("i386") || archName.equals("i686")) {
            return Arch.X86;
        }
        return Arch.UNKNOWN;
    }

    public static OS getOS() {
        return CURRENT_OS;
    }

    public static Arch getArch() {
        return CURRENT_ARCH;
    }

    public static String getOSName() {
        return System.getProperty("os.name", "unknown");
    }

    public static String getArchName() {
        return System.getProperty("os.arch", "unknown");
    }

    public static boolean isWindows() {
        return CURRENT_OS == OS.WINDOWS;
    }

    public static boolean isLinux() {
        return CURRENT_OS == OS.LINUX;
    }

    public static boolean isMacOS() {
        return CURRENT_OS == OS.MACOS;
    }
}
