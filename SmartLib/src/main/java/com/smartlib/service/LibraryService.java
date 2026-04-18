package com.smartlib.service;

import com.smartlib.model.Book;
import com.smartlib.model.PreBooking;
import com.smartlib.model.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;

/**
 * In-memory data store shared across all views.
 * Replace with JDBC/MySQL calls for production.
 */
public class LibraryService {

    private static LibraryService instance;

    private final ObservableList<Book>        books        = FXCollections.observableArrayList();
    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private final ObservableList<PreBooking>  preBookings  = FXCollections.observableArrayList();

    private int txnCounter  = 44;
    private int pbCounter   = 14;

    private LibraryService() { seed(); }

    public static LibraryService getInstance() {
        if (instance == null) instance = new LibraryService();
        return instance;
    }

    // ── Seed demo data ────────────────────────────────────────────────────────

    private void seed() {
        books.addAll(
            new Book("BK-001", "Data Structures & Algorithms",    "Mark Weiss",         5, 2, 1),
            new Book("BK-002", "Operating Systems",               "Silberschatz",        3, 0, 3),
            new Book("BK-003", "Java: The Complete Reference",    "Herbert Schildt",     4, 1, 2),
            new Book("BK-004", "Computer Networks",               "Andrew Tanenbaum",    2, 0, 0),
            new Book("BK-005", "Database Systems",                "Ramez Elmasri",       6, 4, 0),
            new Book("BK-006", "Discrete Mathematics",            "Kenneth Rosen",       3, 0, 3)
        );

        LocalDate today = LocalDate.now();
        transactions.addAll(
            new Transaction("TXN-039","Rafiur Rahman",  "101062","Batch 14","CSE",
                    "Data Structures & Algorithms", today.minusDays(17), today.minusDays(3)),
            new Transaction("TXN-041","Fatema Tasnim",  "101054","Batch 14","CSE",
                    "Operating Systems",            today.minusDays(14), today),
            new Transaction("TXN-042","Arif Hossain",   "101022","Batch 13","EEE",
                    "Database Systems",             today.minusDays(7),  today.plusDays(7)),
            new Transaction("TXN-038","Sheak Islam",    "101014","Batch 14","CSE",
                    "Java: The Complete Reference", today.minusDays(14), today.minusDays(1))
        );
        transactions.get(3).setReturned(true);   // TXN-038 returned

        preBookings.addAll(
            new PreBooking("PB-011","Nadia Islam",  "101030","Operating Systems",  today.plusDays(2),  "Confirmed"),
            new PreBooking("PB-012","Arif Hasan",   "101031","Discrete Mathematics",today.plusDays(3), "Confirmed"),
            new PreBooking("PB-013","Sadia Akter",  "101032","Computer Networks",   today.plusDays(4), "Pending")
        );
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public ObservableList<Book>        getBooks()        { return books; }
    public ObservableList<Transaction> getTransactions() { return transactions; }
    public ObservableList<PreBooking>  getPreBookings()  { return preBookings; }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public int totalBooks()    { return books.stream().mapToInt(Book::getTotalQty).sum(); }
    public int totalAvailable(){ return books.stream().mapToInt(Book::getAvailable).sum(); }
    public int totalBorrowed() {
        return (int) transactions.stream().filter(t -> !t.isReturned()).count();
    }
    public int totalOverdue() {
        return (int) transactions.stream()
                .filter(t -> !t.isReturned() && t.daysLeft() < 0).count();
    }
    public int totalReserved()    { return preBookings.size(); }
    public int dueToday() {
        return (int) transactions.stream()
                .filter(t -> !t.isReturned() && t.daysLeft() == 0).count();
    }
    public int activeMembers() { return 312; }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public void registerBorrow(String studentName, String studentId,
                               String batch, String dept,
                               Book book, LocalDate issueDate, LocalDate dueDate) {
        String id = "TXN-0" + (++txnCounter);
        transactions.add(new Transaction(id, studentName, studentId,
                batch, dept, book.getTitle(), issueDate, dueDate));
        book.setAvailable(book.getAvailable() - 1);
    }

    public void returnBook(Transaction txn) {
        txn.setReturned(true);
        // restore inventory
        books.stream()
             .filter(b -> b.getTitle().equals(txn.getBookTitle()))
             .findFirst()
             .ifPresent(b -> b.setAvailable(b.getAvailable() + 1));
    }

    public void addPreBooking(String studentName, String studentId,
                              Book book, LocalDate pickupDate) {
        String id = "PB-0" + (++pbCounter);
        preBookings.add(new PreBooking(id, studentName, studentId,
                book.getTitle(), pickupDate, "Confirmed"));
        book.setReserved(book.getReserved() + 1);
        if (book.getAvailable() > 0) book.setAvailable(book.getAvailable() - 1);
    }

    public void addBook(Book book) { books.add(book); }
}
