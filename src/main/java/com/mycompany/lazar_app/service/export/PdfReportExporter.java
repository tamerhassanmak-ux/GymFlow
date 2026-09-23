package com.mycompany.lazar_app.service.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.model.report.FinancialActivity;
import com.mycompany.lazar_app.model.report.ReportSummary;
import com.mycompany.lazar_app.model.report.ReportType;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PdfReportExporter {

    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color NAVY_2 = new Color(30, 41, 59);
    private static final Color ORANGE = new Color(249, 115, 22);
    private static final Color ORANGE_SOFT = new Color(255, 247, 237);
    private static final Color SLATE = new Color(71, 85, 105);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color SURFACE = new Color(248, 250, 252);
    private static final Color WHITE = Color.WHITE;
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color RED = new Color(220, 38, 38);
    private static final Color AMBER = new Color(217, 119, 6);

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void export(
            File file,
            ReportType reportType,
            LocalDate from,
            LocalDate to,
            ReportSummary summary,
            List<FinancialActivity> activities
    ) throws Exception {

        boolean arabic = LanguageManager.isArabic();
        ReportSummary safeSummary = summary;
        List<FinancialActivity> safeActivities =
                activities == null ? List.of() : activities;

        BaseFont regularBase = createUnicodeFont();
        BaseFont boldBase = regularBase;

        Font brandFont = new Font(boldBase, 19, Font.BOLD, NAVY);
        Font titleFont = new Font(boldBase, 17, Font.BOLD, NAVY);
        Font subtitleFont = new Font(regularBase, 8.5f, Font.NORMAL, MUTED);
        Font sectionFont = new Font(boldBase, 11, Font.BOLD, NAVY);
        Font labelFont = new Font(boldBase, 8, Font.BOLD, MUTED);
        Font valueFont = new Font(boldBase, 12, Font.BOLD, NAVY);
        Font normalFont = new Font(regularBase, 8.2f, Font.NORMAL, SLATE);
        Font tableHeaderFont = new Font(boldBase, 8.2f, Font.BOLD, WHITE);
        Font smallFont = new Font(regularBase, 7.2f, Font.NORMAL, MUTED);
        Font whiteSmallFont = new Font(regularBase, 7.2f, Font.NORMAL, WHITE);

        Document document = new Document(
                PageSize.A4.rotate(),
                30, 30, 34, 42
        );

        try (FileOutputStream output = new FileOutputStream(file)) {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new GymFlowPageEvent(
                    regularBase,
                    arabic
            ));

            document.open();

            addBrandHeader(
                    document, brandFont, subtitleFont, arabic
            );

            addReportHeading(
                    document,
                    reportType,
                    from,
                    to,
                    titleFont,
                    subtitleFont,
                    arabic
            );

            addKpis(
                    document,
                    safeSummary,
                    labelFont,
                    valueFont,
                    smallFont,
                    arabic
            );

            addSectionTitle(
                    document,
                    arabic ? "الحركة المالية" : "Financial Activity",
                    sectionFont,
                    arabic
            );

            addActivityTable(
                    document,
                    safeActivities,
                    tableHeaderFont,
                    normalFont,
                    smallFont,
                    arabic
            );

            addReportMeta(
                    document,
                    safeSummary,
                    labelFont,
                    normalFont,
                    arabic
            );

            document.close();
        }
    }

    // =========================================================
    // BRAND HEADER
    // =========================================================

    private void addBrandHeader(
            Document document,
            Font brandFont,
            Font subtitleFont,
            boolean arabic
    ) throws Exception {

        PdfPTable header = new PdfPTable(new float[]{2.0f, 5.2f});
        header.setWidthPercentage(100);
        header.setSpacingAfter(8f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(0);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Image logo = loadLogo();
        if (logo != null) {
            logo.scaleToFit(118f, 64f);
            logoCell.addElement(logo);
        } else {
            Paragraph fallback = new Paragraph("GymFlow", brandFont);
            fallback.setAlignment(Element.ALIGN_LEFT);
            logoCell.addElement(fallback);
        }

        PdfPCell identity = new PdfPCell();
        identity.setBorder(Rectangle.NO_BORDER);
        identity.setPadding(4f);
        identity.setVerticalAlignment(Element.ALIGN_MIDDLE);
        identity.setRunDirection(arabic
                ? PdfWriter.RUN_DIRECTION_RTL
                : PdfWriter.RUN_DIRECTION_LTR);

        Paragraph product = new Paragraph(
                arabic ? "نظام إدارة الصالات الرياضية" : "GYM MANAGEMENT SYSTEM",
                subtitleFont
        );
        product.setAlignment(arabic
                ? Element.ALIGN_RIGHT
                : Element.ALIGN_LEFT);

        Paragraph company = new Paragraph(
                arabic ? "حلول برمجية بواسطة MAKSOFT" : "POWERED BY MAKSOFT • SOFTWARE SOLUTIONS",
                subtitleFont
        );
        company.setAlignment(arabic
                ? Element.ALIGN_RIGHT
                : Element.ALIGN_LEFT);
        company.setSpacingBefore(3f);

        identity.addElement(product);
        identity.addElement(company);

        if (arabic) {
            header.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }

        header.addCell(logoCell);
        header.addCell(identity);
        document.add(header);

        PdfPTable accent = new PdfPTable(new float[]{1.2f, 8.8f});
        accent.setWidthPercentage(100);

        PdfPCell orange = new PdfPCell();
        orange.setFixedHeight(4f);
        orange.setBackgroundColor(ORANGE);
        orange.setBorder(Rectangle.NO_BORDER);

        PdfPCell navy = new PdfPCell();
        navy.setFixedHeight(4f);
        navy.setBackgroundColor(NAVY);
        navy.setBorder(Rectangle.NO_BORDER);

        accent.addCell(orange);
        accent.addCell(navy);
        accent.setSpacingAfter(14f);
        document.add(accent);
    }

    // =========================================================
    // REPORT HEADING
    // =========================================================

    private void addReportHeading(
            Document document,
            ReportType reportType,
            LocalDate from,
            LocalDate to,
            Font titleFont,
            Font subtitleFont,
            boolean arabic
    ) throws Exception {

        PdfPTable box = new PdfPTable(new float[]{4.6f, 2.4f});
        box.setWidthPercentage(100);
        box.setSpacingAfter(13f);
        if (arabic) {
            box.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }

        PdfPCell title = new PdfPCell();
        title.setBackgroundColor(SURFACE);
        title.setBorderColor(BORDER);
        title.setPadding(13f);
        title.setRunDirection(arabic
                ? PdfWriter.RUN_DIRECTION_RTL
                : PdfWriter.RUN_DIRECTION_LTR);

        Paragraph p1 = new Paragraph(
                reportType == null
                        ? (arabic ? "تقرير GymFlow" : "GymFlow Report")
                        : reportType.getDisplayName(),
                titleFont
        );
        p1.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);

        Paragraph p2 = new Paragraph(
                arabic
                        ? "تقرير إداري ومالي صادر من نظام GymFlow"
                        : "Management & financial report generated by GymFlow",
                subtitleFont
        );
        p2.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        p2.setSpacingBefore(4f);

        title.addElement(p1);
        title.addElement(p2);

        PdfPCell period = new PdfPCell();
        period.setBackgroundColor(ORANGE_SOFT);
        period.setBorderColor(new Color(254, 215, 170));
        period.setPadding(13f);
        period.setRunDirection(arabic
                ? PdfWriter.RUN_DIRECTION_RTL
                : PdfWriter.RUN_DIRECTION_LTR);

        Paragraph periodLabel = new Paragraph(
                arabic ? "الفترة الزمنية" : "REPORTING PERIOD",
                new Font(subtitleFont.getBaseFont(), 7.5f, Font.BOLD, ORANGE)
        );
        periodLabel.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);

        Paragraph periodValue = new Paragraph(
                formatPeriod(from, to, arabic),
                new Font(titleFont.getBaseFont(), 10f, Font.BOLD, NAVY)
        );
        periodValue.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        periodValue.setSpacingBefore(4f);

        period.addElement(periodLabel);
        period.addElement(periodValue);

        box.addCell(title);
        box.addCell(period);
        document.add(box);
    }

    // =========================================================
    // KPI CARDS
    // =========================================================

    private void addKpis(
            Document document,
            ReportSummary summary,
            Font labelFont,
            Font valueFont,
            Font smallFont,
            boolean arabic
    ) throws Exception {

        PdfPTable cards = new PdfPTable(4);
        cards.setWidthPercentage(100);
        cards.setSpacingAfter(15f);

        if (arabic) {
            cards.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }

        BigDecimal revenue = summary == null ? BigDecimal.ZERO : safe(summary.getTotalRevenue());
        BigDecimal expenses = summary == null ? BigDecimal.ZERO : safe(summary.getTotalExpenses());
        BigDecimal profit = summary == null ? BigDecimal.ZERO : safe(summary.getNetProfit());
        long transactions = summary == null ? 0 : summary.getTransactions();

        cards.addCell(kpi(
                arabic ? "إجمالي الإيرادات" : "TOTAL REVENUE",
                money(revenue, arabic),
                arabic ? "إيرادات الفترة" : "Period revenue",
                GREEN, labelFont, valueFont, smallFont, arabic
        ));

        cards.addCell(kpi(
                arabic ? "إجمالي المصروفات" : "TOTAL EXPENSES",
                money(expenses, arabic),
                arabic ? "مصروفات الفترة" : "Period expenses",
                RED, labelFont, valueFont, smallFont, arabic
        ));

        cards.addCell(kpi(
                arabic ? "صافي الربح" : "NET PROFIT",
                money(profit, arabic),
                arabic ? "الإيرادات ناقص المصروفات" : "Revenue minus expenses",
                ORANGE, labelFont, valueFont, smallFont, arabic
        ));

        cards.addCell(kpi(
                arabic ? "المعاملات" : "TRANSACTIONS",
                String.valueOf(transactions),
                arabic ? "إجمالي العمليات" : "Total activity",
                NAVY, labelFont, valueFont, smallFont, arabic
        ));

        document.add(cards);
    }

    private PdfPCell kpi(
            String label,
            String value,
            String note,
            Color accent,
            Font labelFont,
            Font valueFont,
            Font smallFont,
            boolean arabic
    ) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(11f);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.8f);
        cell.setBackgroundColor(WHITE);
        cell.setRunDirection(arabic
                ? PdfWriter.RUN_DIRECTION_RTL
                : PdfWriter.RUN_DIRECTION_LTR);

        Paragraph l = new Paragraph(label, labelFont);
        l.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);

        Font accentedValue = new Font(
                valueFont.getBaseFont(),
                valueFont.getSize(),
                Font.BOLD,
                accent
        );
        Paragraph v = new Paragraph(value, accentedValue);
        v.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        v.setSpacingBefore(5f);

        Paragraph n = new Paragraph(note, smallFont);
        n.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        n.setSpacingBefore(3f);

        cell.addElement(l);
        cell.addElement(v);
        cell.addElement(n);
        return cell;
    }

    // =========================================================
    // SECTION TITLE
    // =========================================================

    private void addSectionTitle(
            Document document,
            String text,
            Font font,
            boolean arabic
    ) throws Exception {

        PdfPTable section = new PdfPTable(new float[]{0.08f, 3.92f});
        section.setWidthPercentage(100);
        section.setSpacingAfter(7f);
        if (arabic) {
            section.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }

        PdfPCell accent = new PdfPCell();
        accent.setBackgroundColor(ORANGE);
        accent.setBorder(Rectangle.NO_BORDER);
        accent.setFixedHeight(22f);

        PdfPCell title = cell(text, font, arabic, false);
        title.setBorder(Rectangle.NO_BORDER);
        title.setPaddingLeft(8f);
        title.setPaddingRight(8f);

        section.addCell(accent);
        section.addCell(title);
        document.add(section);
    }

    // =========================================================
    // ACTIVITY TABLE
    // =========================================================

    private void addActivityTable(
            Document document,
            List<FinancialActivity> activities,
            Font headerFont,
            Font normalFont,
            Font smallFont,
            boolean arabic
    ) throws Exception {

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.55f, 3.0f, 1.25f, 1.55f, 1.65f, 1.25f});
        table.setHeaderRows(1);
        table.setSplitRows(true);
        table.setSplitLate(false);

        if (arabic) {
            table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }

        String[] headers = arabic
                ? new String[]{"التاريخ", "الوصف", "النوع", "المبلغ", "طريقة الدفع", "الحالة"}
                : new String[]{"Date", "Description", "Type", "Amount", "Payment Method", "Status"};

        for (String header : headers) {
            PdfPCell c = cell(header, headerFont, arabic, true);
            c.setBackgroundColor(NAVY);
            c.setBorderColor(NAVY);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPaddingTop(8f);
            c.setPaddingBottom(8f);
            table.addCell(c);
        }

        if (activities.isEmpty()) {
            PdfPCell empty = cell(
                    arabic
                            ? "لا توجد عمليات مالية خلال الفترة المحددة"
                            : "No financial activity for the selected period",
                    normalFont,
                    arabic,
                    false
            );
            empty.setColspan(6);
            empty.setHorizontalAlignment(Element.ALIGN_CENTER);
            empty.setPadding(18f);
            empty.setBackgroundColor(SURFACE);
            empty.setBorderColor(BORDER);
            table.addCell(empty);
        } else {
            int rowIndex = 0;
            for (FinancialActivity a : activities) {
                Color bg = rowIndex++ % 2 == 0 ? WHITE : SURFACE;

                addDataCell(table,
                        a.getDate() == null ? "-" : a.getDate().format(DATE_TIME),
                        normalFont, false, bg);

                addDataCell(table,
                        safeText(a.getDescription()),
                        normalFont, arabic, bg);

                addDataCell(table,
                        translateType(a.getType(), arabic),
                        normalFont, arabic, bg);

                addDataCell(table,
                        money(a.getAmount(), arabic),
                        normalFont, arabic, bg);

                addDataCell(table,
                        translatePaymentMethod(a.getPaymentMethod(), arabic),
                        normalFont, arabic, bg);

                String status = translateStatus(a.getStatus(), arabic);
                PdfPCell statusCell = cell(status, statusFont(
                        a.getStatus(), normalFont
                ), arabic, false);
                statusCell.setBackgroundColor(bg);
                statusCell.setBorderColor(BORDER);
                statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                statusCell.setPadding(7f);
                table.addCell(statusCell);
            }
        }

        document.add(table);
    }

    private void addDataCell(
            PdfPTable table,
            String value,
            Font font,
            boolean arabic,
            Color background
    ) {
        PdfPCell c = cell(value, font, arabic, false);
        c.setBackgroundColor(background);
        c.setBorderColor(BORDER);
        c.setPadding(7f);
        table.addCell(c);
    }

    private Font statusFont(String status, Font base) {
        Color color = SLATE;
        if (status != null) {
            switch (status.toUpperCase(Locale.ROOT)) {
                case "PAID" -> color = GREEN;
                case "PENDING" -> color = AMBER;
                case "CANCELLED" -> color = RED;
                default -> color = SLATE;
            }
        }
        return new Font(base.getBaseFont(), base.getSize(), Font.BOLD, color);
    }

    // =========================================================
    // META / SECONDARY SUMMARY
    // =========================================================

    private void addReportMeta(
            Document document,
            ReportSummary summary,
            Font labelFont,
            Font normalFont,
            boolean arabic
    ) throws Exception {

        if (summary == null) {
            return;
        }

        PdfPTable meta = new PdfPTable(4);
        meta.setWidthPercentage(100);
        meta.setSpacingBefore(12f);
        if (arabic) {
            meta.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        }

        addMeta(meta,
                arabic ? "إيرادات العضويات" : "Membership Revenue",
                money(summary.getMembershipRevenue(), arabic),
                labelFont, normalFont, arabic);

        addMeta(meta,
                arabic ? "إيرادات أخرى" : "Other Revenue",
                money(summary.getOtherRevenue(), arabic),
                labelFont, normalFont, arabic);

        addMeta(meta,
                arabic ? "هامش الربح" : "Profit Margin",
                percentage(summary.getProfitMargin()),
                labelFont, normalFont, arabic);

        addMeta(meta,
                arabic ? "الأعضاء النشطون" : "Active Members",
                String.valueOf(summary.getActiveMembers()),
                labelFont, normalFont, arabic);

        document.add(meta);
    }

    private void addMeta(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font normalFont,
            boolean arabic
    ) {
        PdfPCell c = new PdfPCell();
        c.setBackgroundColor(SURFACE);
        c.setBorderColor(BORDER);
        c.setPadding(8f);
        c.setRunDirection(arabic
                ? PdfWriter.RUN_DIRECTION_RTL
                : PdfWriter.RUN_DIRECTION_LTR);

        Paragraph l = new Paragraph(label, labelFont);
        l.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        Paragraph v = new Paragraph(value, normalFont);
        v.setAlignment(arabic ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        v.setSpacingBefore(3f);

        c.addElement(l);
        c.addElement(v);
        table.addCell(c);
    }

    // =========================================================
    // GENERIC CELL
    // =========================================================

    private PdfPCell cell(
            String text,
            Font font,
            boolean arabic,
            boolean bold
    ) {
        PdfPCell c = new PdfPCell(new Phrase(
                text == null ? "" : text,
                font
        ));
        c.setRunDirection(arabic
                ? PdfWriter.RUN_DIRECTION_RTL
                : PdfWriter.RUN_DIRECTION_LTR);
        c.setHorizontalAlignment(arabic
                ? Element.ALIGN_RIGHT
                : Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6f);
        return c;
    }

    // =========================================================
    // LOGO
    // =========================================================

    private Image loadLogo() {
        String[] resources = {
                "/images/gymflow-logo.png",
                "/img/gymflow-logo.png",
                "/gymflow-logo.png"
        };

        for (String resource : resources) {
            try (InputStream in =
                         PdfReportExporter.class.getResourceAsStream(resource)) {
                if (in != null) {
                    return Image.getInstance(in.readAllBytes());
                }
            } catch (Exception ignored) {
                // Try next resource.
            }
        }
        return null;
    }

    // =========================================================
    // FONT
    // =========================================================

    private BaseFont createUnicodeFont() throws Exception {
        String[] fonts = {
                "/usr/share/fonts/truetype/noto/NotoNaskhArabic-Regular.ttf",
                "/usr/share/fonts/opentype/noto/NotoNaskhArabic-Regular.ttf",
                "/usr/share/fonts/truetype/noto/NotoSansArabic-Regular.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansArabic-Regular.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/freefont/FreeSans.ttf"
        };

        for (String path : fonts) {
            File f = new File(path);
            if (f.exists()) {
                return BaseFont.createFont(
                        path,
                        BaseFont.IDENTITY_H,
                        BaseFont.EMBEDDED
                );
            }
        }

        throw new IllegalStateException(
                "No Unicode Arabic font was found on this system."
        );
    }

    // =========================================================
    // PAGE EVENT — FOOTER + PAGE NUMBER
    // =========================================================

    private static final class GymFlowPageEvent extends PdfPageEventHelper {

        private final BaseFont font;
        private final boolean arabic;

        private GymFlowPageEvent(BaseFont font, boolean arabic) {
            this.font = font;
            this.arabic = arabic;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();

            float y = document.bottom() - 18f;

            canvas.setColorStroke(BORDER);
            canvas.setLineWidth(0.6f);
            canvas.moveTo(document.left(), y + 12f);
            canvas.lineTo(document.right(), y + 12f);
            canvas.stroke();

            Font footerFont = new Font(font, 7f, Font.NORMAL, MUTED);

            Phrase left = new Phrase(
                    arabic
                            ? "GymFlow • نظام إدارة الصالات الرياضية • MAKSOFT"
                            : "GymFlow • Gym Management System • MAKSOFT",
                    footerFont
            );

            Phrase right = new Phrase(
                    (arabic ? "صفحة " : "Page ")
                            + writer.getPageNumber(),
                    footerFont
            );

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_LEFT,
                    left,
                    document.left(),
                    y,
                    0,
                    arabic
                            ? PdfWriter.RUN_DIRECTION_RTL
                            : PdfWriter.RUN_DIRECTION_LTR,
                    0
            );

            ColumnText.showTextAligned(
                    canvas,
                    Element.ALIGN_RIGHT,
                    right,
                    document.right(),
                    y,
                    0
            );
        }
    }

    // =========================================================
    // FORMATTING
    // =========================================================

    private String formatPeriod(LocalDate from, LocalDate to, boolean arabic) {
        if (from == null || to == null) {
            return "-";
        }
        String a = from.format(DATE);
        String b = to.format(DATE);
        return arabic
                ? "من " + a + " إلى " + b
                : a + " — " + b;
    }

    private String money(BigDecimal value, boolean arabic) {
        return String.format(
                Locale.US,
                "%,.2f %s",
                safe(value).doubleValue(),
                arabic ? "ج.م" : "EGP"
        );
    }

    private String percentage(BigDecimal value) {
        return String.format(
                Locale.US,
                "%.2f%%",
                safe(value).doubleValue()
        );
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String translateType(String value, boolean arabic) {
        if (value == null || value.isBlank()) return "-";
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "REVENUE" -> arabic ? "إيراد" : "Revenue";
            case "EXPENSE" -> arabic ? "مصروف" : "Expense";
            default -> value;
        };
    }

    private String translatePaymentMethod(String value, boolean arabic) {
        if (value == null || value.isBlank()) return "-";
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "CASH" -> arabic ? "نقدي" : "Cash";
            case "CARD" -> arabic ? "بطاقة" : "Card";
            case "BANK_TRANSFER" -> arabic ? "تحويل بنكي" : "Bank Transfer";
            case "ONLINE" -> arabic ? "دفع إلكتروني" : "Online";
            case "WALLET" -> arabic ? "محفظة إلكترونية" : "Wallet";
            default -> value;
        };
    }

    private String translateStatus(String value, boolean arabic) {
        if (value == null || value.isBlank()) return "-";
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "PAID" -> arabic ? "مدفوع" : "Paid";
            case "PENDING" -> arabic ? "معلق" : "Pending";
            case "CANCELLED" -> arabic ? "ملغي" : "Cancelled";
            default -> value;
        };
    }
}
