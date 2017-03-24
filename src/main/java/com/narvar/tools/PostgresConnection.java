package com.narvar.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PostgresConnection {

    public static String APPLICATION_PROPERTIES;
    private static final String DB_DRIVER_NAME;
    private static final String DB_URL;
    private static final String DB_USERNAME;
    private static final String DB_PASSWORD;
    private static FileInputStream input;
    private static String url = null;
    private static String username = null;
    private static String dbpassword = null;

    static {
        APPLICATION_PROPERTIES = "/etc/narvar/application.properties";
        DB_DRIVER_NAME = "postgres_datasource.driverClassName";
        DB_URL = "postgres_datasource.url";
        DB_USERNAME = "postgres_datasource.username";
        DB_PASSWORD = "postgres_datasource.password";
    }

    public static Connection getPostgresConnection() {

        Connection connection = null;
        try {

            Properties prop = new Properties();
            input = new FileInputStream(APPLICATION_PROPERTIES);
            prop.load(input);
            prop.getProperty(DB_DRIVER_NAME);
            url = prop.getProperty(DB_URL);
            username = prop.getProperty(DB_USERNAME);
            dbpassword = prop.getProperty(DB_PASSWORD);

            /*connection = DriverManager.getConnection(
					"jdbc:postgresql://localhost:5432/tracking", "narvar",
					"narvar12345");
             */
            connection = DriverManager.getConnection(
                    url, username, dbpassword);

        } catch (SQLException e) {
            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return null;
        } catch (FileNotFoundException e) {
            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return null;
        }
        return connection;
    }
}
