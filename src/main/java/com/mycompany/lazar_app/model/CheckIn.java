package com.mycompany.lazar_app.model;

import java.time.LocalDateTime;

public class CheckIn {

    private int id;

    private int memberId;
    private Integer membershipId;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    private String status;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // Joined / display fields

    private String memberCode;
    private String memberName;
    private String phone;

    private String membershipNameAr;
    private String membershipNameEn;


    public CheckIn() {
    }


    public CheckIn(
            int id,
            int memberId,
            Integer membershipId,
            LocalDateTime checkInTime,
            LocalDateTime checkOutTime,
            String status,
            String notes
    ) {

        this.id = id;
        this.memberId = memberId;
        this.membershipId = membershipId;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = safe(status);
        this.notes = safe(notes);
    }


    // =========================================================
    // HELPERS
    // =========================================================

    public boolean isInside() {

        return "IN".equalsIgnoreCase(status)
                && checkOutTime == null;
    }


    public boolean isCheckedOut() {

        return "OUT".equalsIgnoreCase(status)
                || checkOutTime != null;
    }


    public String getMembershipDisplayName(boolean arabic) {

        if (arabic) {

            if (membershipNameAr != null
                    && !membershipNameAr.isBlank()) {

                return membershipNameAr;
            }
        }

        if (membershipNameEn != null
                && !membershipNameEn.isBlank()) {

            return membershipNameEn;
        }

        return "";
    }


    private String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }


    public Integer getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
    }


    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }


    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = safe(status);
    }


    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = safe(notes);
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


    public String getMemberCode() {
        return memberCode;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = safe(memberCode);
    }


    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = safe(memberName);
    }


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = safe(phone);
    }


    public String getMembershipNameAr() {
        return membershipNameAr;
    }

    public void setMembershipNameAr(String membershipNameAr) {
        this.membershipNameAr = safe(membershipNameAr);
    }


    public String getMembershipNameEn() {
        return membershipNameEn;
    }

    public void setMembershipNameEn(String membershipNameEn) {
        this.membershipNameEn = safe(membershipNameEn);
    }


    @Override
    public String toString() {

        return memberName
                + " - "
                + checkInTime;
    }
}