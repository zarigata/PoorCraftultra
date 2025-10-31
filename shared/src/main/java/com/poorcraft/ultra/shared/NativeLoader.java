package com.poorcraft.ultra.shared;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class NativeLoader {

    private static final Logger logger = Logger.getLogger(NativeLoader.class);
    private static final Object EXTRACTION_LOCK = new Object();
    private static final ConcurrentMap<Path, Boolean> CLEANUP_REGISTERED = new ConcurrentHashMap<>();

    private NativeLoader() {
    }

    public static void extractNatives(Path targetDir) throws IOException {
        Objects.requireNonNull(targetDir, "targetDir must not be null");

        synchronized (EXTRACTION_LOCK) {
            Path absoluteTarget = targetDir.toAbsolutePath();
            if (Files.notExists(absoluteTarget)) {
                Files.createDirectories(absoluteTarget);
            }
            if (!Files.isDirectory(absoluteTarget)) {
                throw new IOException("Target path is not a directory: " + absoluteTarget);
            }
            if (!Files.isWritable(absoluteTarget)) {
                throw new IOException("Target directory is not writable: " + absoluteTarget);
            }

            Path extractionDir = Files.createTempDirectory(absoluteTarget, "run-");
            registerShutdownCleanup(extractionDir);

            String classifier = getNativeClassifier();
            List<Path> nativeJars = locateNativeJars(classifier);
            if (nativeJars.isEmpty()) {
                logger.warn("No native libraries found on classpath for classifier {}. Expected LWJGL native jars with classifier hint in filename.", classifier);
                appendLibraryPath(extractionDir);
                return;
            }

            Set<String> extensions = getNativeExtensions();
            int extractedCount = 0;

            for (Path jarPath : nativeJars) {
                try (JarFile jarFile = new JarFile(jarPath.toFile())) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        if (entry.isDirectory()) {
                            continue;
                        }
                        String name = entry.getName();
                        if (!hasNativeExtension(name, extensions)) {
                            continue;
                        }
                        String filename = Paths.get(name).getFileName().toString();
                        Path outputFile = extractionDir.resolve(filename);
                        try (InputStream inputStream = jarFile.getInputStream(entry)) {
                            Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
                            outputFile.toFile().setReadable(true, false);
                            outputFile.toFile().setWritable(true, false);
                            outputFile.toFile().setExecutable(true, false);
                            extractedCount++;
                        }
                    }
                } catch (IOException exception) {
                    logger.error("Failed to extract natives from {}", jarPath, exception);
                }
            }

            if (extractedCount == 0) {
                logger.warn("No native entries extracted for classifier {} from {}", classifier, nativeJars);
            }

            appendLibraryPath(extractionDir);
            logger.info("Extracted {} native libraries to {}", extractedCount, extractionDir);
        }
    }

    public static void loadNative(String libraryName) {
        if (libraryName == null || libraryName.isBlank()) {
            throw new IllegalArgumentException("libraryName must not be null or blank");
        }
        try {
            System.loadLibrary(libraryName);
            logger.debug("Loaded native library {}", libraryName);
        } catch (UnsatisfiedLinkError error) {
            logger.error("Failed to load native library {}", libraryName, error);
            throw error;
        }
    }

    private static void appendLibraryPath(Path extractionDir) {
        String absoluteTargetPath = extractionDir.toAbsolutePath().toString();

        String previousLibraryPath = System.getProperty("java.library.path");
        String newLibraryPath = (previousLibraryPath == null || previousLibraryPath.isBlank())
                ? absoluteTargetPath
                : absoluteTargetPath + File.pathSeparator + previousLibraryPath;

        System.setProperty("java.library.path", newLibraryPath);

        String previousLwjglPath = System.getProperty("org.lwjgl.librarypath");
        String newLwjglPath = (previousLwjglPath == null || previousLwjglPath.isBlank())
                ? absoluteTargetPath
                : absoluteTargetPath + File.pathSeparator + previousLwjglPath;
        System.setProperty("org.lwjgl.librarypath", newLwjglPath);
    }

    private static void registerShutdownCleanup(Path extractionDir) {
        if (CLEANUP_REGISTERED.putIfAbsent(extractionDir, Boolean.TRUE) != null) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                cleanupDirectory(extractionDir);
            } catch (IOException exception) {
                logger.warn("Failed to clean up native extraction directory {}", extractionDir, exception);
            } finally {
                CLEANUP_REGISTERED.remove(extractionDir);
            }
        }, "native-cleanup-" + extractionDir.getFileName()));
    }

    private static void cleanupDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String getNativeClassifier() {
        PlatformInfo.OS os = PlatformInfo.getOS();
        PlatformInfo.Arch arch = PlatformInfo.getArch();

        return switch (os) {
            case WINDOWS -> {
                if (arch == PlatformInfo.Arch.X86_64) {
                    yield "natives-windows";
                }
                if (arch == PlatformInfo.Arch.X86) {
                    yield "natives-windows-x86";
                }
                throw new UnsupportedOperationException("Unsupported Windows architecture: " + arch);
            }
            case LINUX -> {
                if (arch == PlatformInfo.Arch.X86_64) {
                    yield "natives-linux";
                }
                if (arch == PlatformInfo.Arch.AARCH64) {
                    yield "natives-linux-arm64";
                }
                throw new UnsupportedOperationException("Unsupported Linux architecture: " + arch);
            }
            case MACOS -> {
                if (arch == PlatformInfo.Arch.X86_64) {
                    yield "natives-macos";
                }
                if (arch == PlatformInfo.Arch.AARCH64) {
                    yield "natives-macos-arm64";
                }
                throw new UnsupportedOperationException("Unsupported macOS architecture: " + arch);
            }
            case UNKNOWN -> throw new UnsupportedOperationException("Unsupported operating system");
        };
    }

    private static Set<String> getNativeExtensions() {
        return switch (PlatformInfo.getOS()) {
            case WINDOWS -> Set.of(".dll");
            case LINUX -> Set.of(".so");
            case MACOS -> Set.of(".dylib");
            case UNKNOWN -> Set.of();
        };
    }

    private static boolean hasNativeExtension(String name, Set<String> extensions) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static List<Path> locateNativeJars(String classifier) {
        String classPath = System.getProperty("java.class.path", "");
        if (classPath.isEmpty()) {
            return List.of();
        }

        String[] entries = classPath.split(File.pathSeparator);
        Set<Path> result = new LinkedHashSet<>();
        for (String entry : entries) {
            if (!entry.endsWith(".jar")) {
                continue;
            }
            if (!entry.contains(classifier)) {
                continue;
            }
            Path jarPath = Paths.get(entry);
            if (Files.isRegularFile(jarPath)) {
                result.add(jarPath);
                logger.debug("Detected native JAR: {}", jarPath);
            }
        }

        return new ArrayList<>(result);
    }
}
