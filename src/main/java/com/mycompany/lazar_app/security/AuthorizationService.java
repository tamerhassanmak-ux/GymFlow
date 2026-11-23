package com.mycompany.lazar_app.security;

import java.time.Duration;

/** Local desktop roles. Financial and administrative writes require recent admin approval. */
public final class AuthorizationService {
    public enum Role { OPERATOR, SUPER_ADMIN }
    private static volatile long approvedUntil;
    private static final long APPROVAL_NANOS = Duration.ofMinutes(15).toNanos();
    private AuthorizationService() {}
    public static boolean authenticate(String password) {
        // A failed authentication never extends or creates authorization.
        if (!SuperAdminAuthService.verify(password)) return false;
        approvedUntil = System.nanoTime() + APPROVAL_NANOS;
        return true;
    }
    public static Role currentRole() {
        return approvedUntil != 0 && System.nanoTime() - approvedUntil < 0
                ? Role.SUPER_ADMIN : Role.OPERATOR;
    }
    public static void requireAdmin() {
        if (currentRole() != Role.SUPER_ADMIN) {
            throw new SecurityException("Super Admin approval is required for this operation.");
        }
    }
    public static void logout() { approvedUntil = 0; }
}
