package com.mycompany.lazar_app.model;

import java.time.LocalDate;

public class Member {

    // =========================================================
    // BASIC DATA
    // =========================================================

    private int id;

    private String memberCode;

    private String name;

    private String phone;

    private String whatsapp;

    private String email;


    // =========================================================
    // PERSONAL DATA
    // =========================================================

    private String gender;

    private LocalDate birthDate;

    private String photoPath;


    // =========================================================
    // MEMBERSHIP DATA
    // =========================================================

    private String membership;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;


    // =========================================================
    // OTHER DATA
    // =========================================================

    private String notes;


    // =========================================================
    // FULL CONSTRUCTOR
    // =========================================================

    public Member(
            int id,
            String memberCode,
            String name,
            String phone,
            String whatsapp,
            String email,
            String gender,
            LocalDate birthDate,
            String membership,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String notes,
            String photoPath
    ) {

        this.id = id;

        this.memberCode =
                normalizeMemberCode(
                        memberCode,
                        id
                );

        this.name =
                safeString(name);

        this.phone =
                safeString(phone);

        this.whatsapp =
                safeString(whatsapp);

        this.email =
                safeString(email);

        this.gender =
                safeString(gender);

        this.birthDate =
                birthDate;

        this.membership =
                safeString(membership);

        this.startDate =
                startDate;

        this.endDate =
                endDate;

        this.status =
                safeString(status);

        this.notes =
                safeString(notes);

        this.photoPath =
                safeString(photoPath);
    }


    // =========================================================
    // SIMPLE CONSTRUCTOR
    // Used by current test/demo data
    // =========================================================

    public Member(
            int id,
            String name,
            String phone,
            String membership,
            String status
    ) {

        this(
                id,
                generateMemberCode(id),
                name,
                phone,
                phone,
                "",
                "",
                null,
                membership,
                LocalDate.now(),
                calculateMembershipEndDate(
                        LocalDate.now(),
                        membership
                ),
                status,
                "",
                ""
        );
    }


    // =========================================================
    // GENERATE MEMBER CODE
    // =========================================================

    private static String generateMemberCode(
            int id
    ) {

        return String.format(
                "LZR-%06d",
                id
        );
    }


    // =========================================================
    // NORMALIZE MEMBER CODE
    // =========================================================

    private static String normalizeMemberCode(
            String memberCode,
            int id
    ) {

        if (memberCode == null
                || memberCode.isBlank()) {

            return generateMemberCode(
                    id
            );
        }

        return memberCode.trim();
    }


    // =========================================================
    // CALCULATE MEMBERSHIP END DATE
    // =========================================================

    private static LocalDate calculateMembershipEndDate(
            LocalDate startDate,
            String membership
    ) {

        if (startDate == null) {

            return null;
        }


        if (membership == null
                || membership.isBlank()) {

            return startDate;
        }


        return switch (membership) {

            case "Monthly" ->
                    startDate.plusMonths(1);

            case "3 Months" ->
                    startDate.plusMonths(3);

            case "6 Months" ->
                    startDate.plusMonths(6);

            case "Yearly" ->
                    startDate.plusYears(1);

            default ->
                    startDate;
        };
    }


    // =========================================================
    // SAFE STRING
    // =========================================================

    private static String safeString(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }


    // =========================================================
    // GETTERS
    // =========================================================

    public int getId() {

        return id;
    }


    public String getMemberCode() {

        return memberCode;
    }


    public String getName() {

        return name;
    }


    public String getPhone() {

        return phone;
    }


    public String getWhatsapp() {

        return whatsapp;
    }


    public String getEmail() {

        return email;
    }


    public String getGender() {

        return gender;
    }


    public LocalDate getBirthDate() {

        return birthDate;
    }


    public String getMembership() {

        return membership;
    }


    public LocalDate getStartDate() {

        return startDate;
    }


    public LocalDate getEndDate() {

        return endDate;
    }


    public String getStatus() {

        return status;
    }


    public String getNotes() {

        return notes;
    }


    public String getPhotoPath() {

        return photoPath;
    }


    // =========================================================
    // SETTERS
    // =========================================================

    public void setId(
            int id
    ) {

        this.id = id;
    }


    public void setMemberCode(
            String memberCode
    ) {

        this.memberCode =
                normalizeMemberCode(
                        memberCode,
                        id
                );
    }


    public void setName(
            String name
    ) {

        this.name =
                safeString(name);
    }


    public void setPhone(
            String phone
    ) {

        this.phone =
                safeString(phone);
    }


    public void setWhatsapp(
            String whatsapp
    ) {

        this.whatsapp =
                safeString(whatsapp);
    }


    public void setEmail(
            String email
    ) {

        this.email =
                safeString(email);
    }


    public void setGender(
            String gender
    ) {

        this.gender =
                safeString(gender);
    }


    public void setBirthDate(
            LocalDate birthDate
    ) {

        this.birthDate =
                birthDate;
    }


    public void setMembership(
            String membership
    ) {

        this.membership =
                safeString(membership);
    }


    public void setStartDate(
            LocalDate startDate
    ) {

        this.startDate =
                startDate;
    }


    public void setEndDate(
            LocalDate endDate
    ) {

        this.endDate =
                endDate;
    }


    public void setStatus(
            String status
    ) {

        this.status =
                safeString(status);
    }


    public void setNotes(
            String notes
    ) {

        this.notes =
                safeString(notes);
    }


    public void setPhotoPath(
            String photoPath
    ) {

        this.photoPath =
                safeString(photoPath);
    }


    // =========================================================
    // HELPER METHODS
    // =========================================================

    public boolean isActive() {

        return "Active".equalsIgnoreCase(
                status
        );
    }


    public boolean isExpired() {

        if ("Expired".equalsIgnoreCase(
                status
        )) {

            return true;
        }


        return endDate != null
                && endDate.isBefore(
                        LocalDate.now()
                );
    }


    public long getRemainingDays() {

        if (endDate == null) {

            return 0;
        }


        long days =
                java.time.temporal.ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        endDate
                );


        return Math.max(
                days,
                0
        );
    }


    // =========================================================
    // TO STRING
    // =========================================================

    @Override
    public String toString() {

        return name
                + " ("
                + memberCode
                + ")";
    }
}