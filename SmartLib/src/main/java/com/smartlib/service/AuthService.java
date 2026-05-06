package com.smartlib.service;

import java.sql.*;
import java.util.UUID;

/**
 * Handles user authentication and registration.
 * Stores users in the Users table with roles ADMIN or STUDENT.
 */
public class AuthService {

    private static AuthService instance;
    private final DatabaseManager dbManager;

    // Currently logged-in user info
    private String currentUserId;
    private String currentUsername;
    private String currentName;
    private String currentRole;   // "ADMIN" or "STUDENT"
    private String currentStudentId;

    private AuthService() {
        dbManager = new DatabaseManager();
    }

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Attempts login. Returns null on success, or an error message string.
     */
    public String login(String username, String password) {
        String sql = "SELECT id, username, name, role, studentId FROM Users WHERE username = ? AND password = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentUserId    = rs.getString("id");
                    currentUsername  = rs.getString("username");
                    currentName      = rs.getString("name");
                    currentRole      = rs.getString("role");
                    currentStudentId = rs.getString("studentId");
                    return null; // success
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
        return "Invalid username or password.";
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * Register a new user. Returns null on success, or an error message.
     * role must be "ADMIN" or "STUDENT".
     */
    public String register(String username, String password, String name,
                           String role, String studentId) {
        if (username.isBlank() || password.isBlank() || name.isBlank()) {
            return "Username, password and name are required.";
        }
        if (password.length() < 4) {
            return "Password must be at least 4 characters.";
        }

        // Check username uniqueness
        String check = "SELECT COUNT(*) FROM Users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "Username already taken. Please choose another.";
                }
            }
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }

        // Insert user
        String sql = "INSERT INTO Users (id, username, password, role, name, studentId) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, username.trim());
            ps.setString(3, password);
            ps.setString(4, role.toUpperCase());
            ps.setString(5, name.trim());
            if (studentId != null && !studentId.isBlank())
                ps.setString(6, studentId.trim());
            else
                ps.setNull(6, Types.VARCHAR);
            ps.executeUpdate();
            return null; // success
        } catch (SQLException e) {
            e.printStackTrace();
            return "Registration failed: " + e.getMessage();
        }
    }

    // ── Session ───────────────────────────────────────────────────────────────

    public void logout() {
        currentUserId    = null;
        currentUsername  = null;
        currentName      = null;
        currentRole      = null;
        currentStudentId = null;
    }

    public boolean isLoggedIn()  { return currentUserId != null; }
    public boolean isAdmin()     { return "ADMIN".equals(currentRole); }
    public boolean isStudent()   { return "STUDENT".equals(currentRole); }

    public String getCurrentUserId()    { return currentUserId; }
    public String getCurrentUsername()  { return currentUsername; }
    public String getCurrentName()      { return currentName; }
    public String getCurrentRole()      { return currentRole; }
    public String getCurrentStudentId() { return currentStudentId; }
}