package com.mycompany.lazar_app.model.dashboard;

import java.time.LocalDate;

public class DashboardMember {

    private final long id;
    private final String memberCode;
    private final String name;
    private final String membership;
    private final String status;
    private final LocalDate startDate;


    public DashboardMember(
            long id,
            String memberCode,
            String name,
            String membership,
            String status,
            LocalDate startDate
    ) {

        this.id = id;
        this.memberCode = memberCode;
        this.name = name;
        this.membership = membership;
        this.status = status;
        this.startDate = startDate;
    }


    public long getId() {
        return id;
    }

    public String getMemberCode() {
        return memberCode;
    }

    public String getName() {
        return name;
    }

    public String getMembership() {
        return membership;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }
}