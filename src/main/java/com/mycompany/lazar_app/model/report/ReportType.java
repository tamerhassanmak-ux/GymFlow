package com.mycompany.lazar_app.model.report;

import com.mycompany.lazar_app.LanguageManager;

public enum ReportType {

    FINANCIAL_SUMMARY,
    REVENUE,
    EXPENSES,
    MEMBERSHIPS;

    public String getDisplayName() {

        boolean arabic = LanguageManager.isArabic();

        return switch (this) {

            case FINANCIAL_SUMMARY ->
                    arabic ? "الملخص المالي" : "Financial Summary";

            case REVENUE ->
                    arabic ? "الإيرادات" : "Revenue";

            case EXPENSES ->
                    arabic ? "المصروفات" : "Expenses";

            case MEMBERSHIPS ->
                    arabic ? "العضويات" : "Memberships";
        };
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}