package com.mycompany.lazar_app.model;

import java.time.LocalDateTime;

public class MembershipPlan {

    private int id;

    private String code;

    private String nameAr;

    private String nameEn;

    private int durationMonths;

    /*
     * Maximum number of training sessions allowed
     * during the membership period.
     *
     * Example:
     * Monthly plan = 12 sessions.
     */
    private int maxSessions;

    private double price;

    private String status;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;


    // =========================================================
    // EMPTY CONSTRUCTOR
    // =========================================================

    public MembershipPlan() {
    }


    // =========================================================
    // CONSTRUCTOR FOR NEW PLAN
    // =========================================================

    public MembershipPlan(
            String code,
            String nameAr,
            String nameEn,
            int durationMonths,
            int maxSessions,
            double price,
            String status,
            String description
    ) {

        this.code = code;

        this.nameAr = nameAr;

        this.nameEn = nameEn;

        this.durationMonths = durationMonths;

        this.maxSessions = maxSessions;

        this.price = price;

        this.status = status;

        this.description = description;
    }


    // =========================================================
    // FULL CONSTRUCTOR
    // =========================================================

    public MembershipPlan(
            int id,
            String code,
            String nameAr,
            String nameEn,
            int durationMonths,
            int maxSessions,
            double price,
            String status,
            String description,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {

        this.id = id;

        this.code = code;

        this.nameAr = nameAr;

        this.nameEn = nameEn;

        this.durationMonths = durationMonths;

        this.maxSessions = maxSessions;

        this.price = price;

        this.status = status;

        this.description = description;

        this.createdAt = createdAt;

        this.updatedAt = updatedAt;
    }


    // =========================================================
    // GETTERS & SETTERS
    // =========================================================

    public int getId() {

        return id;
    }


    public void setId(
            int id
    ) {

        this.id = id;
    }


    public String getCode() {

        return code;
    }


    public void setCode(
            String code
    ) {

        this.code = code;
    }


    public String getNameAr() {

        return nameAr;
    }


    public void setNameAr(
            String nameAr
    ) {

        this.nameAr = nameAr;
    }


    public String getNameEn() {

        return nameEn;
    }


    public void setNameEn(
            String nameEn
    ) {

        this.nameEn = nameEn;
    }


    public int getDurationMonths() {

        return durationMonths;
    }


    public void setDurationMonths(
            int durationMonths
    ) {

        this.durationMonths = durationMonths;
    }


    // =========================================================
    // MAX SESSIONS
    // =========================================================

    public int getMaxSessions() {

        return maxSessions;
    }


    public void setMaxSessions(
            int maxSessions
    ) {

        this.maxSessions = maxSessions;
    }


    public double getPrice() {

        return price;
    }


    public void setPrice(
            double price
    ) {

        this.price = price;
    }


    public String getStatus() {

        return status;
    }


    public void setStatus(
            String status
    ) {

        this.status = status;
    }


    public String getDescription() {

        return description;
    }


    public void setDescription(
            String description
    ) {

        this.description = description;
    }


    public LocalDateTime getCreatedAt() {

        return createdAt;
    }


    public void setCreatedAt(
            LocalDateTime createdAt
    ) {

        this.createdAt = createdAt;
    }


    public LocalDateTime getUpdatedAt() {

        return updatedAt;
    }


    public void setUpdatedAt(
            LocalDateTime updatedAt
    ) {

        this.updatedAt = updatedAt;
    }


    // =========================================================
    // HELPERS
    // =========================================================

    public boolean isActive() {

        return "Active".equalsIgnoreCase(
                status
        );
    }


    // =========================================================
    // DISPLAY NAME
    // =========================================================

    public String getDisplayName(
            boolean arabic
    ) {

        return arabic
                ? nameAr
                : nameEn;
    }


    // =========================================================
    // HAS SESSION LIMIT
    // =========================================================

    public boolean hasSessionLimit() {

        return maxSessions > 0;
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    public boolean hasValidDuration() {

        return durationMonths > 0;
    }


    public boolean hasValidSessions() {

        return maxSessions > 0;
    }


    // =========================================================
    // DISPLAY PLAN INFO
    // =========================================================

    public String getDisplayInfo(
            boolean arabic
    ) {

        if (arabic) {

            return nameAr
                    + " - "
                    + durationMonths
                    + " شهر"
                    + " - "
                    + maxSessions
                    + " تمرين";
        }


        return nameEn
                + " - "
                + durationMonths
                + " Month(s)"
                + " - "
                + maxSessions
                + " Sessions";
    }


    // =========================================================
    // TO STRING
    // =========================================================

    @Override
    public String toString() {

        return nameEn;
    }
}