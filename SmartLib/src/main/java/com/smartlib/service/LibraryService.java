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

/**
 * Data store using JDBC/H2 database.
 */
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

    // ── Accessors ─────────────────────────────────────────────────────────────

    public ObservableList<Book> getBooks() {
        ObservableList<Book> books = FXCollections.observableArrayList();
        String sql = "SELECT id, title, author, isbn, totalCopies, availableCopies FROM Books";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getString("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("isbn"),
                        rs.getInt("totalCopies"),
                        rs.getInt("availableCopies"),
                        0 // Reserved count requires separate query
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public ObservableList<Transaction> getTransactions() {
        ObservableList<Transaction> transactions = FXCollections.observableArrayList();
        String sql = "SELECT b.id AS bookId, b.title AS bookTitle, " +
                "m.id AS memberId, m.name AS memberName, " +
                "t.id AS transactionId, t.borrowDate, t.dueDate, t.returnDate " +
                "FROM Borrowings t " +
                "JOIN Books b ON t.bookId = b.id " +
                "JOIN Members m ON t.memberId = m.id";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // FIX: returnDate is properly checked and used to set the 'returned' boolean
                boolean isReturned = rs.getDate("returnDate") != null;
                transactions.add(new Transaction(
                        rs.getString("transactionId"),
                        rs.getString("memberName"),
                        rs.getString("memberId"),
                        "N/A", // Batch - not stored in DB
                        "N/A", // Dept - not stored in DB
                        rs.getString("bookTitle"),
                        rs.getDate("borrowDate").toLocalDate(),
                        rs.getDate("dueDate").toLocalDate(),
                        isReturned  // FIX: was missing this argument; constructor now accepts it
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public ObservableList<PreBooking> getPreBookings() {
        ObservableList<PreBooking> preBookings = FXCollections.observableArrayList();
        String sql = "SELECT pb.id AS prebookId, b.title AS bookTitle, " +
                "m.name AS memberName, m.id AS memberId, " +
                "pb.prebookDate, pb.status " +
                "FROM Prebookings pb " +
                "JOIN Books b ON pb.bookId = b.id " +
                "JOIN Members m ON pb.memberId = m.id";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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
        String sql = "SELECT SUM(totalCopies) FROM Books";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int totalAvailable() {
        String sql = "SELECT SUM(availableCopies) FROM Books";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int totalBorrowed() {
        String sql = "SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int totalOverdue() {
        String sql = "SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL AND dueDate < CURRENT_DATE()";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int totalReserved() {
        String sql = "SELECT COUNT(*) FROM Prebookings WHERE status = 'PENDING' OR status = 'CONFIRMED'";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int dueToday() {
        String sql = "SELECT COUNT(*) FROM Borrowings WHERE returnDate IS NULL AND dueDate = CURRENT_DATE()";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int activeMembers() {
        String sql = "SELECT COUNT(*) FROM Members";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public void registerBorrow(String studentName, String studentId,
                               String batch, String dept,
                               Book book, LocalDate issueDate, LocalDate dueDate) {
        String transactionId = UUID.randomUUID().toString();
        String memberId = getMemberIdByName(studentName);
        if (memberId == null) {
            memberId = UUID.randomUUID().toString();
            addMember(new Member(memberId, studentName, studentId + "@example.com"));
        }

        String insertSql = "INSERT INTO Borrowings (id, bookId, memberId, borrowDate, dueDate, returnDate) VALUES (?, ?, ?, ?, ?, ?)";
        // FIX: Use book.getId() — this required adding getId() to Book.java
        String updateBookSql = "UPDATE Books SET availableCopies = availableCopies - 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql);
                 PreparedStatement pstmtBook = conn.prepareStatement(updateBookSql)) {

                pstmt.setString(1, transactionId);
                pstmt.setString(2, book.getId());  // FIX: book.getId() now exists
                pstmt.setString(3, memberId);
                pstmt.setDate(4, Date.valueOf(issueDate));
                pstmt.setDate(5, Date.valueOf(dueDate));
                pstmt.setNull(6, Types.DATE);
                pstmt.executeUpdate();

                pstmtBook.setString(1, book.getId());  // FIX: book.getId() now exists
                pstmtBook.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void returnBook(Transaction txn) {
        // FIX: Separated the two SQL operations to avoid unreliable correlated subquery on same connection.
        // First fetch the bookId, then update both tables explicitly.
        String getBookIdSql = "SELECT bookId FROM Borrowings WHERE id = ?";
        String updateTransactionSql = "UPDATE Borrowings SET returnDate = CURRENT_DATE() WHERE id = ?";
        String updateBookSql = "UPDATE Books SET availableCopies = availableCopies + 1 WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Step 1: get bookId for this transaction
                String bookId = null;
                try (PreparedStatement pstmtGet = conn.prepareStatement(getBookIdSql)) {
                    pstmtGet.setString(1, txn.getId()); // FIX: txn.getId() now exists (was missing in Transaction)
                    try (ResultSet rs = pstmtGet.executeQuery()) {
                        if (rs.next()) {
                            bookId = rs.getString("bookId");
                        }
                    }
                }

                if (bookId == null) {
                    throw new SQLException("No borrowing record found for transaction id: " + txn.getId());
                }

                // Step 2: mark as returned
                try (PreparedStatement pstmtTxn = conn.prepareStatement(updateTransactionSql)) {
                    pstmtTxn.setString(1, txn.getId());
                    pstmtTxn.executeUpdate();
                }

                // Step 3: increment available copies
                try (PreparedStatement pstmtBook = conn.prepareStatement(updateBookSql)) {
                    pstmtBook.setString(1, bookId);
                    pstmtBook.executeUpdate();
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

    public void addPreBooking(String studentName, String studentId,
                              Book book, LocalDate prebookDate) {
        String prebookId = UUID.randomUUID().toString();
        String memberId = getMemberIdByName(studentName);
        if (memberId == null) {
            memberId = UUID.randomUUID().toString();
            addMember(new Member(memberId, studentName, studentId + "@example.com"));
        }

        String insertSql = "INSERT INTO Prebookings (id, bookId, memberId, prebookDate, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setString(1, prebookId);
            pstmt.setString(2, book.getId());  // FIX: book.getId() now exists
            pstmt.setString(3, memberId);
            pstmt.setDate(4, Date.valueOf(prebookDate));
            pstmt.setString(5, "PENDING");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addBook(Book book) {
        String sql = "INSERT INTO Books (id, title, author, isbn, totalCopies, availableCopies) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getId());  // FIX: book.getId() now exists
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setString(4, book.getIsbn());
            pstmt.setInt(5, book.getTotalQty());
            pstmt.setInt(6, book.getAvailable());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMember(Member member) {
        String sql = "INSERT INTO Members (id, name, contactInfo) VALUES (?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, member.getId());
            pstmt.setString(2, member.getName());
            pstmt.setString(3, member.getContactInfo());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // FIX: Properly closed ResultSet using try-with-resources to prevent resource leak
    private String getMemberIdByName(String name) {
        String sql = "SELECT id FROM Members WHERE name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {  // FIX: ResultSet now in try-with-resources
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}