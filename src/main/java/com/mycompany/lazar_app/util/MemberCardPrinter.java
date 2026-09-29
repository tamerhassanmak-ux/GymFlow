package com.mycompany.lazar_app.util;

import com.mycompany.lazar_app.model.Member;
import com.mycompany.lazar_app.model.MemberMembership;
import com.mycompany.lazar_app.repository.MemberMembershipRepository;

import java.io.File;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.time.format.DateTimeFormatter;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

/**
 * Centralized renderer/printer for GymFlow membership cards and QR labels.
 *
 * One renderer is used by:
 * - Add/Edit Member
 * - Member Details
 * - Members table
 *
 * This keeps the preview and every reprint consistent.
 */
public final class MemberCardPrinter {

    public enum PrintResult {
        PRINTED,
        CANCELLED,
        NO_PRINTER,
        FAILED
    }

    // ISO/IEC 7810 ID-1 / CR80 physical card size.
    private static final double CARD_WIDTH_MM = 85.60;
    private static final double CARD_HEIGHT_MM = 53.98;

    /*
     * Screen/design size.  Keep the exact CR80 aspect ratio so the preview
     * and the physical print never stretch differently.
     */
    private static final double CARD_WIDTH = 540.0;
    private static final double CARD_HEIGHT =
            CARD_WIDTH * CARD_HEIGHT_MM / CARD_WIDTH_MM;

    /*
     * Card-printer drivers sometimes expose the media as 85.6 x 54 mm,
     * 86 x 54 mm, "CR80", "ID Card", etc.  A tight tolerance prevents
     * accidentally selecting unrelated photo/A-series media.
     */
    private static final double CARD_MEDIA_TOLERANCE_MM = 3.0;

    private static final DateTimeFormatter CARD_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM/yyyy");

    private MemberCardPrinter() {
    }


    // =========================================================
    // SAVED MEMBER CHECK
    // =========================================================

    public static boolean isSavedMember(Member member) {

        return member != null
                && member.getId() > 0
                && member.getMemberCode() != null
                && !member.getMemberCode().isBlank();
    }


    // =========================================================
    // MEMBERSHIP CARD PREVIEW
    // =========================================================

    public static BorderPane createCard(
            Member member,
            boolean arabic
    ) {

        /*
         * Subscription data must come from member_memberships.
         * The Member master record no longer owns plan/start/end data.
         */
        MemberMembership membership =
                resolveMembershipForCard(member);

        BorderPane card =
                new BorderPane();

        card.setPrefSize(
                CARD_WIDTH,
                CARD_HEIGHT
        );

        card.setMinSize(
                CARD_WIDTH,
                CARD_HEIGHT
        );

        card.setMaxSize(
                CARD_WIDTH,
                CARD_HEIGHT
        );

        card.setPadding(
                new Insets(
                        24
                )
        );

        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0f172a, #111827 58%, #1e293b);"
                        + "-fx-background-radius: 22;"
                        + "-fx-border-radius: 22;"
                        + "-fx-border-color: rgba(255,255,255,0.16);"
                        + "-fx-border-width: 1;"
                        + "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.22), 20, 0.18, 0, 8);"
        );


        // -----------------------------------------------------
        // HEADER
        // -----------------------------------------------------

        Label brand =
                new Label(
                        "GYMFLOW"
                );

        brand.setFont(
                Font.font(
                        Font.getDefault().getFamily(),
                        FontWeight.EXTRA_BOLD,
                        26
                )
        );

        brand.setStyle(
                "-fx-text-fill: white;"
        );


        Label cardType =
                new Label(
                        arabic
                                ? "بطاقة عضوية"
                                : "MEMBERSHIP CARD"
                );

        cardType.setStyle(
                "-fx-text-fill: #fb923c;"
                        + "-fx-font-size: 10px;"
                        + "-fx-font-weight: bold;"
        );


        VBox brandBox =
                new VBox(
                        0,
                        brand,
                        cardType
                );


        Label status =
                new Label(
                        translateStatus(
                                membership == null
                                        ? null
                                        : membership.getStatus(),
                                arabic
                        )
                );

        status.setStyle(
                "-fx-background-color: rgba(34,197,94,0.16);"
                        + "-fx-border-color: rgba(74,222,128,0.45);"
                        + "-fx-border-radius: 12;"
                        + "-fx-background-radius: 12;"
                        + "-fx-padding: 5 10;"
                        + "-fx-text-fill: #86efac;"
                        + "-fx-font-size: 10px;"
                        + "-fx-font-weight: bold;"
        );


        HBox header =
                new HBox(
                        12,
                        brandBox,
                        createSpacer(),
                        status
                );

        header.setAlignment(
                Pos.CENTER_LEFT
        );


        // -----------------------------------------------------
        // PHOTO
        // -----------------------------------------------------

        StackPane photoBox =
                createPhotoBox(
                        member,
                        arabic
                );


        // -----------------------------------------------------
        // MEMBER INFO
        // -----------------------------------------------------

        Label name =
                new Label(
                        safe(
                                member == null
                                        ? null
                                        : member.getName(),
                                arabic
                                        ? "اسم العضو"
                                        : "Member Name"
                        )
                );

        name.setWrapText(
                true
        );

        name.setMaxWidth(
                245
        );

        name.setFont(
                Font.font(
                        Font.getDefault().getFamily(),
                        FontWeight.BOLD,
                        20
                )
        );

        name.setStyle(
                "-fx-text-fill: white;"
        );


        Label code =
                new Label(
                        safe(
                                member == null
                                        ? null
                                        : member.getMemberCode(),
                                "-"
                        )
                );

        code.setStyle(
                "-fx-text-fill: #cbd5e1;"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
        );


        Label planTitle =
                new Label(
                        arabic
                                ? "الخطة"
                                : "PLAN"
                );

        planTitle.setStyle(
                "-fx-text-fill: #94a3b8;"
                        + "-fx-font-size: 9px;"
                        + "-fx-font-weight: bold;"
        );


        Label plan =
                new Label(
                        membershipPlanName(
                                membership,
                                arabic
                        )
                );

        plan.setStyle(
                "-fx-text-fill: #fdba74;"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
        );


        VBox info =
                new VBox(
                        6,
                        name,
                        code,
                        planTitle,
                        plan
                );

        info.setAlignment(
                arabic
                        ? Pos.CENTER_RIGHT
                        : Pos.CENTER_LEFT
        );

        HBox.setHgrow(
                info,
                Priority.ALWAYS
        );


        // -----------------------------------------------------
        // QR
        // -----------------------------------------------------

        VBox qrArea =
                createQrArea(
                        member,
                        arabic
                );


        HBox body =
                new HBox(
                        18,
                        photoBox,
                        info,
                        qrArea
                );

        body.setAlignment(
                Pos.CENTER_LEFT
        );


        // -----------------------------------------------------
        // FOOTER
        // -----------------------------------------------------

        LocalDate expiry =
                membership == null
                        ? null
                        : membership.getEndDate();


        Label validTitle =
                new Label(
                        arabic
                                ? "صالح حتى"
                                : "VALID THRU"
                );

        validTitle.setStyle(
                "-fx-text-fill: #94a3b8;"
                        + "-fx-font-size: 8px;"
                        + "-fx-font-weight: bold;"
        );


        Label validValue =
                new Label(
                        expiry == null
                                ? "--/----"
                                : expiry.format(
                                        CARD_DATE_FORMAT
                                )
                );

        validValue.setStyle(
                "-fx-text-fill: white;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
        );


        VBox validBox =
                new VBox(
                        1,
                        validTitle,
                        validValue
                );


        Label footerBrand =
                new Label(
                        "Powered by MAKSOFT"
                );

        footerBrand.setStyle(
                "-fx-text-fill: #64748b;"
                        + "-fx-font-size: 9px;"
                        + "-fx-font-weight: bold;"
        );


        HBox footer =
                new HBox(
                        10,
                        validBox,
                        createSpacer(),
                        footerBrand
                );

        footer.setAlignment(
                Pos.CENTER_LEFT
        );


        card.setTop(
                header
        );

        card.setCenter(
                body
        );

        card.setBottom(
                footer
        );


        BorderPane.setMargin(
                body,
                new Insets(
                        16,
                        0,
                        14,
                        0
                )
        );


        if (arabic) {

            card.setNodeOrientation(
                    NodeOrientation.RIGHT_TO_LEFT
            );
        }

        return card;
    }


    // =========================================================
    // QR LABEL PREVIEW
    // =========================================================

    public static VBox createQrLabel(
            Member member,
            boolean arabic
    ) {

        ImageView qrView =
                new ImageView();

        qrView.setFitWidth(
                230
        );

        qrView.setFitHeight(
                230
        );

        qrView.setPreserveRatio(
                true
        );

        qrView.setSmooth(
                false
        );


        if (isSavedMember(member)) {

            try {

                qrView.setImage(
                        QrCodeGenerator.generate(
                                member.getMemberCode(),
                                300,
                                300
                        )
                );

            } catch (Exception exception) {

                exception.printStackTrace();
            }
        }


        Label name =
                new Label(
                        safe(
                                member == null
                                        ? null
                                        : member.getName(),
                                arabic
                                        ? "عضو"
                                        : "Member"
                        )
                );

        name.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #111827;"
        );


        Label code =
                new Label(
                        safe(
                                member == null
                                        ? null
                                        : member.getMemberCode(),
                                "-"
                        )
                );

        code.setStyle(
                "-fx-font-size: 12px;"
                        + "-fx-text-fill: #475569;"
                        + "-fx-font-weight: bold;"
        );


        Label gym =
                new Label(
                        "GYMFLOW"
                );

        gym.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: #f97316;"
        );


        VBox label =
                new VBox(
                        7,
                        gym,
                        qrView,
                        name,
                        code
                );

        label.setAlignment(
                Pos.CENTER
        );

        label.setPadding(
                new Insets(
                        18
                )
        );

        label.setStyle(
                "-fx-background-color: white;"
                        + "-fx-border-color: #d1d5db;"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 12;"
                        + "-fx-background-radius: 12;"
        );


        if (arabic) {

            label.setNodeOrientation(
                    NodeOrientation.RIGHT_TO_LEFT
            );
        }

        return label;
    }


    // =========================================================
    // PRINT CARD
    // =========================================================

    public static PrintResult printCard(
            Window owner,
            Member member,
            boolean arabic
    ) {

        if (!isSavedMember(member)) {
            return PrintResult.FAILED;
        }

        BorderPane card =
                createCard(
                        member,
                        arabic
                );

        /*
         * The preview uses a drop shadow to look like a card on screen.
         * A physical PVC/ID card does not need that shadow, and removing it
         * also prevents the effect from consuming printable space.
         */
        card.setStyle(
                card.getStyle()
                        + "-fx-effect: null;"
        );

        return printNode(
                owner,
                card,
                arabic
                        ? "كارت عضوية GymFlow"
                        : "GymFlow Membership Card",
                CARD_WIDTH_MM,
                CARD_HEIGHT_MM
        );
    }


    // =========================================================
    // PRINT QR
    // =========================================================

    public static PrintResult printQrLabel(
            Window owner,
            Member member,
            boolean arabic
    ) {

        if (!isSavedMember(member)) {
            return PrintResult.FAILED;
        }

        VBox qr =
                createQrLabel(
                        member,
                        arabic
                );

        return printNode(
                owner,
                qr,
                arabic
                        ? "ملصق QR للعضو"
                        : "Member QR Label",
                50.0,
                60.0
        );
    }


    // =========================================================
    // PRINT ENGINE
    // =========================================================

    private static PrintResult printNode(
            Window owner,
            Node content,
            String jobName,
            double targetWidthMm,
            double targetHeightMm
    ) {

        PrinterJob job =
                PrinterJob.createPrinterJob();

        if (job == null) {
            return PrintResult.NO_PRINTER;
        }

        job.getJobSettings()
                .setJobName(
                        jobName
                );

        /*
         * Let the operator select the physical printer first.  The CR80
         * layout is resolved AFTER this dialog because job.getPrinter() may
         * change when another printer is selected.
         */
        boolean accepted =
                job.showPrintDialog(
                        owner
                );

        if (!accepted) {
            return PrintResult.CANCELLED;
        }

        PageLayout pageLayout;

        if (isIdCardSize(
                targetWidthMm,
                targetHeightMm
        )) {

            pageLayout =
                    configureIdCardPageLayout(
                            job,
                            targetWidthMm,
                            targetHeightMm
                    );

        } else {

            pageLayout =
                    job.getJobSettings()
                            .getPageLayout();
        }

        if (pageLayout == null) {
            job.cancelJob();
            return PrintResult.FAILED;
        }

        /*
         * Group is important here: its bounds follow the transformed child.
         * This means StackPane centers the SCALED card rather than centering
         * the original 540px preview node and clipping the result.
         */
        Group scaledContent =
                new Group(
                        content
                );

        StackPane printablePage =
                new StackPane(
                        scaledContent
                );

        printablePage.setAlignment(
                Pos.CENTER
        );

        double printableWidth =
                pageLayout.getPrintableWidth();

        double printableHeight =
                pageLayout.getPrintableHeight();

        if (printableWidth <= 0
                || printableHeight <= 0) {

            job.cancelJob();
            return PrintResult.FAILED;
        }

        printablePage.setPrefSize(
                printableWidth,
                printableHeight
        );

        printablePage.setMinSize(
                printableWidth,
                printableHeight
        );

        printablePage.setMaxSize(
                printableWidth,
                printableHeight
        );

        /*
         * Attach to a temporary scene so CSS, fonts, QR/image sizes and all
         * layout bounds are finalized before we calculate the physical scale.
         */
        new Scene(
                printablePage
        );

        printablePage.applyCss();
        printablePage.layout();

        double contentWidth =
                content.getLayoutBounds()
                        .getWidth();

        double contentHeight =
                content.getLayoutBounds()
                        .getHeight();

        if (contentWidth <= 0
                || contentHeight <= 0) {

            job.cancelJob();
            return PrintResult.FAILED;
        }

        /*
         * JavaFX printing units are points (1/72 inch).  Convert the requested
         * millimetres explicitly so a CR80 job is 85.60 x 53.98 mm in the
         * physical world, independent of monitor DPI/resolution.
         */
        double targetWidthPoints =
                millimetersToPoints(
                        targetWidthMm
                );

        double targetHeightPoints =
                millimetersToPoints(
                        targetHeightMm
                );

        double physicalScale =
                Math.min(
                        targetWidthPoints / contentWidth,
                        targetHeightPoints / contentHeight
                );

        double printableAreaScale =
                Math.min(
                        printableWidth / contentWidth,
                        printableHeight / contentHeight
                );

        /*
         * On a proper borderless/edge-to-edge card printer, physicalScale is
         * used unchanged.  If a driver reports non-printable hardware margins,
         * reduce only as much as necessary to avoid clipping member data/QR.
         */
        double scale =
                Math.min(
                        physicalScale,
                        printableAreaScale
                );

        if (!Double.isFinite(scale)
                || scale <= 0) {

            job.cancelJob();
            return PrintResult.FAILED;
        }

        Scale transform =
                new Scale(
                        scale,
                        scale,
                        0.0,
                        0.0
                );

        content.getTransforms()
                .add(
                        transform
                );

        printablePage.requestLayout();
        printablePage.applyCss();
        printablePage.layout();

        printScaleDiagnostics(
                targetWidthMm,
                targetHeightMm,
                contentWidth * scale,
                contentHeight * scale,
                printableWidth,
                printableHeight,
                scale < physicalScale - 0.0001
        );

        boolean printed;

        try {

            printed =
                    job.printPage(
                            pageLayout,
                            printablePage
                    );

        } catch (Exception exception) {

            exception.printStackTrace();

            job.cancelJob();

            return PrintResult.FAILED;

        } finally {

            content.getTransforms()
                    .remove(
                            transform
                    );
        }

        if (!printed) {

            job.cancelJob();
            return PrintResult.FAILED;
        }

        boolean completed =
                job.endJob();

        return completed
                ? PrintResult.PRINTED
                : PrintResult.FAILED;
    }


    // =========================================================
    // ID CARD PAGE LAYOUT
    // =========================================================

    /**
     * Configures the selected printer for the standard CR80 / ID-1 card size
     * whenever that media is exposed by the printer driver.
     */
    private static PageLayout configureIdCardPageLayout(
            PrinterJob job,
            double targetWidthMm,
            double targetHeightMm
    ) {

        PageLayout fallback =
                job.getJobSettings()
                        .getPageLayout();

        Printer printer =
                job.getPrinter();

        if (printer == null) {
            return fallback;
        }

        /*
         * Respect a CR80/custom-card size already chosen in the driver dialog,
         * even when that custom media is not listed in getSupportedPapers().
         */
        if (isLayoutCloseToTarget(
                fallback,
                targetWidthMm,
                targetHeightMm,
                CARD_MEDIA_TOLERANCE_MM
        )) {

            printLayoutDiagnostics(
                    printer,
                    fallback,
                    true
            );

            return fallback;
        }

        Paper cardPaper =
                findBestCardPaper(
                        printer,
                        targetWidthMm,
                        targetHeightMm
                );

        if (cardPaper == null) {

            printLayoutDiagnostics(
                    printer,
                    fallback,
                    false
            );

            return fallback;
        }

        try {

            PageLayout portrait =
                    printer.createPageLayout(
                            cardPaper,
                            PageOrientation.PORTRAIT,
                            Printer.MarginType.HARDWARE_MINIMUM
                    );

            PageLayout landscape =
                    printer.createPageLayout(
                            cardPaper,
                            PageOrientation.LANDSCAPE,
                            Printer.MarginType.HARDWARE_MINIMUM
                    );

            PageLayout selected =
                    pageLayoutScore(
                            landscape,
                            targetWidthMm,
                            targetHeightMm
                    )
                    <= pageLayoutScore(
                            portrait,
                            targetWidthMm,
                            targetHeightMm
                    )
                            ? landscape
                            : portrait;

            job.getJobSettings()
                    .setPageLayout(
                            selected
                    );

            printLayoutDiagnostics(
                    printer,
                    selected,
                    true
            );

            return selected;

        } catch (Exception exception) {

            /*
             * Some printer drivers advertise media but reject programmatic
             * layout changes. In that case, keep the user's print-dialog
             * selection instead of failing the whole print job.
             */
            exception.printStackTrace();

            printLayoutDiagnostics(
                    printer,
                    fallback,
                    false
            );

            return fallback;
        }
    }


    /**
     * Finds media whose physical dimensions are closest to ID-1 / CR80.
     * Width/height are compared orientation-independently because different
     * drivers report card stock as portrait or landscape media.
     */
    private static Paper findBestCardPaper(
            Printer printer,
            double targetWidthMm,
            double targetHeightMm
    ) {

        if (printer.getPrinterAttributes() == null
                || printer.getPrinterAttributes()
                        .getSupportedPapers() == null) {

            return null;
        }

        double targetLong =
                Math.max(
                        targetWidthMm,
                        targetHeightMm
                );

        double targetShort =
                Math.min(
                        targetWidthMm,
                        targetHeightMm
                );

        Paper best = null;
        double bestScore = Double.MAX_VALUE;

        for (Paper paper
                : printer.getPrinterAttributes()
                        .getSupportedPapers()) {

            if (paper == null) {
                continue;
            }

            double paperWidthMm =
                    pointsToMillimeters(
                            paper.getWidth()
                    );

            double paperHeightMm =
                    pointsToMillimeters(
                            paper.getHeight()
                    );

            double paperLong =
                    Math.max(
                            paperWidthMm,
                            paperHeightMm
                    );

            double paperShort =
                    Math.min(
                            paperWidthMm,
                            paperHeightMm
                    );

            double longDifference =
                    Math.abs(
                            paperLong - targetLong
                    );

            double shortDifference =
                    Math.abs(
                            paperShort - targetShort
                    );

            /*
             * Do not accidentally switch an A4/Letter/photo printer to an
             * unrelated paper size. A real card-driver media definition is
             * normally exact or within only a few millimetres of CR80.
             */
            if (longDifference > CARD_MEDIA_TOLERANCE_MM
                    || shortDifference > CARD_MEDIA_TOLERANCE_MM) {

                continue;
            }

            double score =
                    longDifference
                    + shortDifference;

            String paperName =
                    paper.getName() == null
                            ? ""
                            : paper.getName()
                                    .toUpperCase();

            /*
             * Prefer driver media explicitly named for cards when dimensions
             * are similarly close.
             */
            if (paperName.contains("CR80")
                    || paperName.contains("ID-1")
                    || paperName.contains("ID1")
                    || paperName.contains("CARD")
                    || paperName.contains("PVC")) {

                score -= 0.50;
            }

            if (score < bestScore) {

                bestScore = score;
                best = paper;
            }
        }

        return best;
    }


    private static double pageLayoutScore(
            PageLayout layout,
            double targetWidthMm,
            double targetHeightMm
    ) {

        if (layout == null) {
            return Double.MAX_VALUE;
        }

        return Math.abs(
                pointsToMillimeters(
                        getLayoutPageWidth(layout)
                )
                - targetWidthMm
        )
                + Math.abs(
                        pointsToMillimeters(
                                getLayoutPageHeight(layout)
                        )
                        - targetHeightMm
                );
    }


    private static boolean isLayoutCloseToTarget(
            PageLayout layout,
            double targetWidthMm,
            double targetHeightMm,
            double toleranceMm
    ) {

        if (layout == null) {
            return false;
        }

        double pageWidthMm =
                pointsToMillimeters(
                        getLayoutPageWidth(layout)
                );

        double pageHeightMm =
                pointsToMillimeters(
                        getLayoutPageHeight(layout)
                );

        return Math.abs(
                pageWidthMm - targetWidthMm
        ) <= toleranceMm
                && Math.abs(
                        pageHeightMm - targetHeightMm
                ) <= toleranceMm;
    }


    private static boolean isIdCardSize(
            double widthMm,
            double heightMm
    ) {

        return Math.abs(
                widthMm - CARD_WIDTH_MM
        ) < 0.01
                && Math.abs(
                        heightMm - CARD_HEIGHT_MM
                ) < 0.01;
    }


    private static void printLayoutDiagnostics(
            Printer printer,
            PageLayout layout,
            boolean cardMediaDetected
    ) {

        if (layout == null) {
            return;
        }

        String printerName =
                printer == null
                        ? "Unknown printer"
                        : printer.getName();

        System.out.printf(
                java.util.Locale.US,
                "[GymFlow Card Print] printer=%s, CR80-media=%s, page=%.2f x %.2f mm, printable=%.2f x %.2f mm%n",
                printerName,
                cardMediaDetected,
                pointsToMillimeters(
                        getLayoutPageWidth(layout)
                ),
                pointsToMillimeters(
                        getLayoutPageHeight(layout)
                ),
                pointsToMillimeters(
                        layout.getPrintableWidth()
                ),
                pointsToMillimeters(
                        layout.getPrintableHeight()
                )
        );
    }


    /**
     * JavaFX PageLayout does not expose getPageWidth()/getPageHeight().
     * The physical page size belongs to Paper, while the page orientation
     * determines whether width/height must be swapped.
     */
    private static double getLayoutPageWidth(
            PageLayout layout
    ) {

        if (layout == null
                || layout.getPaper() == null) {

            return 0.0;
        }

        Paper paper =
                layout.getPaper();

        PageOrientation orientation =
                layout.getPageOrientation();

        boolean landscape =
                orientation == PageOrientation.LANDSCAPE
                || orientation == PageOrientation.REVERSE_LANDSCAPE;

        return landscape
                ? paper.getHeight()
                : paper.getWidth();
    }


    private static double getLayoutPageHeight(
            PageLayout layout
    ) {

        if (layout == null
                || layout.getPaper() == null) {

            return 0.0;
        }

        Paper paper =
                layout.getPaper();

        PageOrientation orientation =
                layout.getPageOrientation();

        boolean landscape =
                orientation == PageOrientation.LANDSCAPE
                || orientation == PageOrientation.REVERSE_LANDSCAPE;

        return landscape
                ? paper.getWidth()
                : paper.getHeight();
    }


    private static double pointsToMillimeters(
            double points
    ) {

        return points
                * 25.4
                / 72.0;
    }


    private static void printScaleDiagnostics(
            double targetWidthMm,
            double targetHeightMm,
            double printedWidthPoints,
            double printedHeightPoints,
            double printableWidthPoints,
            double printableHeightPoints,
            boolean reducedForMargins
    ) {

        System.out.printf(
                java.util.Locale.US,
                "[GymFlow Card Print] target=%.2f x %.2f mm, output=%.2f x %.2f mm, printable=%.2f x %.2f mm, reduced-for-margins=%s%n",
                targetWidthMm,
                targetHeightMm,
                pointsToMillimeters(printedWidthPoints),
                pointsToMillimeters(printedHeightPoints),
                pointsToMillimeters(printableWidthPoints),
                pointsToMillimeters(printableHeightPoints),
                reducedForMargins
        );
    }


    // =========================================================
    // PHOTO
    // =========================================================

    private static StackPane createPhotoBox(
            Member member,
            boolean arabic
    ) {

        StackPane box =
                new StackPane();

        box.setMinSize(
                100,
                118
        );

        box.setPrefSize(
                100,
                118
        );

        box.setMaxSize(
                100,
                118
        );

        box.setStyle(
                "-fx-background-color: rgba(255,255,255,0.07);"
                        + "-fx-border-color: rgba(255,255,255,0.16);"
                        + "-fx-border-width: 1;"
                        + "-fx-border-radius: 16;"
                        + "-fx-background-radius: 16;"
        );


        Label placeholder =
                new Label(
                        arabic
                                ? "صورة"
                                : "PHOTO"
                );

        placeholder.setStyle(
                "-fx-text-fill: #64748b;"
                        + "-fx-font-size: 10px;"
                        + "-fx-font-weight: bold;"
        );


        Image image =
                loadMemberPhoto(
                        member
                );


        if (image == null) {

            box.getChildren()
                    .add(
                            placeholder
                    );

            return box;
        }


        ImageView photo =
                new ImageView(
                        image
                );

        photo.setFitWidth(
                92
        );

        photo.setFitHeight(
                110
        );

        photo.setPreserveRatio(
                true
        );

        photo.setSmooth(
                true
        );


        box.getChildren()
                .add(
                        photo
                );

        return box;
    }


    private static Image loadMemberPhoto(
            Member member
    ) {

        if (member == null
                || member.getPhotoPath() == null
                || member.getPhotoPath().isBlank()) {

            return null;
        }


        try {

            File file =
                    new File(
                            member.getPhotoPath()
                    );

            if (!file.exists()
                    || !file.isFile()) {

                return null;
            }


            return new Image(
                    file.toURI().toString(),
                    240,
                    260,
                    true,
                    true
            );

        } catch (Exception exception) {

            exception.printStackTrace();

            return null;
        }
    }


    // =========================================================
    // QR AREA
    // =========================================================

    private static VBox createQrArea(
            Member member,
            boolean arabic
    ) {

        StackPane qrBox =
                new StackPane();

        qrBox.setMinSize(
                118,
                118
        );

        qrBox.setPrefSize(
                118,
                118
        );

        qrBox.setMaxSize(
                118,
                118
        );

        qrBox.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 12;"
                        + "-fx-padding: 6;"
        );


        if (isSavedMember(member)) {

            try {

                Image qr =
                        QrCodeGenerator.generate(
                                member.getMemberCode(),
                                240,
                                240
                        );


                ImageView qrView =
                        new ImageView(
                                qr
                        );

                qrView.setFitWidth(
                        106
                );

                qrView.setFitHeight(
                        106
                );

                qrView.setPreserveRatio(
                        true
                );

                qrView.setSmooth(
                        false
                );


                qrBox.getChildren()
                        .add(
                                qrView
                        );

            } catch (Exception exception) {

                exception.printStackTrace();

                qrBox.getChildren()
                        .add(
                                new Label(
                                        "QR"
                                )
                        );
            }

        } else {

            qrBox.getChildren()
                    .add(
                            new Label(
                                    "QR"
                            )
                    );
        }


        Label scan =
                new Label(
                        arabic
                                ? "امسح للتعريف بالعضو"
                                : "SCAN TO IDENTIFY"
                );

        scan.setTextAlignment(
                TextAlignment.CENTER
        );

        scan.setWrapText(
                true
        );

        scan.setMaxWidth(
                120
        );

        scan.setStyle(
                "-fx-text-fill: #94a3b8;"
                        + "-fx-font-size: 8px;"
                        + "-fx-font-weight: bold;"
        );


        VBox area =
                new VBox(
                        5,
                        qrBox,
                        scan
                );

        area.setAlignment(
                Pos.CENTER
        );

        return area;
    }


    // =========================================================
    // TRANSLATIONS / HELPERS
    // =========================================================

    private static MemberMembership resolveMembershipForCard(
            Member member
    ) {

        if (member == null
                || member.getId() <= 0) {

            return null;
        }

        MemberMembershipRepository repository =
                new MemberMembershipRepository();

        try {

            Optional<MemberMembership> current =
                    repository.findCurrentMembershipByMemberId(
                            member.getId()
                    );

            if (current.isPresent()) {
                return current.get();
            }

            /*
             * If there is no current active membership, show the most
             * recent historical subscription instead of reading legacy
             * fields from members. findByMemberId is ordered newest first.
             */
            List<MemberMembership> history =
                    repository.findByMemberId(
                            member.getId()
                    );

            return history.isEmpty()
                    ? null
                    : history.get(0);

        } catch (SQLException exception) {

            System.err.println(
                    "Unable to load member membership for card: "
                            + member.getId()
            );

            exception.printStackTrace();

            return null;
        }
    }


    private static String membershipPlanName(
            MemberMembership membership,
            boolean arabic
    ) {

        if (membership == null) {
            return "-";
        }

        String primary =
                arabic
                        ? membership.getPlanNameAr()
                        : membership.getPlanNameEn();

        String fallback =
                arabic
                        ? membership.getPlanNameEn()
                        : membership.getPlanNameAr();

        if (primary != null
                && !primary.isBlank()) {

            return primary.trim();
        }

        if (fallback != null
                && !fallback.isBlank()) {

            return fallback.trim();
        }

        String code =
                membership.getPlanCode();

        return code == null
                || code.isBlank()
                ? "-"
                : code.trim();
    }


    private static String translateStatus(
            String status,
            boolean arabic
    ) {

        if (status == null
                || status.isBlank()) {

            return "-";
        }


        if (!arabic) {
            return status;
        }


        return switch (status) {

            case "Active" ->
                    "نشط";

            case "Expired" ->
                    "منتهي";

            case "Suspended" ->
                    "موقوف";

            case "Inactive" ->
                    "غير نشط";

            default ->
                    status;
        };
    }


    private static String safe(
            String value,
            String fallback
    ) {

        if (value == null
                || value.isBlank()) {

            return fallback;
        }

        return value.trim();
    }


    private static javafx.scene.layout.Region createSpacer() {

        javafx.scene.layout.Region spacer =
                new javafx.scene.layout.Region();

        HBox.setHgrow(
                spacer,
                Priority.ALWAYS
        );

        return spacer;
    }


    private static double millimetersToPoints(
            double millimeters
    ) {

        return millimeters
                / 25.4
                * 72.0;
    }
}
