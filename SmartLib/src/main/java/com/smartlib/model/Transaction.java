package com.smartlib.model;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Transaction {
    private final StringProperty txnId;
    private final StringProperty studentName;
    private final StringProperty studentId;
    private final StringProperty batch;
    private final StringProperty department;
    private final StringProperty bookTitle;
    private final ObjectProperty<LocalDate> issueDate;
    private final ObjectProperty<LocalDate> dueDate;
    private final BooleanProperty returned;

    public Transaction(String txnId, String studentName, String studentId,
                       String batch, String department, String bookTitle,
                       LocalDate issueDate, LocalDate dueDate) {
        this.txnId       = new SimpleStringProperty(txnId);
        this.studentName = new SimpleStringProperty(studentName);
        this.studentId   = new SimpleStringProperty(studentId);
        this.batch       = new SimpleStringProperty(batch);
        this.department  = new SimpleStringProperty(department);
        this.bookTitle   = new SimpleStringProperty(bookTitle);
        this.issueDate   = new SimpleObjectProperty<>(issueDate);
        this.dueDate     = new SimpleObjectProperty<>(dueDate);
        this.returned    = new SimpleBooleanProperty(false);
    }

    /** Days remaining (negative = overdue). */
    public long daysLeft() {
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate.get());
    }

    public String getDueStatus() {
        if (returned.get()) return "Returned";
        long d = daysLeft();
        if (d < 0)  return "Overdue " + Math.abs(d) + "d";
        if (d == 0) return "Due Today";
        return d + " days left";
    }

    public double getFine() {
        if (returned.get()) return 0;
        long overdue = -daysLeft();
        return overdue > 0 ? overdue * 5.0 : 0;
    }

    // Getters
    public String     getTxnId()       { return txnId.get(); }
    public String     getStudentName() { return studentName.get(); }
    public String     getStudentId()   { return studentId.get(); }
    public String     getBatch()       { return batch.get(); }
    public String     getDepartment()  { return department.get(); }
    public String     getBookTitle()   { return bookTitle.get(); }
    public LocalDate  getIssueDate()   { return issueDate.get(); }
    public LocalDate  getDueDate()     { return dueDate.get(); }
    public boolean    isReturned()     { return returned.get(); }
    public void       setReturned(boolean v) { returned.set(v); }

    // Properties
    public StringProperty             txnIdProperty()       { return txnId; }
    public StringProperty             studentNameProperty() { return studentName; }
    public StringProperty             studentIdProperty()   { return studentId; }
    public StringProperty             bookTitleProperty()   { return bookTitle; }
    public ObjectProperty<LocalDate>  issueDateProperty()   { return issueDate; }
    public ObjectProperty<LocalDate>  dueDateProperty()     { return dueDate; }
    public BooleanProperty            returnedProperty()    { return returned; }
}
