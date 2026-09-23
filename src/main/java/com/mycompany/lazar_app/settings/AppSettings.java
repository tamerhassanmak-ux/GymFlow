package com.mycompany.lazar_app.settings;

public class AppSettings {

    private String gymName = "GymFlow";
    private String phone = "";
    private String email = "";
    private String address = "";

    private String language = "English";
    private String currency = "EGP";
    private String dateFormat = "DD/MM/YYYY";
    private String timeZone = "Africa/Cairo";

    private String defaultMembership = "Monthly";
    private int gracePeriodDays = 0;
    private boolean autoExpireMemberships = true;
    private boolean renewBeforeExpiry = true;

    private boolean expiryAlerts = true;
    private boolean paymentReminders = true;
    private boolean newMemberNotification = true;
    private boolean expenseNotifications = true;
    private int expiryWarningDays = 3;

    private boolean darkMode = false;
    private String lastBackup = "";

    public String getGymName() { return gymName; }
    public void setGymName(String v) { gymName = safe(v); }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = safe(v); }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = safe(v); }
    public String getAddress() { return address; }
    public void setAddress(String v) { address = safe(v); }

    public String getLanguage() { return language; }
    public void setLanguage(String v) { language = safe(v); }
    public String getCurrency() { return currency; }
    public void setCurrency(String v) { currency = safe(v); }
    public String getDateFormat() { return dateFormat; }
    public void setDateFormat(String v) { dateFormat = safe(v); }
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String v) { timeZone = safe(v); }

    public String getDefaultMembership() { return defaultMembership; }
    public void setDefaultMembership(String v) { defaultMembership = safe(v); }
    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int v) { gracePeriodDays = Math.max(0, v); }
    public boolean isAutoExpireMemberships() { return autoExpireMemberships; }
    public void setAutoExpireMemberships(boolean v) { autoExpireMemberships = v; }
    public boolean isRenewBeforeExpiry() { return renewBeforeExpiry; }
    public void setRenewBeforeExpiry(boolean v) { renewBeforeExpiry = v; }

    public boolean isExpiryAlerts() { return expiryAlerts; }
    public void setExpiryAlerts(boolean v) { expiryAlerts = v; }
    public boolean isPaymentReminders() { return paymentReminders; }
    public void setPaymentReminders(boolean v) { paymentReminders = v; }
    public boolean isNewMemberNotification() { return newMemberNotification; }
    public void setNewMemberNotification(boolean v) { newMemberNotification = v; }
    public boolean isExpenseNotifications() { return expenseNotifications; }
    public void setExpenseNotifications(boolean v) { expenseNotifications = v; }
    public int getExpiryWarningDays() { return expiryWarningDays; }
    public void setExpiryWarningDays(int v) { expiryWarningDays = Math.max(0, v); }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean v) { darkMode = v; }
    public String getLastBackup() { return lastBackup; }
    public void setLastBackup(String v) { lastBackup = safe(v); }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
