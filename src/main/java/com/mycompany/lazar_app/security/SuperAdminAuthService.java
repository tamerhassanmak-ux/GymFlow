package com.mycompany.lazar_app.security;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SuperAdminAuthService {

    private static final Path SECURITY_DIR =
            Paths.get(System.getProperty("user.home"), ".lazar-gym");
    private static final Path HASH_FILE = SECURITY_DIR.resolve("superadmin.auth");

    private SuperAdminAuthService() {}

    public static boolean isConfigured() {
        return Files.exists(HASH_FILE);
    }

    public static synchronized void setPassword(String password) {
        if (isConfigured()) throw new SecurityException("Use password change with the current password.");
        writePassword(password);
    }

    public static synchronized void changePassword(String currentPassword, String newPassword) {
        if (!verify(currentPassword)) throw new SecurityException("Current password is incorrect.");
        writePassword(newPassword);
        AuthorizationService.logout();
    }

    private static void writePassword(String password) {
        if (password == null || password.length() < 8
                || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must contain at least 8 characters and at most 72 UTF-8 bytes.");
        }
        Path temporary = null;
        try {
            Files.createDirectories(SECURITY_DIR);
            temporary = Files.createTempFile(SECURITY_DIR, ".superadmin-", ".tmp");
            try {
                Files.setPosixFilePermissions(temporary, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) { /* Windows inherits profile ACLs. */ }
            Files.writeString(temporary, BCrypt.hashpw(password, BCrypt.gensalt(12)));
            Files.move(temporary, HASH_FILE, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save Super Admin credentials.", e);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    public static boolean verify(String password) {
        if (!isConfigured() || password == null) return false;
        try {
            String hash = Files.readString(HASH_FILE).trim();
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }
}
