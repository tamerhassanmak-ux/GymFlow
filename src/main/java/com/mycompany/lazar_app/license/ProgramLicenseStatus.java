package com.mycompany.lazar_app.license;

public enum ProgramLicenseStatus {
    ACTIVE,
    LIFETIME,
    EXPIRED,
    NOT_STARTED,
    NOT_CONFIGURED,
    CLOCK_ROLLBACK,
    INVALID;

    public boolean isAllowed() {
        return this == ACTIVE || this == LIFETIME;
    }
}
