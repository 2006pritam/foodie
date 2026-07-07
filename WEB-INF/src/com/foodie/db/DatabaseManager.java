package com.foodie.db;

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

            // Prefer environment variables (Railway / any host); fall back to the
            // bundled db.properties for local development.
            Properties props = new Properties();
            try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (is != null) props.load(is);
            }

            url      = firstNonBlank(System.getenv("DB_URL"),      props.getProperty("db.url"));
            username = firstNonBlank(System.getenv("DB_USERNAME"), props.getProperty("db.username"));
            password = firstNonBlank(System.getenv("DB_PASSWORD"), props.getProperty("db.password"));

            if (url == null || url.isEmpty() || url.contains("YOUR_ENDPOINT_HOST")) {
                throw new IllegalStateException(
                    "Database URL is not configured. Set DB_URL / DB_USERNAME / DB_PASSWORD env vars or db.properties.");
            }

            ready = true;
            LOGGER.info("DatabaseManager initialised and ready.");
        } catch (Exception e) {
            ready = false;
            LOGGER.log(Level.SEVERE, "DatabaseManager initialisation failed", e);
        }
    }

    /** Returns the first argument that is non-null and non-blank, else null. */
    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return null;
    }

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        if (!ready) {
            throw new SQLException("DatabaseManager is not ready. Check db.properties and logs.");
        }
        return DriverManager.getConnection(url, username, password);
    }
}
