package com.mycompany.lazar_app.database;

import com.mycompany.lazar_app.util.AppPaths;

public class DatabaseConfig {

    private DatabaseType type = DatabaseType.SQLITE;
    private String sqlitePath = AppPaths.databaseFile().toString();
    private String host = "localhost";
    private int port = 3306;
    private String databaseName = "lazar_gym";
    private String username = "";
    private String password = "";
    private boolean encryptConnection = false;

    public DatabaseType getType() { return type; }
    public void setType(DatabaseType type) { this.type = type; }

    public String getSqlitePath() { return sqlitePath; }
    public void setSqlitePath(String sqlitePath) { this.sqlitePath = sqlitePath; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isEncryptConnection() { return encryptConnection; }
    public void setEncryptConnection(boolean encryptConnection) {
        this.encryptConnection = encryptConnection;
    }
}
