package com.mycompany.lazar_app.database;

public enum DatabaseType {
    SQLITE("SQLite"),
    MYSQL("MySQL"),
    SQL_SERVER("SQL Server");

    private final String displayName;

    DatabaseType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
