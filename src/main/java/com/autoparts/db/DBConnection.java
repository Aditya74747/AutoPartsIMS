package com.autoparts.db;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

//Database connection - JDBC
public class DBConnection {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/autoparts_ims"
                                       + "?useSSL=false&allowPublicKeyRetrieval=true"
                                       + "&serverTimezone=Europe/Berlin";
    private static final String USER = "yourusername";
    private static final String PASS = "your_password_here"; // Change as needed

    public static Connection openConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found. "
                + "Add mysql-connector-j-*.jar to the classpath.", e);
        }
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    // Connections are closed individually by each DAO method
    public static void close() { }
}
