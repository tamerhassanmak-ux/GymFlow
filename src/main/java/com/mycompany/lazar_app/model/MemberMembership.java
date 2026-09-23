package com.mycompany.lazar_app.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MemberMembership {

    private int id;

    private int memberId;
    private int membershipPlanId;

    private LocalDate startDate;
    private LocalDate endDate;

    // =========================================================
    // SESSIONS
    // =========================================================

    /*
     * Number of sessions purchased with this membership.
     *
     * IMPORTANT:
     * This value is copied from MembershipPlan.maxSessions
     * when the membership is created.
     *
     * Example:
     * Plan maxSessions = 12
     * Membership allowedSessions = 12
     *
     * If the plan changes later to 16 sessions,
     * this existing membership remains 12 sessions.
     */
    private int allowedSessions;

    private double price;
    private double discount;
    private double finalPrice;

    private String status;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =========================================================
    // DISPLAY / JOINED DATA
    // =========================================================

    private String memberCode;
    private String memberName;

    private String planCode;
    private String planNameAr;
    private String planNameEn;


    // =========================================================
    // EMPTY CONSTRUCTOR
    // =========================================================

    public MemberMembership() {
    }


    // =========================================================
    // NEW MEMBERSHIP CONSTRUCTOR
    // =========================================================

    public MemberMembership(
            int memberId,
            int membershipPlanId,
            LocalDate startDate,
            LocalDate endDate,
            int allowedSessions,
            double price,
            double discount,
            double finalPrice,
            String status,
            String notes
    ) {

        this.memberId = memberId;

        this.membershipPlanId =
                membershipPlanId;

        this.startDate =
                startDate;

        this.endDate =
                endDate;

        this.allowedSessions =
                allowedSessions;

        this.price =
                price;

        this.discount =
                discount;

        this.finalPrice =
                finalPrice;

        this.status =
                status;

        this.notes =
                notes;
    }


    // =========================================================
    // ID
    // =========================================================

    public int getId() {

        return id;
    }


    public void setId(
            int id
    ) {

        this.id = id;
    }


    // =========================================================
    // MEMBER
    // =========================================================

    public int getMemberId() {

        return memberId;
    }


    public void setMemberId(
            int memberId
    ) {

        this.memberId = memberId;
    }


    // =========================================================
    // MEMBERSHIP PLAN
    // =========================================================

    public int getMembershipPlanId() {

        return membershipPlanId;
    }


    public void setMembershipPlanId(
            int membershipPlanId
    ) {

        this.membershipPlanId =
                membershipPlanId;
    }


    // =========================================================
    // DATES
    // =========================================================

    public LocalDate getStartDate() {

        return startDate;
    }


    public void setStartDate(
            LocalDate startDate
    ) {

        this.startDate =
                startDate;
    }


    public LocalDate getEndDate() {

        return endDate;
    }


    public void setEndDate(
            LocalDate endDate
    ) {

        this.endDate =
                endDate;
    }


    // =========================================================
    // ALLOWED SESSIONS
    // =========================================================

    public int getAllowedSessions() {

        return allowedSessions;
    }


    public void setAllowedSessions(
            int allowedSessions
    ) {

        this.allowedSessions =
                Math.max(
                        allowedSessions,
                        0
                );
    }


    // =========================================================
    // FINANCIAL
    // =========================================================

    public double getPrice() {

        return price;
    }


    public void setPrice(
            double price
    ) {

        this.price =
                price;
    }


    public double getDiscount() {

        return discount;
    }


    public void setDiscount(
            double discount
    ) {

        this.discount =
                discount;
    }


    public double getFinalPrice() {

        return finalPrice;
    }


    public void setFinalPrice(
            double finalPrice
    ) {

        this.finalPrice =
                finalPrice;
    }


    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {

        return status;
    }


    public void setStatus(
            String status
    ) {

        this.status =
                status;
    }


    // =========================================================
    // NOTES
    // =========================================================

    public String getNotes() {

        return notes;
    }


    public void setNotes(
            String notes
    ) {

        this.notes =
                notes;
    }


    // =========================================================
    // TIMESTAMPS
    // =========================================================

    public LocalDateTime getCreatedAt() {

        return createdAt;
    }


    public void setCreatedAt(
            LocalDateTime createdAt
    ) {

        this.createdAt =
                createdAt;
    }


    public LocalDateTime getUpdatedAt() {

        return updatedAt;
    }


    public void setUpdatedAt(
            LocalDateTime updatedAt
    ) {

        this.updatedAt =
                updatedAt;
    }


    // =========================================================
    // MEMBER DISPLAY DATA
    // =========================================================

    public String getMemberCode() {

        return memberCode;
    }


    public void setMemberCode(
            String memberCode
    ) {

        this.memberCode =
                memberCode;
    }


    public String getMemberName() {

        return memberName;
    }


    public void setMemberName(
            String memberName
    ) {

        this.memberName =
                memberName;
    }


    // =========================================================
    // PLAN DISPLAY DATA
    // =========================================================

    public String getPlanCode() {

        return planCode;
    }


    public void setPlanCode(
            String planCode
    ) {

        this.planCode =
                planCode;
    }


    public String getPlanNameAr() {

        return planNameAr;
    }


    public void setPlanNameAr(
            String planNameAr
    ) {

        this.planNameAr =
                planNameAr;
    }


    public String getPlanNameEn() {

        return planNameEn;
    }


    public void setPlanNameEn(
            String planNameEn
    ) {

        this.planNameEn =
                planNameEn;
    }


    // =========================================================
    // MEMBERSHIP DATE VALIDATION
    // =========================================================

    /*
     * Checks:
     *
     * 1. Membership status is Active.
     * 2. Start date exists.
     * 3. End date exists.
     * 4. Membership has already started.
     * 5. Membership has not expired.
     *
     * Session limit is checked separately because
     * the used session count comes from check_ins.
     */
    public boolean isActive() {

        if (!"Active".equalsIgnoreCase(
                status
        )) {

            return false;
        }


        if (startDate == null
                || endDate == null) {

            return false;
        }


        LocalDate today =
                LocalDate.now();


        boolean alreadyStarted =
                !startDate.isAfter(
                        today
                );


        boolean notExpired =
                !endDate.isBefore(
                        today
                );


        return alreadyStarted
                && notExpired;
    }


    // =========================================================
    // STARTED
    // =========================================================

    public boolean hasStarted() {

        if (startDate == null) {

            return false;
        }


        return !startDate.isAfter(
                LocalDate.now()
        );
    }


    // =========================================================
    // EXPIRED
    // =========================================================

    public boolean isExpired() {

        if (endDate == null) {

            return false;
        }


        return endDate.isBefore(
                LocalDate.now()
        );
    }


    // =========================================================
    // FUTURE MEMBERSHIP
    // =========================================================

    public boolean isFuture() {

        if (startDate == null) {

            return false;
        }


        return startDate.isAfter(
                LocalDate.now()
        );
    }


    // =========================================================
    // REMAINING DAYS
    // =========================================================

    public long getRemainingDays() {

        if (endDate == null) {

            return 0;
        }


        long days =
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        endDate
                );


        return Math.max(
                days,
                0
        );
    }


    // =========================================================
    // EXPIRING SOON
    // =========================================================

    public boolean isExpiringSoon() {

        long days =
                getRemainingDays();


        return isActive()
                && days <= 7;
    }


    // =========================================================
    // SESSION HELPERS
    // =========================================================

    /*
     * Returns the remaining sessions.
     *
     * Example:
     *
     * allowedSessions = 12
     * usedSessions    = 8
     *
     * remaining = 4
     */
    public int getRemainingSessions(
            int usedSessions
    ) {

        int used =
                Math.max(
                        usedSessions,
                        0
                );


        return Math.max(
                allowedSessions - used,
                0
        );
    }


    // =========================================================
    // HAS REMAINING SESSIONS
    // =========================================================

    public boolean hasRemainingSessions(
            int usedSessions
    ) {

        /*
         * allowedSessions must be greater than zero.
         *
         * A membership with 0 allowed sessions
         * is NOT valid for gym entry.
         */

        if (allowedSessions <= 0) {

            return false;
        }


        return usedSessions
                < allowedSessions;
    }


    // =========================================================
    // SESSION LIMIT REACHED
    // =========================================================

    public boolean isSessionLimitReached(
            int usedSessions
    ) {

        if (allowedSessions <= 0) {

            return true;
        }


        return usedSessions
                >= allowedSessions;
    }


    // =========================================================
    // MEMBERSHIP CAN BE USED
    // =========================================================

    /*
     * Complete membership validation.
     *
     * BOTH conditions must be valid:
     *
     * 1. Membership period is active.
     * 2. Membership has remaining sessions.
     */
    public boolean canCheckIn(
            int usedSessions
    ) {

        return isActive()
                && hasRemainingSessions(
                        usedSessions
                );
    }


    // =========================================================
    // PLAN DISPLAY NAME
    // =========================================================

    public String getPlanDisplayName(
            boolean arabic
    ) {

        if (arabic) {

            return planNameAr == null
                    ? ""
                    : planNameAr;
        }


        return planNameEn == null
                ? ""
                : planNameEn;
    }


    // =========================================================
    // CALCULATE FINAL PRICE
    // =========================================================

    public void calculateFinalPrice() {

        finalPrice =
                Math.max(
                        price - discount,
                        0
                );
    }


    // =========================================================
    // TO STRING
    // =========================================================

    @Override
    public String toString() {

        String safeMemberName =
                memberName == null
                        ? ""
                        : memberName;


        String safePlanName =
                planNameEn == null
                        ? ""
                        : planNameEn;


        return safeMemberName
                + " - "
                + safePlanName;
    }
}