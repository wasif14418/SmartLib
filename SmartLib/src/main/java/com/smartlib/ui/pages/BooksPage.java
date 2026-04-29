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

        // Load the specific CSS file for the table
        String css = getClass().getResource("/css/books-table.css").toExternalForm();
        page.getStylesheets().add(css);
        page.getStyleClass().add("page");

        // 1. Header Controls
        TextField search = new TextField();
        search.setPromptText("🔍  Search by title or author…");
        search.getStyleClass().add("dark-field");
        search.setPrefWidth(300);

        Button addBtn = new Button("+ Add Book");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showAddBookDialog());

        // 2. Create the Card Container
        VBox card = UiFactory.cardWithAction("📖  Book Inventory", search, addBtn);
        VBox.setVgrow(card, Priority.ALWAYS);

        // 3. Build and Configure Table
        TableView<Book> table = buildTable(search);

        // Add the table to the card's children
        card.getChildren().add(table);
        VBox.setVgrow(table, Priority.ALWAYS);

        page.getChildren().add(card);
        return page;
    }

    private TableView<Book> buildTable(TextField search) {
        FilteredList<Book> filtered = new FilteredList<>(svc.getBooks(), b -> true);

        search.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal.toLowerCase();
            filtered.setPredicate(book -> query.isBlank()
                    || book.getTitle().toLowerCase().contains(query)
                    || book.getAuthor().toLowerCase().contains(query)
                    || book.getIsbn().toLowerCase().contains(query));
        });

        TableView<Book> tv = new TableView<>(filtered);

        // Apply the CSS class from books-table.css
        tv.getStyleClass().add("dark-table");

        // IMPORTANT: Set to -1 so row height grows automatically with font size
        tv.setFixedCellSize(-1);
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Define Columns
        tv.getColumns().addAll(
                col("Book ID",   "bookId",   100),
                col("Title",     "title",    250),
                col("Author",    "author",   180),
                col("ISBN",      "isbn",     140),
                colInt("Total",  "totalQty",  70),
                colInt("Avail",  "available", 70),
                colInt("Resv",   "reserved",  70),
                statusColumn()
        );

        return tv;
    }

    private TableColumn<Book, String> col(String label, String prop, double width) {
        TableColumn<Book, String> c = new TableColumn<>(label);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(width);
        return c;
    }

    private TableColumn<Book, Integer> colInt(String label, String prop, double width) {
        TableColumn<Book, Integer> c = new TableColumn<>(label);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setMinWidth(width);
        return c;
    }

    private TableColumn<Book, Void> statusColumn() {
        TableColumn<Book, Void> col = new TableColumn<>("Status");
        col.setMinWidth(130);
        col.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Book b = getTableRow().getItem();
                    setGraphic(UiFactory.statusPill(b.getStatus()));
                }
            }
        });
        return col;
    }

    private void showAddBookDialog() {
        Stage dlg = new Stage();
        dlg.initModality(Modality.APPLICATION_MODAL);
        dlg.setTitle("Add New Book");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(12);
        grid.setVgap(12);
        grid.getStyleClass().add("main-area"); // Dark background

        TextField fId = field("BK-XXXX");
        TextField fTitle = field("Book Title");
        TextField fAuthor = field("Author Name");
        TextField fIsbn = field("ISBN Number");
        TextField fQty = field("Quantity");

        grid.add(new Label("ID:"), 0, 0);      grid.add(fId, 1, 0);
        grid.add(new Label("Title:"), 0, 1);   grid.add(fTitle, 1, 1);
        grid.add(new Label("Author:"), 0, 2);  grid.add(fAuthor, 1, 2);
        grid.add(new Label("ISBN:"), 0, 3);    grid.add(fIsbn, 1, 3);
        grid.add(new Label("Qty:"), 0, 4);     grid.add(fQty, 1, 4);

        Button save = new Button("✅ Add Book");
        save.getStyleClass().add("btn-primary");

        save.setOnAction(e -> {
            try {
                int qty = Integer.parseInt(fQty.getText().trim());
                svc.addBook(new Book(
                        fId.getText().trim(),
                        fTitle.getText().trim(),
                        fAuthor.getText().trim(),
                        fIsbn.getText().trim(),
                        qty, qty, 0
                ));
                dlg.close();
            } catch (Exception ex) {
                fQty.setStyle("-fx-border-color: red;");
            }
        });

        HBox footer = new HBox(save);
        footer.setAlignment(Pos.CENTER_RIGHT);
        grid.add(footer, 1, 5);

        Scene scene = new Scene(grid);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        dlg.setScene(scene);
        dlg.showAndWait();
    }

    private TextField field(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        t.getStyleClass().add("dark-field");
        return t;
    }
}