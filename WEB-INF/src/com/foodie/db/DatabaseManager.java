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
                if (is != null) {
                    props.load(is);
                }
            }

            String envUrl = System.getenv("DB_URL");
            String envUsername = System.getenv("DB_USERNAME");
            String envPassword = System.getenv("DB_PASSWORD");

            url = (envUrl != null && !envUrl.isBlank()) ? envUrl : props.getProperty("db.url");
            username = (envUsername != null && !envUsername.isBlank()) ? envUsername : props.getProperty("db.username");
            password = (envPassword != null && !envPassword.isBlank()) ? envPassword : props.getProperty("db.password");

            if (url != null && !url.isBlank() && !url.contains("YOUR_ENDPOINT_HOST") && !url.contains("YOUR_DATABASE_URL")) {
                ready = true;
                LOGGER.info("DatabaseManager initialised and ready.");
            } else {
                ready = false;
                LOGGER.warning("DatabaseManager is not ready because database configuration is missing or still uses placeholder values.");
            }
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
