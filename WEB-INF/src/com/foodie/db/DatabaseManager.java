package com.foodie.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private static String url;
    private static String username;
    private static String password;
    private static boolean ready = false;

    static {
        try {
            Class.forName("org.postgresql.Driver");

            Properties props = new Properties();
            try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is == null) {
                    throw new IOException("db.properties not found on the classpath.");
                }
                props.load(is);
            }

            url = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");

            if (url == null || url.isEmpty() || url.contains("YOUR_ENDPOINT_HOST")) {
                throw new IllegalStateException("db.properties is not configured correctly.");
            }

            ready = true;
            LOGGER.info("DatabaseManager initialised and ready.");
        } catch (Exception e) {
            ready = false;
            LOGGER.log(Level.SEVERE, "DatabaseManager initialisation failed", e);
        }
    }

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        if (!ready) {
            throw new SQLException("DatabaseManager is not ready. Check db.properties and logs.");
        }
        return DriverManager.getConnection(url, username, password);
    }
}
