package com.smartlib.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class PreBooking {
    private final StringProperty bookingId;
    private final StringProperty studentName;
    private final StringProperty studentId;
    private final StringProperty bookTitle;
    private final ObjectProperty<LocalDate> pickupDate;
    private final StringProperty status;

    public PreBooking(String bookingId, String studentName, String studentId,
                      String bookTitle, LocalDate pickupDate, String status) {
        this.bookingId   = new SimpleStringProperty(bookingId);
        this.studentName = new SimpleStringProperty(studentName);
        this.studentId   = new SimpleStringProperty(studentId);
        this.bookTitle   = new SimpleStringProperty(bookTitle);
        this.pickupDate  = new SimpleObjectProperty<>(pickupDate);
        this.status      = new SimpleStringProperty(status);
    }

    public String getBookingId()   { return bookingId.get(); }
    public String getStudentName() { return studentName.get(); }
    public String getStudentId()   { return studentId.get(); }
    public String getBookTitle()   { return bookTitle.get(); }
    public LocalDate getPickupDate(){ return pickupDate.get(); }
    public String getStatus()      { return status.get(); }

    public StringProperty bookingIdProperty()   { return bookingId; }
    public StringProperty studentNameProperty() { return studentName; }
    public StringProperty bookTitleProperty()   { return bookTitle; }
    public ObjectProperty<LocalDate> pickupDateProperty() { return pickupDate; }
    public StringProperty statusProperty()      { return status; }
}
