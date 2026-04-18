package com.smartlib.ui.pages;

import com.smartlib.model.Transaction;
import com.smartlib.service.LibraryService;
import com.smartlib.ui.UiFactory;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;

public class ReturnPage {

    private final LibraryService svc = LibraryService.getInstance();

    public Node build() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(22, 24, 22, 24));
        page.getStyleClass().add("page");

        TextField search = new TextField();
        search.setPromptText("🔍  Search student or book…");
        search.getStyleClass().add("search-bar");
        search.setPrefWidth(260);

        VBox card = UiFactory.cardWithAction("📥  Return Tracking & Due Monitor", search);

        FilteredList<Transaction> active = new FilteredList<>(svc.getTransactions(),
                t -> !t.isReturned());
        search.textProperty().addListener((obs, o, n) -> {
            String q = n.toLowerCase();
            active.setPredicate(t -> !t.isReturned() && (q.isBlank()
                    || t.getStudentName().toLowerCase().contains(q)
                    || t.getBookTitle().toLowerCase().contains(q)));
        });

        TableView<Transaction> tv = buildTable(active);
        VBox.setVgrow(tv, Priority.ALWAYS);
        card.getChildren().add(tv);
        VBox.setVgrow(card, Priority.ALWAYS);

        page.getChildren().add(card);
        return page;
    }

    @SuppressWarnings("unchecked")
    private TableView<Transaction> buildTable(FilteredList<Transaction> data) {
        TableView<Transaction> tv = new TableView<>(data);
        tv.getStyleClass().add("dark-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Transaction,String>     c1  = tc("Txn ID",     "txnId",       90);
        TableColumn<Transaction,String>     c2  = tc("Student",    "studentName", 140);
        TableColumn<Transaction,String>     c3  = tc("ID",         "studentId",   90);
        TableColumn<Transaction,String>     c4  = tc("Book",       "bookTitle",   200);
        TableColumn<Transaction,LocalDate>  c5  = new TableColumn<>("Due Date");
        c5.setCellValueFactory(d -> d.getValue().dueDateProperty());
        c5.setMinWidth(100);

        TableColumn<Transaction, Void> c6 = new TableColumn<>("Status");
        c6.setMinWidth(140);
        c6.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(UiFactory.statusPill(getTableRow().getItem().getDueStatus()));
            }
        });

        TableColumn<Transaction, Void> c7 = new TableColumn<>("Fine (৳)");
        c7.setMinWidth(90);
        c7.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setText(null); return; }
                double fine = getTableRow().getItem().getFine();
                Label l = new Label(fine > 0 ? "৳ " + (int)fine : "৳ 0");
                l.setStyle(fine > 0 ? "-fx-text-fill:#f87171;-fx-font-weight:bold;" : "-fx-text-fill:#6b7280;");
                setGraphic(l);
            }
        });

        TableColumn<Transaction, Void> c8 = new TableColumn<>("Action");
        c8.setMinWidth(100);
        c8.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Return");
            { btn.getStyleClass().addAll("btn","btn-primary","btn-sm"); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                Transaction t = getTableRow().getItem();
                btn.setOnAction(e -> {
                    svc.returnBook(t);
                    getTableView().refresh();
                });
                setGraphic(btn);
            }
        });

        tv.getColumns().addAll(c1, c2, c3, c4, c5, c6, c7, c8);
        return tv;
    }

    private <T> TableColumn<Transaction, T> tc(String h, String prop, double w) {
        TableColumn<Transaction,T> c = new TableColumn<>(h);
        c.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>(prop));
        c.setMinWidth(w); return c;
    }
}
