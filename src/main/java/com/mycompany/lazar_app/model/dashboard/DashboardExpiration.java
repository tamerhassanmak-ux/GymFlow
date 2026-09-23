package com.mycompany.lazar_app.model.dashboard;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DashboardExpiration {

    private final long membershipId;
    private final long memberId;

    private final String memberCode;
    private final String memberName;

    private final String membershipNameAr;
    private final String membershipNameEn;

    private final LocalDate endDate;


    public DashboardExpiration(
            long membershipId,
            long memberId,
            String memberCode,
            String memberName,
            String membershipNameAr,
            String membershipNameEn,
            LocalDate endDate
    ) {

        this.membershipId = membershipId;
        this.memberId = memberId;
        this.memberCode = memberCode;
        this.memberName = memberName;
        this.membershipNameAr = membershipNameAr;
        this.membershipNameEn = membershipNameEn;
        this.endDate = endDate;
    }


    public long getMembershipId() {
        return membershipId;
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

    public String getMembershipNameAr() {
        return membershipNameAr;
    }

    public String getMembershipNameEn() {
        return membershipNameEn;
    }

    public LocalDate getEndDate() {
        return endDate;
    }


    // =========================================================
    // DAYS REMAINING
    // =========================================================

    public long getDaysRemaining() {

        if (endDate == null) {
            return 0;
        }

        return ChronoUnit.DAYS.between(
                LocalDate.now(),
                endDate
        );
    }
}