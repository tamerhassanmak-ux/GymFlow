package com.mycompany.lazar_app;

import com.mycompany.lazar_app.settings.AppSettings;
import com.mycompany.lazar_app.settings.AppSettingsManager;

import java.util.Locale;
import java.util.ResourceBundle;

public final class LanguageManager {

    private static Locale currentLocale = loadInitialLocale();

    private LanguageManager() {}

    private static Locale loadInitialLocale() {
        try {
            AppSettings settings = AppSettingsManager.load();
            return "العربية".equals(settings.getLanguage())
                    || "Arabic".equalsIgnoreCase(settings.getLanguage())
                    ? Locale.forLanguageTag("ar")
                    : Locale.ENGLISH;
        } catch (Exception e) {
            return Locale.ENGLISH;
        }
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("i18n.messages", currentLocale);
    }

    public static void setEnglish() {
        currentLocale = Locale.ENGLISH;
    }

    public static void setArabic() {
        currentLocale = Locale.forLanguageTag("ar");
    }

    public static boolean isArabic() {
        return "ar".equals(currentLocale.getLanguage());
    }

    public static Locale getCurrentLocale() {
        return currentLocale;
    }

    public static void toggleLanguage() {
        if (isArabic()) setEnglish();
        else setArabic();
    }
}
