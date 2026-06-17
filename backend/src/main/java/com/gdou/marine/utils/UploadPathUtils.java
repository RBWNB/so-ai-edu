package com.gdou.marine.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class UploadPathUtils {

    private UploadPathUtils() {
    }

    public static Path resolveProjectRoot() {
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path fileName = currentDir.getFileName();

        if (fileName != null && "backend".equalsIgnoreCase(fileName.toString()) && currentDir.getParent() != null) {
            return currentDir.getParent().normalize();
        }
        return currentDir;
    }

    public static Path resolvePathFromProjectRoot(String pathValue) {
        Path configured = Paths.get(pathValue);
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        return resolveProjectRoot().resolve(configured).normalize();
    }

    public static String toFileResourceLocation(Path path) {
        String normalized = path.toString().replace("\\", "/");
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        return "file:" + normalized;
    }
}
