package com.aichatbot.util;

import com.aichatbot.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Creates JDBC connections. Settings are read, in order, from:
 * environment variable (DB_URL / DB_USER / DB_PASSWORD) -> Java system property
 * (db.url / db.user / db.password) -> db.properties on the classpath.
 */
public final class DBConnection {

    private static final Properties FILE_PROPS = loadFileProperties();

    private DBConnection() {
    }

    private static Properties loadFileProperties() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new DatabaseException("Could not read db.properties", e);
        }
        return props;
    }

    private static String setting(String envName, String propKey) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propKey);
        }
        if (value == null || value.isBlank()) {
            value = FILE_PROPS.getProperty(propKey);
        }
        return value == null ? "" : value;
    }

    /** Caller must close the connection (use try-with-resources). */
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(
                    setting("DB_URL", "db.url"),
                    setting("DB_USER", "db.user"),
                    setting("DB_PASSWORD", "db.password"));
        } catch (ClassNotFoundException | SQLException e) {
            throw new DatabaseException("Could not connect to the database", e);
        }
    }
}
