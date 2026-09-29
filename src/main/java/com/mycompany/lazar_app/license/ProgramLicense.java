package com.mycompany.lazar_app.license;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Local software-license state for GymFlow.
 *
 * PERIOD   -> application is available from startDate through endDate (inclusive).
 * LIFETIME -> no expiry date is required.
 */
public final class ProgramLicense {

    public enum Mode {
        PERIOD,
        LIFETIME
    }

    private boolean configured;
    private boolean integrityValid = true;
    private Mode mode = Mode.PERIOD;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate lastSeenDate;
    private LocalDateTime updatedAt;

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public boolean isIntegrityValid() {
        return integrityValid;
    }

    public void setIntegrityValid(boolean integrityValid) {
        this.integrityValid = integrityValid;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.PERIOD : mode;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getLastSeenDate() {
        return lastSeenDate;
    }

    public void setLastSeenDate(LocalDate lastSeenDate) {
        this.lastSeenDate = lastSeenDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isLifetime() {
        return configured && mode == Mode.LIFETIME;
    }
}
