package com.mycompany.lazar_app.service;

import com.mycompany.lazar_app.model.report.FinancialActivity;
import com.mycompany.lazar_app.model.report.MonthlyFinancialSummary;
import com.mycompany.lazar_app.model.report.ReportSummary;
import com.mycompany.lazar_app.model.report.ReportType;
import com.mycompany.lazar_app.repository.ReportRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportService {

    private final ReportRepository repository;


    public ReportService(
            ReportRepository repository
    ) {

        this.repository =
                repository;
    }


    // =========================================================
    // SUMMARY
    // =========================================================

    public ReportSummary getSummary(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        return getSummary(
                from,
                to,
                ReportType.FINANCIAL_SUMMARY
        );
    }


    public ReportSummary getSummary(
            LocalDate from,
            LocalDate to,
            ReportType reportType
    ) throws SQLException {

        validate(from, to);

        return repository.getSummary(
                from,
                to,
                reportType
        );
    }


    // =========================================================
    // RECENT ACTIVITY
    // =========================================================

    public List<FinancialActivity> getRecentActivity(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        return getRecentActivity(
                from,
                to,
                ReportType.FINANCIAL_SUMMARY
        );
    }


    public List<FinancialActivity> getRecentActivity(
            LocalDate from,
            LocalDate to,
            ReportType reportType
    ) throws SQLException {

        validate(from, to);

        return repository.getFinancialActivity(
                from,
                to,
                reportType,
                10
        );
    }


    // =========================================================
    // ALL ACTIVITY
    // =========================================================

    public List<FinancialActivity> getAllActivity(
            LocalDate from,
            LocalDate to
    ) throws SQLException {

        return getAllActivity(
                from,
                to,
                ReportType.FINANCIAL_SUMMARY
        );
    }


    public List<FinancialActivity> getAllActivity(
            LocalDate from,
            LocalDate to,
            ReportType reportType
    ) throws SQLException {

        validate(from, to);

        return repository.getFinancialActivity(
                from,
                to,
                reportType,
                0
        );
    }


    // =========================================================
    // MONTHLY
    // =========================================================

    public List<MonthlyFinancialSummary>
            getMonthlyFinancialSummary(
                    LocalDate from,
                    LocalDate to
            ) throws SQLException {

        return getMonthlyFinancialSummary(
                from,
                to,
                ReportType.FINANCIAL_SUMMARY
        );
    }


    public List<MonthlyFinancialSummary>
            getMonthlyFinancialSummary(
                    LocalDate from,
                    LocalDate to,
                    ReportType reportType
            ) throws SQLException {

        validate(from, to);

        return repository
                .getMonthlyFinancialSummary(
                        from,
                        to,
                        reportType
                );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validate(
            LocalDate from,
            LocalDate to
    ) {

        if (from == null
                || to == null) {

            throw new IllegalArgumentException(
                    "Report date range is required."
            );
        }


        if (to.isBefore(from)) {

            throw new IllegalArgumentException(
                    "End date cannot be before start date."
            );
        }
    }
}