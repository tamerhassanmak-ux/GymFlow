package com.mycompany.lazar_app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Expense {

    private long id;

    private String expenseCode;

    private String title;

    private String category;

    private BigDecimal amount;

    private LocalDate expenseDate;

    private String paymentMethod;

    private String status;

    private String referenceNumber;

    private String beneficiary;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // =========================================================
    // EMPTY CONSTRUCTOR
    // =========================================================

    public Expense() {
    }

    // =========================================================
    // FULL CONSTRUCTOR
    // =========================================================

    public Expense(
            long id,
            String expenseCode,
            String title,
            String category,
            BigDecimal amount,
            LocalDate expenseDate,
            String paymentMethod,
            String status,
            String referenceNumber,
            String beneficiary,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {

        this.id = id;
        this.expenseCode = expenseCode;
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.referenceNumber = referenceNumber;
        this.beneficiary = beneficiary;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // =========================================================
    // ID
    // =========================================================

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // =========================================================
    // EXPENSE CODE
    // =========================================================

    public String getExpenseCode() {
        return expenseCode;
    }

    public void setExpenseCode(String expenseCode) {
        this.expenseCode = expenseCode;
    }

    // =========================================================
    // TITLE
    // =========================================================

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // =========================================================
    // CATEGORY
    // =========================================================

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // =========================================================
    // AMOUNT
    // =========================================================

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    // =========================================================
    // EXPENSE DATE
    // =========================================================

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(LocalDate expenseDate) {
        this.expenseDate = expenseDate;
    }

    // =========================================================
    // PAYMENT METHOD
    // =========================================================

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // =========================================================
    // REFERENCE NUMBER
    // =========================================================

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    // =========================================================
    // BENEFICIARY
    // =========================================================

    public String getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(String beneficiary) {
        this.beneficiary = beneficiary;
    }

    // =========================================================
    // NOTES
    // =========================================================

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // =========================================================
    // CREATED AT
    // =========================================================

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // =========================================================
    // UPDATED AT
    // =========================================================

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return expenseCode + " - " + title;
    }
}