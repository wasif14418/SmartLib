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
 *
 * No installation required — H2 is bundled in the app via Maven.
 * Data is preserved between app restarts.
 */
public class DatabaseManager {

    // ── Persistent file-based URL (changed from mem: to file:) ───────────────
    private static final String DB_URL;

    static {
        // Store the database in the user's home directory under "smartlib_data"
        String home = System.getProperty("user.home");
        File dbDir = new File(home, "smartlib_data");
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        // DB_CLOSE_DELAY=-1 keeps the connection pool alive while the app is running
        // AUTO_SERVER=FALSE is fine for single-user desktop apps
        DB_URL = "jdbc:h2:file:" + dbDir.getAbsolutePath().replace("\\", "/")
                + "/smartlib;DB_CLOSE_DELAY=-1;AUTO_SERVER=FALSE";
    }

    private static final String DB_USER     = "sa";
    private static final String DB_PASSWORD = "";

    public DatabaseManager() {
        // Register H2 driver explicitly (needed when bundled in fat JAR)
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 driver not found. Check your pom.xml dependency.", e);
        }
        initializeDatabase();
    }

    // ── Schema creation ───────────────────────────────────────────────────────

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
            // contactInfo is made nullable so auto-created members don't conflict on UNIQUE
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS Members (" +
                            "  id          VARCHAR(36)  PRIMARY KEY," +
                            "  name        VARCHAR(255) NOT NULL," +
                            "  contactInfo VARCHAR(255)" +
                            ")"
            );

            // Borrowings Table — added batch and department columns
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

            // Prebookings Table
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS Prebookings (" +
                            "  id          VARCHAR(36)  PRIMARY KEY," +
                            "  bookId      VARCHAR(36)  NOT NULL," +
                            "  memberId    VARCHAR(36)  NOT NULL," +
                            "  prebookDate DATE         NOT NULL," +
                            "  status      VARCHAR(50)  NOT NULL," +
                            "  FOREIGN KEY (bookId)   REFERENCES Books(id)," +
                            "  FOREIGN KEY (memberId) REFERENCES Members(id)" +
                            ")"
            );
        }
    }

    // ── Sample data (only inserted once, on first run) ────────────────────────

    private void insertSampleDataIfEmpty(Connection conn) throws SQLException {
        if (hasData(conn, "Books")) {
            System.out.println("Database already has data — skipping sample insertion.");
            return;
        }

        System.out.println("First run detected — inserting sample data...");

        // ── Sample books ──────────────────────────────────────────────────────
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
                ps.setInt(5, (Integer) row[4]);
                ps.setInt(6, (Integer) row[5]);
                ps.executeUpdate();
            }
        }

        // ── Sample members ────────────────────────────────────────────────────
        String memberSql = "INSERT INTO Members (id, name, contactInfo) VALUES (?, ?, ?)";
        String memberId1 = UUID.randomUUID().toString();
        String memberId2 = UUID.randomUUID().toString();
        String memberId3 = UUID.randomUUID().toString();

        try (PreparedStatement ps = conn.prepareStatement(memberSql)) {
            ps.setString(1, memberId1); ps.setString(2, "Rafiur Rahman");  ps.setString(3, "rafiur@example.com");  ps.executeUpdate();
            ps.setString(1, memberId2); ps.setString(2, "Sheak Islam");    ps.setString(3, "sheak@example.com");    ps.executeUpdate();
            ps.setString(1, memberId3); ps.setString(2, "Nadia Hossain");  ps.setString(3, "nadia@example.com");    ps.executeUpdate();
        }

        // ── Sample borrowings ─────────────────────────────────────────────────
        String borrowSql = "INSERT INTO Borrowings (id, bookId, memberId, batch, department, borrowDate, dueDate, returnDate) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(borrowSql)) {
            // Active borrow — due in 5 days
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, bookIds[0]);
            ps.setString(3, memberId1);
            ps.setString(4, "Batch 14");
            ps.setString(5, "CSE");
            ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(9)));
            ps.setDate(7, Date.valueOf(LocalDate.now().plusDays(5)));
            ps.setNull(8, Types.DATE);
            ps.executeUpdate();

            // Overdue borrow — 1 day overdue
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, bookIds[1]);
            ps.setString(3, memberId2);
            ps.setString(4, "Batch 13");
            ps.setString(5, "EEE");
            ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(15)));
            ps.setDate(7, Date.valueOf(LocalDate.now().minusDays(1)));
            ps.setNull(8, Types.DATE);
            ps.executeUpdate();

            // Returned borrow
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, bookIds[2]);
            ps.setString(3, memberId3);
            ps.setString(4, "Batch 14");
            ps.setString(5, "CSE");
            ps.setDate(6, Date.valueOf(LocalDate.now().minusDays(20)));
            ps.setDate(7, Date.valueOf(LocalDate.now().minusDays(6)));
            ps.setDate(8, Date.valueOf(LocalDate.now().minusDays(8))); // returned early
            ps.executeUpdate();
        }

        // ── Sample prebooking ─────────────────────────────────────────────────
        String prebookSql = "INSERT INTO Prebookings (id, bookId, memberId, prebookDate, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(prebookSql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, bookIds[3]);
            ps.setString(3, memberId1);
            ps.setDate(4, Date.valueOf(LocalDate.now().plusDays(4)));
            ps.setString(5, "PENDING");
            ps.executeUpdate();
        }

        System.out.println("Sample data inserted successfully.");
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean hasData(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Returns a new JDBC connection to the persistent H2 file database. */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}