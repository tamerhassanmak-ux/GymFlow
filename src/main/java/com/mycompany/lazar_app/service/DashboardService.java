package com.mycompany.lazar_app.service;

import com.mycompany.lazar_app.model.dashboard.DashboardExpiration;
import com.mycompany.lazar_app.model.dashboard.DashboardMember;
import com.mycompany.lazar_app.model.dashboard.DashboardMembershipData;
import com.mycompany.lazar_app.model.dashboard.DashboardMonthlyData;
import com.mycompany.lazar_app.model.dashboard.DashboardPayment;
import com.mycompany.lazar_app.model.dashboard.DashboardSummary;
import com.mycompany.lazar_app.repository.DashboardRepository;

import java.util.List;


public class DashboardService {

    private static final int DASHBOARD_MONTHS = 6;

    private static final int RECENT_MEMBERS_LIMIT = 3;

    private static final int UPCOMING_EXPIRATIONS_DAYS = 7;

    private static final int UPCOMING_EXPIRATIONS_LIMIT = 3;

    private static final int RECENT_PAYMENTS_LIMIT = 3;


    private final DashboardRepository repository;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DashboardService() {

        this.repository =
                new DashboardRepository();
    }


    // =========================================================
    // SUMMARY
    // =========================================================

    public DashboardSummary getSummary() {

        return repository.getSummary();
    }


    // =========================================================
    // MONTHLY DATA
    // =========================================================

    public List<DashboardMonthlyData>
            getMonthlyData() {

        return repository.getMonthlyData(
                DASHBOARD_MONTHS
        );
    }


    // =========================================================
    // MEMBERSHIP DISTRIBUTION
    // =========================================================

    public List<DashboardMembershipData>
            getMembershipDistribution() {

        return repository
                .getMembershipDistribution();
    }


    // =========================================================
    // RECENT MEMBERS
    // =========================================================

    public List<DashboardMember>
            getRecentMembers() {

        return repository.getRecentMembers(
                RECENT_MEMBERS_LIMIT
        );
    }


    // =========================================================
    // UPCOMING EXPIRATIONS
    // =========================================================

    public List<DashboardExpiration>
            getUpcomingExpirations() {

        return repository
                .getUpcomingExpirations(
                        UPCOMING_EXPIRATIONS_DAYS,
                        UPCOMING_EXPIRATIONS_LIMIT
                );
    }


    // =========================================================
    // RECENT PAYMENTS
    // =========================================================

    public List<DashboardPayment>
            getRecentPayments() {

        return repository.getRecentPayments(
                RECENT_PAYMENTS_LIMIT
        );
    }
}