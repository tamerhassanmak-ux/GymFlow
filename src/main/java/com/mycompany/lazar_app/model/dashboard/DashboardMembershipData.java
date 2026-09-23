package com.mycompany.lazar_app.model.dashboard;

public class DashboardMembershipData {

    private final long planId;

    private final String nameAr;
    private final String nameEn;

    private final long membersCount;


    public DashboardMembershipData(
            long planId,
            String nameAr,
            String nameEn,
            long membersCount
    ) {

        this.planId = planId;
        this.nameAr = nameAr;
        this.nameEn = nameEn;
        this.membersCount = membersCount;
    }


    public long getPlanId() {
        return planId;
    }

    public String getNameAr() {
        return nameAr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public long getMembersCount() {
        return membersCount;
    }


    public String getDisplayName(
            boolean arabic
    ) {

        if (arabic) {

            if (nameAr != null
                    && !nameAr.isBlank()) {

                return nameAr;
            }

            return safe(nameEn);
        }


        if (nameEn != null
                && !nameEn.isBlank()) {

            return nameEn;
        }


        return safe(nameAr);
    }


    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                        ? "-"
                        : value.trim();
    }
}