package com.mycompany.lazar_app.database.migration;

import java.sql.Connection;

public interface Migration {

    String version();

    String description();

    void up(Connection connection) throws Exception;
}
