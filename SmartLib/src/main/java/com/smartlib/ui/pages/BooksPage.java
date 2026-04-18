package com.smartlib.ui.pages;

import com.smartlib.model.Book;
import com.smartlib.service.LibraryService;
import com.smartlib.ui.UiFactory;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class BooksPage {

    private final LibraryService svc = LibraryService.getInstance();

    public Node build() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(22, 24, 22, 24));
        page.getStyleClass().add("page");

        // Search field + Add button
        TextField search = new TextField();
        search.setPromptText("🔍  Search by title or author…");
        search.getStyleClass().add("search-bar");
        search.setPrefWidth(260);

        Button addBtn = new Button("+ Add Book");
        addBtn.getStyleClass().addAll("btn", "btn-primary");
        addBtn.setOnAction(e -> showAddBookDialog());

        VBox card = UiFactory.cardWithAction("📖  Book Inventory", search, addBtn);

        // Table
        TableView<Book> table = buildTable(search);
        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().add(table);
        VBox.setVgrow(card, Priority.ALWAYS);

        page.getChildren().add(card);
        return page;
    }

    @SuppressWarnings("unchecked")
    private TableView<Book> buildTable(TextField search) {
        FilteredList<Book> filtered = new FilteredList<>(svc.getBooks(), b -> true);
        search.textProperty().addListener((obs, o, n) -> {
            String q = n.toLowerCase();
            filtered.setPredicate(b -> q.isBlank()
                    || b.getTitle().toLowerCase().contains(q)
                    || b.getAuthor().toLowerCase().contains(q));
        });

        TableView<Book> tv = new TableView<>(filtered);
        tv.getStyleClass().add("dark-table");
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Book, String>  colId   = col("Book ID",   "bookId",   120);
        TableColumn<Book, String>  colTit  = col("Title",     "title",    260);
        TableColumn<Book, String>  colAuth = col("Author",    "author",   180);
        TableColumn<Book, Integer> colTot  = colInt("Total Qty",  "totalQty",  90);
        TableColumn<Book, Integer> colAvl  = colInt("Available",  "available", 90);
        TableColumn<Book, Integer> colRes  = colInt("Reserved",   "reserved",  90);

        TableColumn<Book, Void> colStat = new TableColumn<>("Status");
        colStat.setMinWidth(130);
        colStat.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setGraphic(null); return; }
                setGraphic(UiFactory.statusPill(getTableRow().getItem().getStatus()));
            }
        });

        tv.getColumns().addAll(colId, colTit, colAuth, colTot, colAvl, colRes, colStat);
        return tv;
    }

    private <T> TableColumn<Book, T> col(String h, String prop, double w) {
        TableColumn<Book, T> c = new TableColumn<>(h);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(w);
        return c;
    }
    private TableColumn<Book, Integer> colInt(String h, String prop, double w) {
        return col(h, prop, w);
    }

    // ── Add Book dialog ───────────────────────────────────────────────────────

    private void showAddBookDialog() {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Add New Book");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("dialog-grid");
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField fId     = field("e.g. BK-007");
        TextField fTitle  = field("Book title");
        TextField fAuthor = field("Author name");
        TextField fQty    = field("e.g. 4");

        grid.addRow(0, lbl("Book ID"),   fId);
        grid.addRow(1, lbl("Title"),     fTitle);
        grid.addRow(2, lbl("Author"),    fAuthor);
        grid.addRow(3, lbl("Quantity"),  fQty);

        Button save = new Button("✅ Add Book");
        save.getStyleClass().addAll("btn", "btn-primary");
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().addAll("btn", "btn-outline");

        save.setOnAction(e -> {
            try {
                int qty = Integer.parseInt(fQty.getText().trim());
                svc.addBook(new Book(fId.getText().trim(), fTitle.getText().trim(),
                        fAuthor.getText().trim(), qty, qty, 0));
                dlg.close();
            } catch (NumberFormatException ex) {
                fQty.setStyle("-fx-border-color: #ef4444;");
            }
        });
        cancel.setOnAction(e -> dlg.close());

        HBox btns = new HBox(10, save, cancel);
        btns.setPadding(new Insets(4, 0, 0, 0));
        grid.add(btns, 0, 4, 2, 1);

        Scene sc = new Scene(grid, 380, 260);
        sc.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        dlg.setScene(sc);
        dlg.showAndWait();
    }

    private TextField field(String prompt) {
        TextField t = new TextField(); t.setPromptText(prompt);
        t.getStyleClass().add("dialog-field"); return t;
    }
    private Label lbl(String text) {
        Label l = new Label(text); l.getStyleClass().add("dialog-label"); return l;
    }
}
