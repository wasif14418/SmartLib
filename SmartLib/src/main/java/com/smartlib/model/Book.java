package com.smartlib.model;

import javafx.beans.property.*;

public class Book {
    private final StringProperty bookId;
    private final StringProperty title;
    private final StringProperty author;
    private final StringProperty isbn;
    private final IntegerProperty totalQty;
    private final IntegerProperty available;
    private final IntegerProperty reserved;

    public Book(String bookId, String title, String author, String isbn, int totalQty, int available, int reserved) {
        this.bookId    = new SimpleStringProperty(bookId);
        this.title     = new SimpleStringProperty(title);
        this.author    = new SimpleStringProperty(author);
        this.isbn      = new SimpleStringProperty(isbn);
        this.totalQty  = new SimpleIntegerProperty(totalQty);
        this.available = new SimpleIntegerProperty(available);
        this.reserved  = new SimpleIntegerProperty(reserved);
    }

    public String getStatus() {
        if (available.get() > 0)  return "Available";
        if (reserved.get() > 0)   return "Fully Reserved";
        return "Not Available";
    }

    // Getters
    public String getId()        { return bookId.get(); }  // FIX: Added missing getId() used by LibraryService.registerBorrow()
    public String getBookId()    { return bookId.get(); }
    public String getTitle()     { return title.get(); }
    public String getAuthor()    { return author.get(); }
    public String getIsbn()      { return isbn.get(); }
    public int    getTotalQty()  { return totalQty.get(); }
    public int    getAvailable() { return available.get(); }
    public int    getReserved()  { return reserved.get(); }

    // Properties
    public StringProperty  bookIdProperty()    { return bookId; }
    public StringProperty  titleProperty()     { return title; }
    public StringProperty  authorProperty()    { return author; }
    public StringProperty  isbnProperty()      { return isbn; }
    public IntegerProperty totalQtyProperty()  { return totalQty; }
    public IntegerProperty availableProperty() { return available; }
    public IntegerProperty reservedProperty()  { return reserved; }

    // Setters
    public void setAvailable(int v) { available.set(v); }
    public void setReserved(int v)  { reserved.set(v); }
}