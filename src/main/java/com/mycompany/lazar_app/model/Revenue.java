package com.mycompany.lazar_app.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Revenue {

    private long id;
    private String revenueCode;
    private String name;
    private String revenueType;
    private BigDecimal amount;
    private LocalDate revenueDate;
    private String attachmentPath;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Revenue() {
        this.amount = BigDecimal.ZERO;
    }

    public Revenue(
            long id,
            String revenueCode,
            String name,
            String revenueType,
            BigDecimal amount,
            LocalDate revenueDate,
            String attachmentPath,
            String notes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.revenueCode = revenueCode;
        this.name = name;
        this.revenueType = revenueType;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.revenueDate = revenueDate;
        this.attachmentPath = attachmentPath;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRevenueCode() {
        return revenueCode;
    }

    public void setRevenueCode(String revenueCode) {
        this.revenueCode = revenueCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRevenueType() {
        return revenueType;
    }

    public void setRevenueType(String revenueType) {
        this.revenueType = revenueType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDate getRevenueDate() {
        return revenueDate;
    }

    public void setRevenueDate(LocalDate revenueDate) {
        this.revenueDate = revenueDate;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
