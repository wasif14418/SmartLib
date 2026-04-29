package com.smartlib.service;

import com.smartlib.model.Book;
import com.smartlib.model.Member;
import com.smartlib.model.PreBooking;
import com.smartlib.model.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

public class LibraryService {

    private static LibraryService instance;
    private final DatabaseManager dbManager;

    private LibraryService() {
        dbManager = new DatabaseManager();
    }

    public static LibraryService getInstance() {
        if (instance == null) {
            instance = new LibraryService();
        }
        return instance;
    }

    public ObservableList<Book> getBooks() {
        ObservableList<Book> books = FXCollections.observableArrayList();
        String sql =
                "SELECT b.id, b.title, b.author, b.isbn, b.totalCopies, b.availableCopies, " +
                        "       (SELECT COUNT(*) FROM Prebookings p " +
                        "        WHERE p.bookId = b.id AND (p.status = 'PENDING' OR p.status = 'CONFIRMED')) AS reservedCount " +
                        "FROM Books b";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int total = rs.getInt("totalCopies");
                int physicalAvailable = rs.getInt("availableCopies");
                int reserved = rs.getInt("reservedCount");

                // FIXED: Calculate display quantity by subtracting reservations
                int displayAvailable = physicalAvailable - reserved;
                if (displayAvailable < 0) displayAvailable = 0;

                books.add(new Book(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        total,
                        displayAvailable,
                        reserved
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public ObservableList<Transaction> getTransactions() {
        ObservableList<Transaction> transactions = FXCollections.observableArrayList();
        // Now reads batch and department from the Borrowings table
        String sql =
                "SELECT t.id AS transactionId, " +
                        "       m.name AS memberName, m.id AS memberId, " +
                        "       t.batch, t.department, " +
                        "       b.title AS bookTitle, " +
                        "       t.borrowDate, t.dueDate, t.returnDate " +
                        "FROM Borrowings t " +
                        "JOIN Books   b ON t.bookId   = b.id " +
                        "JOIN Members m ON t.memberId = m.id " +
                        "ORDER BY t.borrowDate DESC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {
                boolean isReturned = rs.getDate("returnDate") != null;
                String batch = rs.getString("batch");
                String dept  = rs.getString("department");
                transactions.add(new Transaction(
                        rs.getString("transactionId"),
                        rs.getString("memberName"),
                        rs.getString("memberId"),
                        batch != null ? batch : "N/A",
                        dept  != null ? dept  : "N/A",
                        rs.getString("bookTitle"),
                        rs.getDate("borrowDate").toLocalDate(),
                        rs.getDate("dueDate").toLocalDate(),
                        isReturned
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public ObservableList<PreBooking> getPreBookings() {
        ObservableList<PreBooking> preBookings = FXCollections.observableArrayList();
        String sql =
                "SELECT pb.id AS prebookId, " +
                        "       b.title AS bookTitle, " +
                        "       m.name AS memberName, m.id AS memberId, " +
                        "       pb.prebookDate, pb.status " +
                        "FROM Prebookings pb " +
                        "JOIN Books   b ON pb.bookId   = b.id " +
                        "JOIN Members m ON pb.memberId = m.id " +
                        "ORDER BY pb.prebookDate ASC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {
                preBookings.add(new PreBooking(
                        rs.getString("prebookId"),
                        rs.getString("memberName"),
                        rs.getString("memberId"),
                        rs.getString("bookTitle"),
                        rs.getDate("prebookDate").toLocalDate(),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return preBookings;
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public int totalBooks() {
        return queryInt("SELECT COALESCE(SUM(totalCopies), 0) FROM Books");
    }

    public int totalAvailable() {
        return queryInt("SELECT COALESCE(SUM(availableCopies), 0) FROM Books");
    }

    public int totalBorrowed() {
        return queryInt("SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL");
    }

    public int totalOverdue() {
        return queryInt("SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL AND dueDate < CURRENT_DATE()");
    }

    public int totalReserved() {
        return queryInt("SELECT COUNT(*) FROM Prebookings WHERE status = 'PENDING' OR status = 'CONFIRMED'");
    }

    public int dueToday() {
        return queryInt("SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL AND dueDate = CURRENT_DATE()");
    }

    public int activeMembers() {
        return queryInt("SELECT COUNT(*) FROM Members");
    }

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Register a new borrow. Creates the member record if they don't exist yet.
     * Batch and department are now persisted to the Borrowings table.
     */
    public void registerBorrow(String studentName, String studentId,
                               String batch, String dept,
                               Book book, LocalDate issueDate, LocalDate dueDate) {

        String memberId = getOrCreateMember(studentName, studentId);

        String insertSql =
                "INSERT INTO Borrowings (id, bookId, memberId, batch, department, borrowDate, dueDate, returnDate) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NULL)";
        String updateBookSql =
                "UPDATE Books SET availableCopies = availableCopies - 1 WHERE id = ? AND availableCopies > 0";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psBorrow = conn.prepareStatement(insertSql);
                 PreparedStatement psBook   = conn.prepareStatement(updateBookSql)) {

                psBorrow.setString(1, UUID.randomUUID().toString());
                psBorrow.setString(2, book.getId());
                psBorrow.setString(3, memberId);
                psBorrow.setString(4, batch);
                psBorrow.setString(5, dept);
                psBorrow.setDate(6, Date.valueOf(issueDate));
                psBorrow.setDate(7, Date.valueOf(dueDate));
                psBorrow.executeUpdate();

                psBook.setString(1, book.getId());
                int updated = psBook.executeUpdate();
                if (updated == 0) {
                    // No available copies — roll back to be safe
                    conn.rollback();
                    System.err.println("registerBorrow: no available copies for book " + book.getId());
                    return;
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mark a transaction as returned and increment the book's available count.
     */
    public void returnBook(Transaction txn) {
        String getBookIdSql   = "SELECT bookId FROM Borrowings WHERE id = ?";
        String markReturnedSql = "UPDATE Borrowings SET returnDate = CURRENT_DATE() WHERE id = ? AND returnDate IS NULL";
        String updateBookSql  = "UPDATE Books SET availableCopies = availableCopies + 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Step 1: find which book this transaction is for
                String bookId = null;
                try (PreparedStatement ps = conn.prepareStatement(getBookIdSql)) {
                    ps.setString(1, txn.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) bookId = rs.getString("bookId");
                    }
                }
                if (bookId == null) {
                    throw new SQLException("No borrowing found for transaction id: " + txn.getId());
                }

                // Step 2: mark as returned
                try (PreparedStatement ps = conn.prepareStatement(markReturnedSql)) {
                    ps.setString(1, txn.getId());
                    ps.executeUpdate();
                }

                // Step 3: restore available copy
                try (PreparedStatement ps = conn.prepareStatement(updateBookSql)) {
                    ps.setString(1, bookId);
                    ps.executeUpdate();
                }

                conn.commit();
                txn.setReturned(true); // Update in-memory state so the UI refreshes instantly
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a pre-booking. Creates the member record if they don't exist yet.
     */
    public void addPreBooking(String studentName, String studentId,
                              Book book, LocalDate prebookDate) {

        String memberId = getOrCreateMember(studentName, studentId);

        String insertSql =
                "INSERT INTO Prebookings (id, bookId, memberId, prebookDate, status) VALUES (?, ?, ?, ?, 'PENDING')";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, book.getId());
            ps.setString(3, memberId);
            ps.setDate(4, Date.valueOf(prebookDate));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a new book to the inventory.
     */
    public void addBook(Book book) {
        String sql =
                "INSERT INTO Books (id, title, author, isbn, totalCopies, availableCopies) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // Generate a fresh UUID if the caller passed a blank/manual ID
            String id = (book.getId() == null || book.getId().isBlank())
                    ? UUID.randomUUID().toString()
                    : book.getId();
            ps.setString(1, id);
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getIsbn());
            ps.setInt(5, book.getTotalQty());
            ps.setInt(6, book.getAvailable());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Explicitly add a member record.
     */
    public void addMember(Member member) {
        String sql = "INSERT INTO Members (id, name, contactInfo) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getId());
            ps.setString(2, member.getName());
            ps.setString(3, member.getContactInfo());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Looks up a member by name. If not found, inserts a new record and returns the new ID.
     * studentId is used as a secondary identifier stored in contactInfo.
     */
    private String getOrCreateMember(String name, String studentId) {
        String existing = getMemberIdByName(name);
        if (existing != null) return existing;

        String newId = UUID.randomUUID().toString();
        String sql = "INSERT INTO Members (id, name, contactInfo) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newId);
            ps.setString(2, name);
            ps.setString(3, studentId);   // store the actual student ID
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return newId;
    }

    private String getMemberIdByName(String name) {
        String sql = "SELECT id FROM Members WHERE name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Helper to run a COUNT/SUM query that returns a single integer. */
    private int queryInt(String sql) {
        try (Connection conn = dbManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}