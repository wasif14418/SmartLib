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

    // ── Book queries ──────────────────────────────────────────────────────────

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
                int total            = rs.getInt("totalCopies");
                int physicalAvailable = rs.getInt("availableCopies");
                int reserved         = rs.getInt("reservedCount");

                // Display available = physical minus pending pre-bookings (already deducted on prebook)
                // We keep physicalAvailable as the true value from DB (decremented on prebook)
                books.add(new Book(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        total,
                        physicalAvailable,
                        reserved
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    // ── Transaction queries ───────────────────────────────────────────────────

    public ObservableList<Transaction> getTransactions() {
        ObservableList<Transaction> transactions = FXCollections.observableArrayList();
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

    /**
     * Returns only the active (not-returned) borrowings for a specific member name.
     * Used in the Student panel.
     */
    public ObservableList<Transaction> getTransactionsForMember(String memberName) {
        ObservableList<Transaction> transactions = FXCollections.observableArrayList();
        String sql =
                "SELECT t.id AS transactionId, " +
                        "       m.name AS memberName, m.id AS memberId, " +
                        "       t.batch, t.department, " +
                        "       b.title AS bookTitle, " +
                        "       t.borrowDate, t.dueDate, t.returnDate " +
                        "FROM Borrowings t " +
                        "JOIN Books   b ON t.bookId   = b.id " +
                        "JOIN Members m ON t.memberId = m.id " +
                        "WHERE m.name = ? " +
                        "ORDER BY t.borrowDate DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberName);
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    // ── PreBooking queries ────────────────────────────────────────────────────

    public ObservableList<PreBooking> getPreBookings() {
        ObservableList<PreBooking> preBookings = FXCollections.observableArrayList();
        String sql =
                "SELECT pb.id AS prebookId, " +
                        "       b.title AS bookTitle, " +
                        "       m.name AS memberName, m.id AS memberId, " +
                        "       pb.prebookDate, pb.pickupDate, pb.status " +
                        "FROM Prebookings pb " +
                        "JOIN Books   b ON pb.bookId   = b.id " +
                        "JOIN Members m ON pb.memberId = m.id " +
                        "ORDER BY pb.prebookDate ASC";
        try (Connection conn = dbManager.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Date pickupDateSql = rs.getDate("pickupDate");
                LocalDate pickupDate = pickupDateSql != null
                        ? pickupDateSql.toLocalDate()
                        : rs.getDate("prebookDate").toLocalDate();
                preBookings.add(new PreBooking(
                        rs.getString("prebookId"),
                        rs.getString("memberName"),
                        rs.getString("memberId"),
                        rs.getString("bookTitle"),
                        pickupDate,
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return preBookings;
    }

    /**
     * Returns pre-bookings for a specific student (by member name).
     */
    public ObservableList<PreBooking> getPreBookingsForMember(String memberName) {
        ObservableList<PreBooking> list = FXCollections.observableArrayList();
        String sql =
                "SELECT pb.id AS prebookId, b.title AS bookTitle, " +
                        "       m.name AS memberName, m.id AS memberId, " +
                        "       pb.prebookDate, pb.pickupDate, pb.status " +
                        "FROM Prebookings pb " +
                        "JOIN Books   b ON pb.bookId   = b.id " +
                        "JOIN Members m ON pb.memberId = m.id " +
                        "WHERE m.name = ? ORDER BY pb.prebookDate ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date pickupDateSql = rs.getDate("pickupDate");
                    LocalDate pickupDate = pickupDateSql != null
                            ? pickupDateSql.toLocalDate()
                            : rs.getDate("prebookDate").toLocalDate();
                    list.add(new PreBooking(
                            rs.getString("prebookId"),
                            rs.getString("memberName"),
                            rs.getString("memberId"),
                            rs.getString("bookTitle"),
                            pickupDate,
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public int totalBooks()     { return queryInt("SELECT COALESCE(SUM(totalCopies), 0) FROM Books"); }
    public int totalAvailable() { return queryInt("SELECT COALESCE(SUM(availableCopies), 0) FROM Books"); }
    public int totalBorrowed()  { return queryInt("SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL"); }
    public int totalOverdue()   { return queryInt("SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL AND dueDate < CURRENT_DATE()"); }
    public int totalReserved()  { return queryInt("SELECT COUNT(*) FROM Prebookings WHERE status = 'PENDING' OR status = 'CONFIRMED'"); }
    public int dueToday()       { return queryInt("SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL AND dueDate = CURRENT_DATE()"); }
    public int activeMembers()  { return queryInt("SELECT COUNT(*) FROM Members"); }

    // ── Write: Borrow ─────────────────────────────────────────────────────────

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

    // ── Write: Return ─────────────────────────────────────────────────────────

    public void returnBook(Transaction txn) {
        String getBookIdSql    = "SELECT bookId FROM Borrowings WHERE id = ?";
        String markReturnedSql = "UPDATE Borrowings SET returnDate = CURRENT_DATE() WHERE id = ? AND returnDate IS NULL";
        String updateBookSql   = "UPDATE Books SET availableCopies = availableCopies + 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String bookId = null;
                try (PreparedStatement ps = conn.prepareStatement(getBookIdSql)) {
                    ps.setString(1, txn.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) bookId = rs.getString("bookId");
                    }
                }
                if (bookId == null) throw new SQLException("No borrowing found for transaction id: " + txn.getId());

                try (PreparedStatement ps = conn.prepareStatement(markReturnedSql)) {
                    ps.setString(1, txn.getId());
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(updateBookSql)) {
                    ps.setString(1, bookId);
                    ps.executeUpdate();
                }

                conn.commit();
                txn.setReturned(true);
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── Write: Pre-Booking ────────────────────────────────────────────────────

    /**
     * FIX: Now also decrements availableCopies in the same transaction,
     * so the available quantity reflects the pre-booked copy immediately.
     */
    public String addPreBooking(String studentName, String studentId,
                                Book book, LocalDate pickupDate) {

        if (book.getAvailable() <= 0) {
            return "No available copies for this book.";
        }

        String memberId = getOrCreateMember(studentName, studentId);

        String insertSql =
                "INSERT INTO Prebookings (id, bookId, memberId, prebookDate, pickupDate, status) " +
                        "VALUES (?, ?, ?, ?, ?, 'PENDING')";
        String updateBookSql =
                "UPDATE Books SET availableCopies = availableCopies - 1 WHERE id = ? AND availableCopies > 0";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psBook   = conn.prepareStatement(updateBookSql);
                 PreparedStatement psInsert = conn.prepareStatement(insertSql)) {

                // Decrement first — roll back if no copies left
                psBook.setString(1, book.getId());
                int updated = psBook.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    return "No available copies — pre-booking failed.";
                }

                psInsert.setString(1, UUID.randomUUID().toString());
                psInsert.setString(2, book.getId());
                psInsert.setString(3, memberId);
                psInsert.setDate(4, Date.valueOf(LocalDate.now()));
                psInsert.setDate(5, Date.valueOf(pickupDate));
                psInsert.executeUpdate();

                conn.commit();
                return null; // success
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return "Database error: " + e.getMessage();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Admin approves a pre-booking (changes status from PENDING to CONFIRMED).
     * Does NOT change availableCopies again — already decremented at booking time.
     */
    public void approvePreBooking(String bookingId) {
        String sql = "UPDATE Prebookings SET status = 'CONFIRMED' WHERE id = ? AND status = 'PENDING'";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bookingId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Admin cancels a pre-booking and restores the available copy.
     */
    public void cancelPreBooking(String bookingId) {
        String getBookIdSql = "SELECT bookId FROM Prebookings WHERE id = ? AND status != 'CANCELLED'";
        String cancelSql    = "UPDATE Prebookings SET status = 'CANCELLED' WHERE id = ?";
        String restoreSql   = "UPDATE Books SET availableCopies = availableCopies + 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String bookId = null;
                try (PreparedStatement ps = conn.prepareStatement(getBookIdSql)) {
                    ps.setString(1, bookingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) bookId = rs.getString("bookId");
                    }
                }
                if (bookId == null) { conn.rollback(); return; }

                try (PreparedStatement ps = conn.prepareStatement(cancelSql)) {
                    ps.setString(1, bookingId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(restoreSql)) {
                    ps.setString(1, bookId);
                    ps.executeUpdate();
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

    // ── Write: Books ──────────────────────────────────────────────────────────

    public void addBook(Book book) {
        String sql =
                "INSERT INTO Books (id, title, author, isbn, totalCopies, availableCopies) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
     * Deletes a book from the inventory. Only allowed if no active borrowings exist.
     * Returns null on success, or an error message.
     */
    public String deleteBook(String bookId) {
        // Check active borrowings
        String checkSql = "SELECT COUNT(*) FROM Borrowings WHERE bookId = ? AND returnDate IS NULL";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setString(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return "Cannot delete: this book has active borrowings.";
                }
            }
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }

        // Cancel any pending pre-bookings first
        String cancelPrebookSql = "UPDATE Prebookings SET status = 'CANCELLED' WHERE bookId = ? AND status IN ('PENDING','CONFIRMED')";
        String deleteSql = "DELETE FROM Books WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement(cancelPrebookSql);
                 PreparedStatement ps2 = conn.prepareStatement(deleteSql)) {
                ps1.setString(1, bookId);
                ps1.executeUpdate();
                ps2.setString(1, bookId);
                ps2.executeUpdate();
                conn.commit();
                return null;
            } catch (SQLException e) {
                conn.rollback();
                return "Delete failed: " + e.getMessage();
            }
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // ── Members ───────────────────────────────────────────────────────────────

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

    private String getOrCreateMember(String name, String studentId) {
        String existing = getMemberIdByName(name);
        if (existing != null) return existing;

        String newId = UUID.randomUUID().toString();
        String sql = "INSERT INTO Members (id, name, contactInfo) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newId);
            ps.setString(2, name);
            ps.setString(3, studentId);
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