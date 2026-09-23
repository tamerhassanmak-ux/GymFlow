package com.mycompany.lazar_app.model.report;

import java.math.BigDecimal;
import java.time.YearMonth;

public class MonthlyFinancialSummary {

    private final YearMonth month;
    private final BigDecimal revenue;
    private final BigDecimal expenses;

    public MonthlyFinancialSummary(
            YearMonth month,
            BigDecimal revenue,
            BigDecimal expenses) {

        this.month = month;
        this.revenue = revenue;
        this.expenses = expenses;
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }
}