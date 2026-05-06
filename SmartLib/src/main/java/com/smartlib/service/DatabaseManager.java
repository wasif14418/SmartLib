package com.smartlib.service;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Manages a PERSISTENT H2 file-based database.
 *
 * The database file is stored at:
 *   Windows:  C:\Users\<you>\smartlib_data\smartlib
 *   Mac/Linux: ~/smartlib_data/smartlib
 */
public class DatabaseManager {

    private static final String DB_URL;

    static {
        String home = System.getProperty("user.home");
        File dbDir = new File(home, "smartlib_data");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        DB_URL = "jdbc:h2:file:" + dbDir.getAbsolutePath().replace("\\", "/")
                + "/smartlib;DB_CLOSE_DELAY=-1;AUTO_SERVER=FALSE";
    }

    private static final String DB_USER     = "sa";
    private static final String DB_PASSWORD = "";

    public DatabaseManager() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 driver not found. Check your pom.xml dependency.", e);
        }
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            createTables(conn);
            insertSampleDataIfEmpty(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // Users table for authentication
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS Users (" +
                            "  id       VARCHAR(36)  PRIMARY KEY," +
                            "  username VARCHAR(100) UNIQUE NOT NULL," +
                            "  password VARCHAR(255) NOT NULL," +
                            "  role     VARCHAR(20)  NOT NULL," +    // 'ADMIN' or 'STUDENT'
                            "  name     VARCHAR(255) NOT NULL," +
                            "  studentId VARCHAR(100)" +             // only for students
                            ")"
            );

            // Books Table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS Books (" +
                            "  id             VARCHAR(36)  PRIMARY KEY," +
                            "  title          VARCHAR(255) NOT NULL," +
                            "  author         VARCHAR(255) NOT NULL," +
                            "  isbn           VARCHAR(20)  UNIQUE NOT NULL," +
                            "  totalCopies    INT          NOT NULL," +
                            "  availableCopies INT         NOT NULL" +
                            ")"
            );

            // Members Table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS Members (" +
                            "  id          VARCHAR(36)  PRIMARY KEY," +
                            "  name        VARCHAR(255) NOT NULL," +
                            "  contactInfo VARCHAR(255)" +
                            ")"
            );

            // Borrowings Table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS Borrowings (" +
                            "  id         VARCHAR(36) PRIMARY KEY," +
                            "  bookId     VARCHAR(36) NOT NULL," +
                            "  memberId   VARCHAR(36) NOT NULL," +
                            "  batch      VARCHAR(100)," +
                            "  department VARCHAR(100)," +
                            "  borrowDate DATE        NOT NULL," +
                            "  dueDate    DATE        NOT NULL," +
                            "  returnDate DATE," +
                            "  FOREIGN KEY (bookId)   REFERENCES Books(id)," +
                            "  FOREIGN KEY (memberId) REFERENCES Members(id)" +
                            ")"
            );

            // Prebookings Table — pickupDate is the user-chosen pickup date
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS Prebookings (" +
                            "  id          VARCHAR(36)  PRIMARY KEY," +
                            "  bookId      VARCHAR(36)  NOT NULL," +
                            "  memberId    VARCHAR(36)  NOT NULL," +
                            "  prebookDate DATE         NOT NULL," +
                            "  pickupDate  DATE," +
                            "  status      VARCHAR(50)  NOT NULL," +
                            "  FOREIGN KEY (bookId)   REFERENCES Books(id)," +
                            "  FOREIGN KEY (memberId) REFERENCES Members(id)" +
                            ")"
            );

            // Migrate: add pickupDate column if upgrading from old schema
            try {
                stmt.execute("ALTER TABLE Prebookings ADD COLUMN IF NOT EXISTS pickupDate DATE");
            } catch (Exception ignored) {}
        }
    }

    private void insertSampleDataIfEmpty(Connection conn) throws SQLException {
        // Always ensure default admin exists
        ensureDefaultAdmin(conn);

        if (hasData(conn, "Books")) {
            System.out.println("Database already has data — skipping sample insertion.");
            return;
        }

        System.out.println("First run detected — inserting sample data...");

        String bookSql = "INSERT INTO Books (id, title, author, isbn, totalCopies, availableCopies) VALUES (?, ?, ?, ?, ?, ?)";
        String[] bookIds = new String[6];
        for (int i = 0; i < 6; i++) bookIds[i] = UUID.randomUUID().toString();

        try (PreparedStatement ps = conn.prepareStatement(bookSql)) {
            Object[][] books = {
                    {bookIds[0], "Data Structures & Algorithms",    "Thomas Cormen",    "978-0262033848", 5, 3},
                    {bookIds[1], "Operating Systems",               "Andrew Tanenbaum", "978-0133591620", 4, 2},
                    {bookIds[2], "Java: The Complete Reference",    "Herbert Schildt",  "978-1260440249", 6, 5},
                    {bookIds[3], "Database Systems",                "Ramez Elmasri",    "978-0133970777", 3, 3},
                    {bookIds[4], "Computer Networks",               "Andrew Tanenbaum", "978-0132126953", 4, 4},
                    {bookIds[5], "Discrete Mathematics",            "Kenneth Rosen",    "978-0073383095", 3, 2},
            };
            for (Object[] row : books) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, (String) row[3]);
                ps.setInt(5,    (Integer) row[4]);
                ps.setInt(6,    (Integer) row[5]);
                ps.executeUpdate();
            }
        }

        String memberSql = "INSERT INTO Members (id, name, contactInfo) VALUES (?, ?, ?)";
        String memberId1 = UUID.randomUUID().toString();
        String memberId2 = UUID.randomUUID().toString();
        String memberId3 = UUID.randomUUID().toString();

        try (PreparedStatement ps = conn.prepareStatement(memberSql)) {
            ps.setString(1, memberId1); ps.setString(2, "Rafiur Rahman");  ps.setString(3, "rafiur@example.com");  ps.executeUpdate();
            ps.setString(1, memberId2); ps.setString(2, "Sheak Islam");    ps.setString(3, "sheak@example.com");    ps.executeUpdate();
            ps.setString(1, memberId3); ps.setString(2, "Nadia Hossain");  ps.setString(3, "nadia@example.com");    ps.executeUpdate();
        }

        String borrowSql = "INSERT INTO Borrowings (id, bookId, memberId, batch, department, borrowDate, dueDate, returnDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(borrowSql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, bookIds[0]);
            ps.setString(3, memberId1);
            ps.setString(4, "Batch 14");
            ps.setString(5, "CSE");
            ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(9)));
            ps.setDate(7, Date.valueOf(LocalDate.now().plusDays(5)));
            ps.setNull(8, Types.DATE);
            ps.executeUpdate();

            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, bookIds[1]);
            ps.setString(3, memberId2);
            ps.setString(4, "Batch 13");
            ps.setString(5, "EEE");
            ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(15)));
            ps.setDate(7, Date.valueOf(LocalDate.now().minusDays(1)));
            ps.setNull(8, Types.DATE);
            ps.executeUpdate();

            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, bookIds[2]);
            ps.setString(3, memberId3);
            ps.setString(4, "Batch 14");
            ps.setString(5, "CSE");
            ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(20)));
            ps.setDate(7, Date.valueOf(LocalDate.now().minusDays(6)));
            ps.setDate(8, Date.valueOf(LocalDate.now().minusDays(8)));
            ps.executeUpdate();
        }

        String prebookSql = "INSERT INTO Prebookings (id, bookId, memberId, prebookDate, pickupDate, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(prebookSql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, bookIds[3]);
            ps.setString(3, memberId1);
            ps.setDate(4, Date.valueOf(LocalDate.now()));
            ps.setDate(5, Date.valueOf(LocalDate.now().plusDays(4)));
            ps.setString(6, "PENDING");
            ps.executeUpdate();
        }

        System.out.println("Sample data inserted successfully.");
    }

    private void ensureDefaultAdmin(Connection conn) throws SQLException {
        String check = "SELECT COUNT(*) FROM Users WHERE role = 'ADMIN'";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(check)) {
            if (rs.next() && rs.getInt(1) > 0) return; // admin already exists
        }
        String sql = "INSERT INTO Users (id, username, password, role, name, studentId) VALUES (?, ?, ?, ?, ?, NULL)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, "admin");
            ps.setString(3, "admin123");  // plain text for simplicity
            ps.setString(4, "ADMIN");
            ps.setString(5, "Library Admin");
            ps.executeUpdate();
            System.out.println("Default admin created: username=admin, password=admin123");
        }
    }

    private boolean hasData(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}