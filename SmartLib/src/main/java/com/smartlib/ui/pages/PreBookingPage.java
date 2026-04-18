package com.smartlib.ui.pages;

import com.smartlib.model.Book;
import com.smartlib.model.PreBooking;
import com.smartlib.service.LibraryService;
import com.smartlib.ui.UiFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;

public class PreBookingPage {

    private final LibraryService svc = LibraryService.getInstance();

    public Node build() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");

        VBox page = new VBox(16);
        page.setPadding(new Insets(22, 24, 22, 24));
        page.getStyleClass().add("page");

        page.getChildren().addAll(buildForm(), buildTable());
        scroll.setContent(page);
        return scroll;
    }

    private VBox buildForm() {
        VBox card = UiFactory.card("🔖  Advanced Pre-Booking");
        card.setMaxWidth(680);

        // Info banner
        HBox info = UiFactory.alertRow("ℹ️",
            "How Pre-Booking Works",
            "Reserved copies are immediately deducted from available quantity. Prevents double allocation.",
            "notif-green");
        info.setStyle(info.getStyle() + "-fx-margin:0;");

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.setPadding(new Insets(18, 18, 10, 18));

        TextField fName  = field("e.g. Sheak Islam");
        TextField fId    = field("e.g. 0812410205101014");
        TextField fBatch = field("e.g. Batch 14");

        ComboBox<Book> cbBook = new ComboBox<>();
        cbBook.setMaxWidth(Double.MAX_VALUE);
        cbBook.getStyleClass().add("combo-dark");
        svc.getBooks().forEach(cbBook.getItems()::add);
        cbBook.setButtonCell(bookCell());
        cbBook.setCellFactory(lv -> bookCell());

        DatePicker dpPickup = new DatePicker(LocalDate.now().plusDays(4));
        dpPickup.getStyleClass().add("dark-datepicker");
        dpPickup.setMaxWidth(Double.MAX_VALUE);

        grid.addRow(0, lbl("Student Name"), fName,   lbl("Student ID"), fId);
        grid.addRow(1, lbl("Batch"),        fBatch,  lbl("Pickup Date"),dpPickup);
        grid.add(lbl("Book"), 0, 2);
        grid.add(cbBook, 1, 2, 3, 1);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(i % 2 == 1 ? Priority.ALWAYS : Priority.NEVER);
            if (i % 2 == 0) cc.setMinWidth(100);
            grid.getColumnConstraints().add(cc);
        }

        Label feedback = new Label();
        feedback.getStyleClass().add("feedback-label");
        feedback.setPadding(new Insets(0, 18, 0, 18));

        Button confirm = new Button("🔖  Confirm Pre-Booking");
        confirm.getStyleClass().addAll("btn", "btn-primary");
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll("btn", "btn-outline");

        confirm.setOnAction(e -> {
            if (fName.getText().isBlank() || fId.getText().isBlank() || cbBook.getValue() == null) {
                feedback.setText("⚠ Please fill all required fields.");
                feedback.setStyle("-fx-text-fill:#f87171;");
                return;
            }
            svc.addPreBooking(fName.getText().trim(), fId.getText().trim(),
                    cbBook.getValue(), dpPickup.getValue());
            feedback.setText("✅ Pre-booking confirmed!");
            feedback.setStyle("-fx-text-fill:#6ee7b7;");
            fName.clear(); fId.clear(); fBatch.clear(); cbBook.setValue(null);
        });
        cancel.setOnAction(e -> { fName.clear(); fId.clear(); fBatch.clear(); cbBook.setValue(null); feedback.setText(""); });

        HBox btns = new HBox(10, confirm, cancel);
        btns.setPadding(new Insets(6, 18, 18, 18));

        VBox infoWrap = new VBox(info);
        infoWrap.setPadding(new Insets(0, 18, 10, 18));

        card.getChildren().addAll(grid, infoWrap, feedback, btns);
        return card;
    }

    @SuppressWarnings("unchecked")
    private VBox buildTable() {
        VBox card = UiFactory.card("📋  Active Pre-Bookings");

        TableView<PreBooking> tv = new TableView<>(svc.getPreBookings());
        tv.getStyleClass().add("dark-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPrefHeight(200);

        TableColumn<PreBooking,String> c1 = tc("Booking ID",  "bookingId",   110);
        TableColumn<PreBooking,String> c2 = tc("Student",     "studentName", 160);
        TableColumn<PreBooking,String> c3 = tc("Book",        "bookTitle",   220);
        TableColumn<PreBooking,LocalDate> c4 = new TableColumn<>("Pickup Date");
        c4.setCellValueFactory(d -> d.getValue().pickupDateProperty());
        c4.setMinWidth(120);

        TableColumn<PreBooking, Void> c5 = new TableColumn<>("Status");
        c5.setMinWidth(120);
        c5.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(UiFactory.statusPill(getTableRow().getItem().getStatus()));
            }
        });

        tv.getColumns().addAll(c1, c2, c3, c4, c5);
        card.getChildren().add(tv);
        return card;
    }

    private ListCell<Book> bookCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Book b, boolean empty) {
                super.updateItem(b, empty);
                setText(b == null ? "" : b.getBookId() + " · " + b.getTitle());
            }
        };
    }

    private <T> TableColumn<PreBooking, T> tc(String h, String prop, double w) {
        TableColumn<PreBooking,T> c = new TableColumn<>(h);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(w); return c;
    }
    private TextField field(String p) {
        TextField t = new TextField(); t.setPromptText(p); t.getStyleClass().add("dark-field"); return t;
    }
    private Label lbl(String text) {
        Label l = new Label(text); l.getStyleClass().add("form-label"); return l;
    }
}
