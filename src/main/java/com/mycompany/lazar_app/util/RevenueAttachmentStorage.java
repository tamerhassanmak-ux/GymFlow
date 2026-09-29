package com.mycompany.lazar_app.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class RevenueAttachmentStorage {

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private RevenueAttachmentStorage() {
    }

    public static String copyToApplicationStorage(File source)
            throws IOException {

        if (source == null) {
            return null;
        }

        Path directory = Path.of(
                "data",
                "revenue_attachments"
        );

        Files.createDirectories(directory);

        String originalName = source.getName();
        String safeName = sanitize(originalName);
        String targetName = "REV-"
                + LocalDateTime.now().format(FILE_TIME)
                + "-"
                + safeName;

        Path target = directory.resolve(targetName);

        Files.copy(
                source.toPath(),
                target,
                StandardCopyOption.REPLACE_EXISTING
        );

        return target.toString();
    }

    private static String sanitize(String name) {

        if (name == null || name.isBlank()) {
            return "attachment";
        }

        return name
                .replaceAll("[^\\p{L}\\p{N}._-]+", "_")
                .replaceAll("_+", "_");
    }
}
