package com.mycompany.lazar_app.service.export;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.report.FinancialActivity;
import com.mycompany.lazar_app.model.report.ReportSummary;
import com.mycompany.lazar_app.model.report.ReportType;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelReportExporter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    // =========================================================
    // EXPORT
    // =========================================================

    public void export(
            File file,
            ReportType reportType,
            LocalDate from,
            LocalDate to,
            ReportSummary summary,
            List<FinancialActivity> activities
    ) throws Exception {

        boolean arabic =
                LanguageManager.isArabic();


        try (
                Workbook workbook =
                        new XSSFWorkbook();

                FileOutputStream output =
                        new FileOutputStream(file)
        ) {

            Sheet sheet =
                    workbook.createSheet(
                            arabic
                                    ? "التقرير"
                                    : "Report"
                    );


            sheet.setRightToLeft(arabic);


            CellStyle titleStyle =
                    createTitleStyle(workbook);

            CellStyle headerStyle =
                    createHeaderStyle(workbook);

            CellStyle moneyStyle =
                    createMoneyStyle(workbook);

            CellStyle normalStyle =
                    createNormalStyle(workbook);


            int rowIndex = 0;


            // =================================================
            // TITLE
            // =================================================

            Row titleRow =
                    sheet.createRow(rowIndex++);

            Cell titleCell =
                    titleRow.createCell(0);

            titleCell.setCellValue(
                    arabic
                            ? "GymFlow - تقرير"
                            : "GymFlow - Report"
            );

            titleCell.setCellStyle(
                    titleStyle
            );


            sheet.addMergedRegion(
                    new org.apache.poi.ss.util.CellRangeAddress(
                            0,
                            0,
                            0,
                            5
                    )
            );


            rowIndex++;


            // =================================================
            // REPORT INFO
            // =================================================

            rowIndex =
                    addInfoRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "نوع التقرير"
                                    : "Report Type",
                            reportType.getDisplayName(),
                            headerStyle,
                            normalStyle
                    );


            rowIndex =
                    addInfoRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "من"
                                    : "From",
                            from.toString(),
                            headerStyle,
                            normalStyle
                    );


            rowIndex =
                    addInfoRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "إلى"
                                    : "To",
                            to.toString(),
                            headerStyle,
                            normalStyle
                    );


            rowIndex += 2;


            // =================================================
            // SUMMARY
            // =================================================

            Row summaryTitle =
                    sheet.createRow(rowIndex++);

            Cell summaryCell =
                    summaryTitle.createCell(0);

            summaryCell.setCellValue(
                    arabic
                            ? "ملخص التقرير"
                            : "Report Summary"
            );

            summaryCell.setCellStyle(
                    titleStyle
            );


            rowIndex =
                    addMoneyRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "إجمالي الإيرادات"
                                    : "Total Revenue",
                            summary.getTotalRevenue(),
                            headerStyle,
                            moneyStyle
                    );


            rowIndex =
                    addMoneyRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "إيرادات العضويات"
                                    : "Membership Revenue",
                            summary.getMembershipRevenue(),
                            headerStyle,
                            moneyStyle
                    );


            rowIndex =
                    addMoneyRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "إيرادات أخرى"
                                    : "Other Revenue",
                            summary.getOtherRevenue(),
                            headerStyle,
                            moneyStyle
                    );


            rowIndex =
                    addMoneyRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "إجمالي المصروفات"
                                    : "Total Expenses",
                            summary.getTotalExpenses(),
                            headerStyle,
                            moneyStyle
                    );


            rowIndex =
                    addMoneyRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "صافي الربح"
                                    : "Net Profit",
                            summary.getNetProfit(),
                            headerStyle,
                            moneyStyle
                    );


            rowIndex =
                    addInfoRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "هامش الربح"
                                    : "Profit Margin",
                            summary.getProfitMargin()
                                    .toPlainString()
                                    + "%",
                            headerStyle,
                            normalStyle
                    );


            rowIndex =
                    addInfoRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "عدد المعاملات"
                                    : "Transactions",
                            String.valueOf(
                                    summary.getTransactions()
                            ),
                            headerStyle,
                            normalStyle
                    );


            rowIndex =
                    addInfoRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "الأعضاء النشطون"
                                    : "Active Members",
                            String.valueOf(
                                    summary.getActiveMembers()
                            ),
                            headerStyle,
                            normalStyle
                    );


            rowIndex =
                    addInfoRow(
                            sheet,
                            rowIndex,
                            arabic
                                    ? "الأعضاء الجدد"
                                    : "New Members",
                            String.valueOf(
                                    summary.getNewMembers()
                            ),
                            headerStyle,
                            normalStyle
                    );


            rowIndex += 2;


            // =================================================
            // ACTIVITY TABLE
            // =================================================

            Row tableHeader =
                    sheet.createRow(rowIndex++);


            String[] headers =
                    arabic
                            ? new String[]{
                                "التاريخ",
                                "الوصف",
                                "النوع",
                                "المبلغ",
                                "طريقة الدفع",
                                "الحالة"
                            }
                            : new String[]{
                                "Date",
                                "Description",
                                "Type",
                                "Amount",
                                "Payment Method",
                                "Status"
                            };


            for (int i = 0;
                    i < headers.length;
                    i++) {

                Cell cell =
                        tableHeader.createCell(i);

                cell.setCellValue(
                        headers[i]
                );

                cell.setCellStyle(
                        headerStyle
                );
            }


            for (FinancialActivity activity
                    : activities) {

                Row row =
                        sheet.createRow(rowIndex++);


                row.createCell(0)
                        .setCellValue(
                                activity.getDate() == null
                                        ? "-"
                                        : activity
                                                .getDate()
                                                .format(
                                                        DATE_FORMAT
                                                )
                        );


                row.createCell(1)
                        .setCellValue(
                                safe(
                                        activity
                                                .getDescription()
                                )
                        );


                row.createCell(2)
                        .setCellValue(
                                translateType(
                                        activity.getType(),
                                        arabic
                                )
                        );


                Cell amountCell =
                        row.createCell(3);

                amountCell.setCellValue(
                        safeDecimal(
                                activity.getAmount()
                        ).doubleValue()
                );

                amountCell.setCellStyle(
                        moneyStyle
                );


                row.createCell(4)
                        .setCellValue(
                                translatePaymentMethod(
                                        activity
                                                .getPaymentMethod(),
                                        arabic
                                )
                        );


                row.createCell(5)
                        .setCellValue(
                                translateStatus(
                                        activity.getStatus(),
                                        arabic
                                )
                        );
            }


            // =================================================
            // COLUMN WIDTHS
            // =================================================

            sheet.setColumnWidth(
                    0,
                    22 * 256
            );

            sheet.setColumnWidth(
                    1,
                    35 * 256
            );

            sheet.setColumnWidth(
                    2,
                    18 * 256
            );

            sheet.setColumnWidth(
                    3,
                    18 * 256
            );

            sheet.setColumnWidth(
                    4,
                    22 * 256
            );

            sheet.setColumnWidth(
                    5,
                    18 * 256
            );


            sheet.createFreezePane(
                    0,
                    Math.max(
                            1,
                            rowIndex
                                    - activities.size()
                                    - 1
                    )
            );


            workbook.write(output);
        }
    }


    // =========================================================
    // STYLES
    // =========================================================

    private CellStyle createTitleStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);
        font.setFontHeightInPoints(
                (short) 16
        );

        style.setFont(font);

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        return style;
    }


    private CellStyle createHeaderStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);

        style.setFont(font);

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setBorderBottom(
                BorderStyle.THIN
        );

        style.setBorderTop(
                BorderStyle.THIN
        );

        style.setBorderLeft(
                BorderStyle.THIN
        );

        style.setBorderRight(
                BorderStyle.THIN
        );

        return style;
    }


    private CellStyle createMoneyStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        DataFormat format =
                workbook.createDataFormat();

        style.setDataFormat(
                format.getFormat(
                        "#,##0.00"
                )
        );

        return style;
    }


    private CellStyle createNormalStyle(
            Workbook workbook
    ) {

        return workbook.createCellStyle();
    }


    // =========================================================
    // ROW HELPERS
    // =========================================================

    private int addInfoRow(
            Sheet sheet,
            int rowIndex,
            String label,
            String value,
            CellStyle labelStyle,
            CellStyle valueStyle
    ) {

        Row row =
                sheet.createRow(rowIndex++);

        Cell labelCell =
                row.createCell(0);

        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);


        Cell valueCell =
                row.createCell(1);

        valueCell.setCellValue(
                value == null
                        ? ""
                        : value
        );

        valueCell.setCellStyle(
                valueStyle
        );


        return rowIndex;
    }


    private int addMoneyRow(
            Sheet sheet,
            int rowIndex,
            String label,
            BigDecimal value,
            CellStyle labelStyle,
            CellStyle moneyStyle
    ) {

        Row row =
                sheet.createRow(rowIndex++);

        Cell labelCell =
                row.createCell(0);

        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);


        Cell valueCell =
                row.createCell(1);

        valueCell.setCellValue(
                safeDecimal(value)
                        .doubleValue()
        );

        valueCell.setCellStyle(
                moneyStyle
        );


        return rowIndex;
    }


    // =========================================================
    // TRANSLATION
    // =========================================================

    private String translateType(
            String value,
            boolean arabic
    ) {

        if (value == null) {
            return "-";
        }

        return switch (
                value.toUpperCase()
        ) {

            case "REVENUE" ->
                    arabic
                            ? "إيراد"
                            : "Revenue";

            case "EXPENSE" ->
                    arabic
                            ? "مصروف"
                            : "Expense";

            default ->
                    value;
        };
    }


    private String translatePaymentMethod(
            String value,
            boolean arabic
    ) {

        if (value == null
                || value.isBlank()) {

            return "-";
        }

        return switch (
                value.toUpperCase()
        ) {

            case "CASH" ->
                    arabic
                            ? "نقدي"
                            : "Cash";

            case "CARD" ->
                    arabic
                            ? "بطاقة"
                            : "Card";

            case "BANK_TRANSFER" ->
                    arabic
                            ? "تحويل بنكي"
                            : "Bank Transfer";

            case "ONLINE" ->
                    arabic
                            ? "دفع إلكتروني"
                            : "Online";

            default ->
                    value;
        };
    }


    private String translateStatus(
            String value,
            boolean arabic
    ) {

        if (value == null
                || value.isBlank()) {

            return "-";
        }

        return switch (
                value.toUpperCase()
        ) {

            case "PAID" ->
                    arabic
                            ? "مدفوع"
                            : "Paid";

            case "PENDING" ->
                    arabic
                            ? "معلق"
                            : "Pending";

            case "CANCELLED" ->
                    arabic
                            ? "ملغي"
                            : "Cancelled";

            default ->
                    value;
        };
    }


    // =========================================================
    // SAFE
    // =========================================================

    private BigDecimal safeDecimal(
            BigDecimal value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : value;
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