package com.autoparts.auth;
import com.autoparts.db.DBConnection;
import com.autoparts.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

//Handles user login and password hashing
public class AuthService {

    public static User login(String username, String plainPassword) {
        String hashed = sha256(plainPassword);
        if (hashed == null) return null;

        String sql = "SELECT user_id, username, role, full_name, email "
                   + "FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    User.Role.valueOf(rs.getString("role")),
                    rs.getString("full_name"),
                    rs.getString("email")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // SHA-256 hex digest
    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
