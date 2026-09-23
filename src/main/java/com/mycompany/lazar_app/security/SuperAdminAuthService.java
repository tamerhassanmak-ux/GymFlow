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

    public static void setPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Super Admin password must contain at least 8 characters.");
        }
        try {
            Files.createDirectories(SECURITY_DIR);
            Files.writeString(HASH_FILE, BCrypt.hashpw(password, BCrypt.gensalt(12)));
        } catch (IOException e) {
            throw new IllegalStateException("Could not save Super Admin credentials.", e);
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
