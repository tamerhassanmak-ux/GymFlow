package com.mycompany.lazar_app.model.report;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ReportSummary {

    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal membershipRevenue = BigDecimal.ZERO;
    private BigDecimal otherRevenue = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;

    private long transactions;
    private long activeMembers;
    private long newMembers;

    public BigDecimal getNetProfit() {

        return safe(totalRevenue)
                .subtract(safe(totalExpenses));
    }

    public BigDecimal getProfitMargin() {

        if (safe(totalRevenue)
                .compareTo(BigDecimal.ZERO) == 0) {

            return BigDecimal.ZERO;
        }

        return getNetProfit()
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        totalRevenue,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = safe(totalRevenue);
    }

    public BigDecimal getMembershipRevenue() {
        return membershipRevenue;
    }

    public void setMembershipRevenue(BigDecimal membershipRevenue) {
        this.membershipRevenue = safe(membershipRevenue);
    }

    public BigDecimal getOtherRevenue() {
        return otherRevenue;
    }

    public void setOtherRevenue(BigDecimal otherRevenue) {
        this.otherRevenue = safe(otherRevenue);
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = safe(totalExpenses);
    }

    public long getTransactions() {
        return transactions;
    }

    public void setTransactions(long transactions) {
        this.transactions = transactions;
    }

    public long getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(long activeMembers) {
        this.activeMembers = activeMembers;
    }

    public long getNewMembers() {
        return newMembers;
    }

    public void setNewMembers(long newMembers) {
        this.newMembers = newMembers;
    }
}