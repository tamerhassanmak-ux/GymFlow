package com.mycompany.lazar_app.database;

import java.sql.SQLException;
import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/** Diagnostics deliberately exclude vendor messages, URLs, identities and parameter values. */
public final class DatabaseExceptionHandler {
    public enum Category { CONFIGURATION, DRIVER, AUTHENTICATION, PERMISSION, TLS, TIMEOUT, NETWORK, SCHEMA, UNKNOWN }
    private DatabaseExceptionHandler() {}
    public static Category classify(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof SSLException || t instanceof CertificateException) return Category.TLS;
        }
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof IllegalArgumentException) return Category.CONFIGURATION;
            if (t instanceof SQLException sql) {
                String state = sql.getSQLState() == null ? "" : sql.getSQLState();
                if (state.startsWith("IM") || t instanceof java.sql.SQLNonTransientConnectionException && state.equals("08001") && sql.getErrorCode() == 0
                        && sql.getMessage() != null && sql.getMessage().startsWith("No suitable driver")) return Category.DRIVER;
                if (state.startsWith("28")) return Category.AUTHENTICATION;
                if (state.equals("42000") && (sql.getErrorCode() == 1044 || sql.getErrorCode() == 1142)) return Category.PERMISSION;
                if (t instanceof java.sql.SQLTimeoutException || state.startsWith("HYT")) return Category.TIMEOUT;
                if (state.startsWith("08")) return Category.NETWORK;
                if (state.startsWith("42") || state.equals("GF001") || state.equals("GF002")) return Category.SCHEMA;
            }
            if (t instanceof java.net.SocketTimeoutException) return Category.TIMEOUT;
            if (t instanceof java.net.UnknownHostException || t instanceof java.net.ConnectException) return Category.NETWORK;
        }
        return Category.UNKNOWN;
    }
    public static String diagnostic(Throwable failure) {
        StringBuilder result = new StringBuilder(classify(failure).name());
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                String state = sql.getSQLState();
                result.append(" SQLState=").append(state != null && state.matches("[A-Za-z0-9]{5}") ? state : "unknown")
                        .append(" code=").append(sql.getErrorCode());
                break;
            }
        }
        return result.toString();
    }
    public static String message(Throwable failure) {
        for (Throwable t=failure;t!=null;t=t.getCause()) {
            if (t instanceof SQLException sql && "GF001".equals(sql.getSQLState()))
                return "توقفت الترقية: يلزم مسار نسخ واستعادة معتمد للخادم. راجع دليل الترقية. / Upgrade stopped: a validated server backup/recovery workflow is required. See UPGRADE_GUIDE.md.";
        }
        return switch (classify(failure)) {
            case TLS -> "تعذر التحقق من شهادة الخادم. راجع الصلاحية والاسم والجهة الموثوقة. / TLS verification failed: check CA, expiry and hostname.";
            case AUTHENTICATION -> "فشل تسجيل الدخول لقاعدة البيانات. / Database authentication failed. Check credentials.";
            case PERMISSION -> "صلاحيات قاعدة البيانات غير كافية. / Insufficient database permissions.";
            case DRIVER -> "برنامج تشغيل قاعدة البيانات غير متاح. أعد تثبيت التطبيق. / JDBC driver unavailable; reinstall the application.";
            case CONFIGURATION -> "إعدادات قاعدة البيانات غير صالحة. راجع الحقول ووضع TLS. / Invalid database configuration; check fields and TLS mode.";
            case TIMEOUT, NETWORK -> "تعذر الوصول إلى الخادم. راجع الشبكة والعنوان والمنفذ. / Server unreachable; check service, host, port and network.";
            case SCHEMA -> "تعذر التحقق من مخطط قاعدة البيانات. راجع النسخة الاحتياطية والترحيلات. / Schema check failed; review backup and migrations.";
            default -> "تعذر تجهيز قاعدة البيانات. راجع التشخيص ودليل الاستعادة. / Database readiness failed; review diagnostics and recovery guide.";
        };
    }
}
