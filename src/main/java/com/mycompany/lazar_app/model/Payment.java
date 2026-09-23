package com.mycompany.lazar_app.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {

    private long id;
    private long memberId;
    private Long membershipId;

    private BigDecimal amount;

    private String paymentMethod;
    private String status;

    private LocalDateTime paymentDate;

    private String referenceNumber;
    private String notes;

    public Payment() {
    }

    public Payment(
            long id,
            long memberId,
            Long membershipId,
            BigDecimal amount,
            String paymentMethod,
            String status,
            LocalDateTime paymentDate,
            String referenceNumber,
            String notes) {

        this.id = id;
        this.memberId = memberId;
        this.membershipId = membershipId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.paymentDate = paymentDate;
        this.referenceNumber = referenceNumber;
        this.notes = notes;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Long membershipId) {
        this.membershipId = membershipId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}