package com.smartlib.ui.pages;

import com.smartlib.model.Book;
import com.smartlib.model.Transaction;
import com.smartlib.service.LibraryService;
import com.smartlib.ui.UiFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;

public class BorrowPage {

    private final LibraryService svc = LibraryService.getInstance();

    public Node build() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("edge-to-edge");

        VBox page = new VBox(16);
        page.setPadding(new Insets(22, 24, 22, 24));
        page.getStyleClass().add("page");

        page.getChildren().addAll(buildForm(), buildRecentTable());
        scroll.setContent(page);
        return scroll;
    }

    // ── Registration form ─────────────────────────────────────────────────────

    private VBox buildForm() {
        VBox card = UiFactory.card("📤  New Borrow Registration");
        card.setMaxWidth(680);

        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(12);
        grid.setPadding(new Insets(18));

        TextField fName  = field("e.g. Rafiur Rahman");
        TextField fId    = field("e.g. 0812410205101062");
        TextField fBatch = field("e.g. Batch 14");

        ComboBox<String> cbDept = new ComboBox<>();
        cbDept.getItems().addAll("CSE","EEE","CE","ME");
        cbDept.setValue("CSE");
        cbDept.getStyleClass().add("combo-dark");
        cbDept.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Book> cbBook = new ComboBox<>();
        cbBook.setMaxWidth(Double.MAX_VALUE);
        cbBook.getStyleClass().add("combo-dark");
        // show only available books
        svc.getBooks().filtered(b -> b.getAvailable() > 0).forEach(cbBook.getItems()::add);
        cbBook.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Book b, boolean empty) {
                super.updateItem(b, empty);
                setText(b == null ? "" : b.getBookId() + " · " + b.getTitle());
            }
        });
        cbBook.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Book b, boolean empty) {
                super.updateItem(b, empty);
                setText(b == null ? "" : b.getBookId() + " · " + b.getTitle());
            }
        });

        DatePicker dpIssue = datePicker(LocalDate.now());
        DatePicker dpDue   = datePicker(LocalDate.now().plusDays(14));

        grid.addRow(0, lbl("Student Name"), fName,  lbl("Student ID"), fId);
        grid.addRow(1, lbl("Batch"),        fBatch, lbl("Department"), cbDept);
        grid.add(lbl("Book"), 0, 2);
        grid.add(cbBook, 1, 2, 3, 1);
        grid.addRow(3, lbl("Issue Date"), dpIssue, lbl("Due Date"), dpDue);

        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(i % 2 == 1 ? Priority.ALWAYS : Priority.NEVER);
            if (i % 2 == 0) cc.setMinWidth(100);
            grid.getColumnConstraints().add(cc);
        }

        Label feedback = new Label();
        feedback.getStyleClass().add("feedback-label");

        Button reg   = new Button("✅  Register Borrow");
        reg.getStyleClass().addAll("btn", "btn-primary");
        Button clear = new Button("Clear");
        clear.getStyleClass().addAll("btn", "btn-outline");

        reg.setOnAction(e -> {
            if (fName.getText().isBlank() || fId.getText().isBlank() || cbBook.getValue() == null) {
                feedback.setText("⚠ Please fill all required fields.");
                feedback.setStyle("-fx-text-fill:#f87171;");
                return;
            }
            svc.registerBorrow(fName.getText().trim(), fId.getText().trim(),
                    fBatch.getText().trim(), cbDept.getValue(),
                    cbBook.getValue(), dpIssue.getValue(), dpDue.getValue());
            feedback.setText("✅ Borrow registered successfully!");
            feedback.setStyle("-fx-text-fill:#6ee7b7;");
            fName.clear(); fId.clear(); fBatch.clear(); cbBook.setValue(null);
        });
        clear.setOnAction(e -> { fName.clear(); fId.clear(); fBatch.clear(); cbBook.setValue(null); feedback.setText(""); });

        HBox btns = new HBox(10, reg, clear);
        btns.setPadding(new Insets(0, 18, 18, 18));

        card.getChildren().addAll(grid, feedback, btns);
        return card;
    }

    // ── Recent borrows table ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private VBox buildRecentTable() {
        VBox card = UiFactory.card("📋  Recent Borrows");

        TableView<Transaction> tv = new TableView<>(svc.getTransactions());
        tv.getStyleClass().add("dark-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setPrefHeight(220);

        TableColumn<Transaction,String> c1 = tc("Txn ID",      "txnId",       100);
        TableColumn<Transaction,String> c2 = tc("Student",     "studentName", 160);
        TableColumn<Transaction,String> c3 = tc("Book",        "bookTitle",   220);
        TableColumn<Transaction,LocalDate> c4 = tcDate("Issue Date","issueDate",110);
        TableColumn<Transaction,LocalDate> c5 = tcDate("Due Date",  "dueDate",  110);

        TableColumn<Transaction, Void> c6 = new TableColumn<>("Status");
        c6.setMinWidth(130);
        c6.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(UiFactory.statusPill(getTableRow().getItem().getDueStatus()));
            }
        });

        tv.getColumns().addAll(c1, c2, c3, c4, c5, c6);
        card.getChildren().add(tv);
        return card;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private <T> TableColumn<Transaction, T> tc(String h, String prop, double w) {
        TableColumn<Transaction,T> c = new TableColumn<>(h);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(w); return c;
    }
    private TableColumn<Transaction, LocalDate> tcDate(String h, String prop, double w) {
        TableColumn<Transaction, LocalDate> c = new TableColumn<>(h);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(w); return c;
    }
    private TextField field(String prompt) {
        TextField t = new TextField(); t.setPromptText(prompt);
        t.getStyleClass().add("dark-field"); return t;
    }
    private Label lbl(String text) {
        Label l = new Label(text); l.getStyleClass().add("form-label"); return l;
    }
    private DatePicker datePicker(LocalDate val) {
        DatePicker dp = new DatePicker(val);
        dp.getStyleClass().add("dark-datepicker");
        dp.setMaxWidth(Double.MAX_VALUE); return dp;
    }
}
