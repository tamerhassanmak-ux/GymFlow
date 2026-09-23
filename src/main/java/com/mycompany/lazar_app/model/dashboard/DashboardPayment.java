package com.mycompany.lazar_app.model.dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DashboardPayment {

    private final long id;
    private final long memberId;

    private final String memberCode;
    private final String memberName;

    private final BigDecimal amount;

    private final String paymentMethod;
    private final String status;

    private final LocalDateTime paymentDate;


    public DashboardPayment(
            long id,
            long memberId,
            String memberCode,
            String memberName,
            BigDecimal amount,
            String paymentMethod,
            String status,
            LocalDateTime paymentDate
    ) {

        this.id = id;
        this.memberId = memberId;
        this.memberCode = memberCode;
        this.memberName = memberName;

        this.amount =
                amount == null
                        ? BigDecimal.ZERO
                        : amount;

        this.paymentMethod = paymentMethod;
        this.status = status;
        this.paymentDate = paymentDate;
    }


    public long getId() {
        return id;
    }

    public long getMemberId() {
        return memberId;
    }

    public String getMemberCode() {
        return memberCode;
    }

    public String getMemberName() {
        return memberName;
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

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }
}