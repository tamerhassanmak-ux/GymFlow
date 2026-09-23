package com.mycompany.lazar_app.model.dashboard;

import java.math.BigDecimal;

public class DashboardSummary {

    private long activeMembers;
    private long todayCheckIns;
    private long expiringMemberships;

    private BigDecimal monthlyRevenue = BigDecimal.ZERO;
    private BigDecimal previousMonthRevenue = BigDecimal.ZERO;

    private long newMembersThisMonth;
    private long newMembersPreviousMonth;


    // =========================================================
    // ACTIVE MEMBERS
    // =========================================================

    public long getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(long activeMembers) {
        this.activeMembers = activeMembers;
    }


    // =========================================================
    // TODAY CHECK-INS
    // =========================================================

    public long getTodayCheckIns() {
        return todayCheckIns;
    }

    public void setTodayCheckIns(long todayCheckIns) {
        this.todayCheckIns = todayCheckIns;
    }


    // =========================================================
    // EXPIRING MEMBERSHIPS
    // =========================================================

    public long getExpiringMemberships() {
        return expiringMemberships;
    }

    public void setExpiringMemberships(long expiringMemberships) {
        this.expiringMemberships = expiringMemberships;
    }


    // =========================================================
    // MONTHLY REVENUE
    // =========================================================

    public BigDecimal getMonthlyRevenue() {
        return safe(monthlyRevenue);
    }

    public void setMonthlyRevenue(BigDecimal monthlyRevenue) {
        this.monthlyRevenue = safe(monthlyRevenue);
    }


    // =========================================================
    // PREVIOUS MONTH REVENUE
    // =========================================================

    public BigDecimal getPreviousMonthRevenue() {
        return safe(previousMonthRevenue);
    }

    public void setPreviousMonthRevenue(BigDecimal previousMonthRevenue) {
        this.previousMonthRevenue = safe(previousMonthRevenue);
    }


    // =========================================================
    // NEW MEMBERS
    // =========================================================

    public long getNewMembersThisMonth() {
        return newMembersThisMonth;
    }

    public void setNewMembersThisMonth(long newMembersThisMonth) {
        this.newMembersThisMonth = newMembersThisMonth;
    }

    public long getNewMembersPreviousMonth() {
        return newMembersPreviousMonth;
    }

    public void setNewMembersPreviousMonth(long newMembersPreviousMonth) {
        this.newMembersPreviousMonth = newMembersPreviousMonth;
    }


    // =========================================================
    // REVENUE CHANGE %
    // =========================================================

    public double getRevenueChangePercentage() {

        BigDecimal current =
                getMonthlyRevenue();

        BigDecimal previous =
                getPreviousMonthRevenue();


        if (previous.compareTo(BigDecimal.ZERO) == 0) {

            return current.compareTo(BigDecimal.ZERO) > 0
                    ? 100.0
                    : 0.0;
        }


        return current
                .subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        previous,
                        2,
                        java.math.RoundingMode.HALF_UP
                )
                .doubleValue();
    }


    // =========================================================
    // MEMBER CHANGE %
    // =========================================================

    public double getMemberChangePercentage() {

        if (newMembersPreviousMonth == 0) {

            return newMembersThisMonth > 0
                    ? 100.0
                    : 0.0;
        }


        return (
                (
                        newMembersThisMonth
                        - newMembersPreviousMonth
                )
                * 100.0
                / newMembersPreviousMonth
        );
    }


    // =========================================================
    // SAFE
    // =========================================================

    private BigDecimal safe(BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }
}