package com.mycompany.lazar_app.model.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FinancialActivity {

    private final LocalDateTime date;
    private final String description;
    private final String type;
    private final BigDecimal amount;
    private final String paymentMethod;
    private final String status;
    private final boolean membershipRevenue;

    public FinancialActivity(
            LocalDateTime date,
            String description,
            String type,
            BigDecimal amount,
            String paymentMethod,
            String status,
            boolean membershipRevenue) {

        this.date = date;
        this.description = description;
        this.type = type;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.membershipRevenue = membershipRevenue;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public boolean isMembershipRevenue() {
        return membershipRevenue;
    }

    public boolean isRevenue() {
        return "REVENUE".equalsIgnoreCase(type);
    }

    public boolean isExpense() {
        return "EXPENSE".equalsIgnoreCase(type);
    }
}