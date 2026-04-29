package com.smartlib.service;

import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:h2:mem:smartlib;DB_CLOSE_DELAY=-1"; // In-memory H2 database
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public DatabaseManager() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            createTables(conn);
            insertSampleData(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();

        // Books Table
        String createBooksTable = "CREATE TABLE IF NOT EXISTS Books (" +
                                  "id VARCHAR(36) PRIMARY KEY," +
                                  "title VARCHAR(255) NOT NULL," +
                                  "author VARCHAR(255) NOT NULL," +
                                  "isbn VARCHAR(20) UNIQUE NOT NULL," +
                                  "totalCopies INT NOT NULL," +
                                  "availableCopies INT NOT NULL" +
                                  ");";
        stmt.execute(createBooksTable);

        // Members Table
        String createMembersTable = "CREATE TABLE IF NOT EXISTS Members (" +
                                    "id VARCHAR(36) PRIMARY KEY," +
                                    "name VARCHAR(255) NOT NULL," +
                                    "contactInfo VARCHAR(255) UNIQUE NOT NULL" +
                                    ");";
        stmt.execute(createMembersTable);

        // Borrowings Table
        String createBorrowingsTable = "CREATE TABLE IF NOT EXISTS Borrowings (" +
                                       "id VARCHAR(36) PRIMARY KEY," +
                                       "bookId VARCHAR(36) NOT NULL," +
                                       "memberId VARCHAR(36) NOT NULL," +
                                       "borrowDate DATE NOT NULL," +
                                       "dueDate DATE NOT NULL," +
                                       "returnDate DATE," + // Nullable
                                       "FOREIGN KEY (bookId) REFERENCES Books(id)," +
                                       "FOREIGN KEY (memberId) REFERENCES Members(id)" +
                                       ");";
        stmt.execute(createBorrowingsTable);

        // Prebookings Table
        String createPrebookingsTable = "CREATE TABLE IF NOT EXISTS Prebookings (" +
                                        "id VARCHAR(36) PRIMARY KEY," +
                                        "bookId VARCHAR(36) NOT NULL," +
                                        "memberId VARCHAR(36) NOT NULL," +
                                        "prebookDate DATE NOT NULL," +
                                        "status VARCHAR(50) NOT NULL," + // e.g., PENDING, READY, CANCELLED
                                        "FOREIGN KEY (bookId) REFERENCES Books(id)," +
                                        "FOREIGN KEY (memberId) REFERENCES Members(id)" +
                                        ");";
        stmt.execute(createPrebookingsTable);

        stmt.close();
    }

    private void insertSampleData(Connection conn) throws SQLException {
        // Check if data already exists to prevent duplicates on re-initialization
        if (hasData(conn, "Books") || hasData(conn, "Members")) {
            System.out.println("Sample data already exists. Skipping insertion.");
            return;
        }

        System.out.println("Inserting sample data...");
        // Insert Sample Books
        String insertBookSql = "INSERT INTO Books (id, title, author, isbn, totalCopies, availableCopies) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertBookSql)) {
            String book1Id = UUID.randomUUID().toString();
            pstmt.setString(1, book1Id);
            pstmt.setString(2, "The Great Adventure");
            pstmt.setString(3, "Alice Wonderland");
            pstmt.setString(4, "978-0321765723");
            pstmt.setInt(5, 5);
            pstmt.setInt(6, 3); // 2 borrowed
            pstmt.executeUpdate();

            String book2Id = UUID.randomUUID().toString();
            pstmt.setString(1, book2Id);
            pstmt.setString(2, "Mystery of the Old House");
            pstmt.setString(3, "Bob The Builder");
            pstmt.setString(4, "978-1234567890");
            pstmt.setInt(5, 3);
            pstmt.setInt(6, 2); // 1 borrowed
            pstmt.executeUpdate();

            String book3Id = UUID.randomUUID().toString();
            pstmt.setString(1, book3Id);
            pstmt.setString(2, "Coding for Dummies");
            pstmt.setString(3, "Charlie Chaplin");
            pstmt.setString(4, "978-9876543210");
            pstmt.setInt(5, 2);
            pstmt.setInt(6, 2); // 0 borrowed
            pstmt.executeUpdate();
        }

        // Insert Sample Members
        String insertMemberSql = "INSERT INTO Members (id, name, contactInfo) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertMemberSql)) {
            String member1Id = UUID.randomUUID().toString();
            pstmt.setString(1, member1Id);
            pstmt.setString(2, "John Doe");
            pstmt.setString(3, "john.doe@example.com");
            pstmt.executeUpdate();

            String member2Id = UUID.randomUUID().toString();
            pstmt.setString(1, member2Id);
            pstmt.setString(2, "Jane Smith");
            pstmt.setString(3, "jane.smith@example.com");
            pstmt.executeUpdate();
        }

        // Insert Sample Borrowings (assuming book1 and member1, book2 and member2)
        String insertBorrowingSql = "INSERT INTO Borrowings (id, bookId, memberId, borrowDate, dueDate, returnDate) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertBorrowingSql)) {
            // John Doe borrowed "The Great Adventure"
            String book1Id = getBookIdByTitle(conn, "The Great Adventure");
            String member1Id = getMemberIdByName(conn, "John Doe");
            if (book1Id != null && member1Id != null) {
                pstmt.setString(1, UUID.randomUUID().toString());
                pstmt.setString(2, book1Id);
                pstmt.setString(3, member1Id);
                pstmt.setDate(4, Date.valueOf(LocalDate.now().minusDays(10)));
                pstmt.setDate(5, Date.valueOf(LocalDate.now().plusDays(5)));
                pstmt.setNull(6, Types.DATE); // Not returned yet
                pstmt.executeUpdate();
            }

            // Jane Smith borrowed "Mystery of the Old House"
            String book2Id = getBookIdByTitle(conn, "Mystery of the Old House");
            String member2Id = getMemberIdByName(conn, "Jane Smith");
            if (book2Id != null && member2Id != null) {
                pstmt.setString(1, UUID.randomUUID().toString());
                pstmt.setString(2, book2Id);
                pstmt.setString(3, member2Id);
                pstmt.setDate(4, Date.valueOf(LocalDate.now().minusDays(15)));
                pstmt.setDate(5, Date.valueOf(LocalDate.now().minusDays(1))); // Overdue
                pstmt.setNull(6, Types.DATE); // Not returned yet
                pstmt.executeUpdate();
            }
        }

        // Insert Sample Prebookings (assuming book1 and member2)
        String insertPrebookingSql = "INSERT INTO Prebookings (id, bookId, memberId, prebookDate, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertPrebookingSql)) {
            String book1Id = getBookIdByTitle(conn, "The Great Adventure");
            String member2Id = getMemberIdByName(conn, "Jane Smith");
            if (book1Id != null && member2Id != null) {
                pstmt.setString(1, UUID.randomUUID().toString());
                pstmt.setString(2, book1Id);
                pstmt.setString(3, member2Id);
                pstmt.setDate(4, Date.valueOf(LocalDate.now().plusDays(2)));
                pstmt.setString(5, "PENDING");
                pstmt.executeUpdate();
            }
        }
    }

    private boolean hasData(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // Helper methods to get IDs for foreign key constraints
    private String getBookIdByTitle(Connection conn, String title) throws SQLException {
        String sql = "SELECT id FROM Books WHERE title = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("id");
            }
        }
        return null;
    }

    private String getMemberIdByName(Connection conn, String name) throws SQLException {
        String sql = "SELECT id FROM Members WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("id");
            }
        }
        return null;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
