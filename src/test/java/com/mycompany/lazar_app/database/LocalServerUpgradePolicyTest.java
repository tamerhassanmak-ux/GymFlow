package com.mycompany.lazar_app.database;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalServerUpgradePolicyTest {
    @Test void automaticUpgradeNeverDisablesTlsOrSwitchesEndpoints() {
        DatabaseConfig config = new DatabaseConfig(); config.setType(DatabaseType.MYSQL);
        config.setHost("localhost"); config.setEncryptConnection(true);
        SQLException tls = assertThrows(SQLException.class, () -> LocalServerUpgrade.prepare(null, config, c -> fail()));
        assertEquals("GF003", tls.getSQLState()); assertTrue(config.isEncryptConnection());
        config.setHost("remote.example"); config.setEncryptConnection(false);
        SQLException remote = assertThrows(SQLException.class, () -> LocalServerUpgrade.prepare(null, config, c -> fail()));
        assertEquals("GF003", remote.getSQLState()); assertEquals("remote.example",config.getHost());
    }
    @Test void rehearsalFailureShowsSpecificSafeConflictInsteadOfGenericBackupError() {
        SQLException conflict = new SQLException("secret-member-data", "GF006");
        SQLException wrapped = new SQLException("preparation failed", "GF004", conflict);
        String message = DatabaseExceptionHandler.message(wrapped);
        assertTrue(message.contains("أرقام هاتف مكررة")); assertFalse(message.contains("secret-member-data"));
        assertEquals(DatabaseExceptionHandler.Category.SCHEMA, DatabaseExceptionHandler.classify(wrapped));
    }
}
