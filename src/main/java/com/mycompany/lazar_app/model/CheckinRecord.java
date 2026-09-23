package com.mycompany.lazar_app.model;

import java.time.LocalDateTime;

public class CheckinRecord {

    private int id;
    private int memberId;

    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;

    public CheckinRecord() {
    }

    public CheckinRecord(
            int id,
            int memberId,
            LocalDateTime checkinTime,
            LocalDateTime checkoutTime) {

        this.id = id;
        this.memberId = memberId;
        this.checkinTime = checkinTime;
        this.checkoutTime = checkoutTime;
    }

    public CheckinRecord(
            int memberId,
            LocalDateTime checkinTime) {

        this.memberId = memberId;
        this.checkinTime = checkinTime;
    }

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

    public LocalDateTime getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(LocalDateTime checkinTime) {
        this.checkinTime = checkinTime;
    }

    public LocalDateTime getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(LocalDateTime checkoutTime) {
        this.checkoutTime = checkoutTime;
    }

    public boolean isInside() {
        return checkoutTime == null;
    }
}
