package com.mycompany.lazar_app.model.dashboard;

import java.math.BigDecimal;
import java.time.YearMonth;

public class DashboardMonthlyData {

    private final YearMonth month;

    private BigDecimal revenue =
            BigDecimal.ZERO;

    private BigDecimal expenses =
            BigDecimal.ZERO;

    private long newMembers;


    public DashboardMonthlyData(
            YearMonth month
    ) {

        this.month = month;
    }


    public DashboardMonthlyData(
            YearMonth month,
            BigDecimal revenue,
            BigDecimal expenses,
            long newMembers
    ) {

        this.month = month;

        this.revenue =
                revenue == null
                        ? BigDecimal.ZERO
                        : revenue;

        this.expenses =
                expenses == null
                        ? BigDecimal.ZERO
                        : expenses;

        this.newMembers = newMembers;
    }


    public YearMonth getMonth() {
        return month;
    }


    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {

        this.revenue =
                revenue == null
                        ? BigDecimal.ZERO
                        : revenue;
    }


    public BigDecimal getExpenses() {
        return expenses;
    }

    public void setExpenses(BigDecimal expenses) {

        this.expenses =
                expenses == null
                        ? BigDecimal.ZERO
                        : expenses;
    }


    public long getNewMembers() {
        return newMembers;
    }

    public void setNewMembers(long newMembers) {
        this.newMembers = newMembers;
    }
}