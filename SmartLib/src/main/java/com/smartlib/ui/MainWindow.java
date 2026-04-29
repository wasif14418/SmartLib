package com.smartlib.ui;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;
import com.smartlib.ui.pages.*;
import javafx.scene.layout.Pane;

public class MainWindow {

    private final StackPane root = new StackPane();
    private final BorderPane mainLayout = new BorderPane();
    private final VBox sidebar;
    private final Pane overlay = new Pane();
    private final StackPane contentArea = new StackPane();
    private final Label topBadge = new Label("Overview");

    private boolean menuVisible = false;

    // Page instances
    private Node dashPage, booksPage, borrowPage, returnPage, prebookPage, notifPage, analyticsPage;

    public MainWindow() {
        sidebar = buildSidebar();

        // Configure Overlay: It must catch mouse events to trigger the close
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.4);"); // Dim effect
        overlay.setOpacity(0);
        overlay.setVisible(false);

        // This listener is critical for "clicking anywhere else" to work
        overlay.setOnMouseClicked(e -> {
            if (menuVisible) toggleMenu();
        });

        mainLayout.setTop(buildTopBar());
        mainLayout.setCenter(buildContentArea());

        // Sidebar initialization
        sidebar.setTranslateX(250);
        sidebar.setMaxWidth(250);
        StackPane.setAlignment(sidebar, Pos.TOP_RIGHT);

        root.getChildren().addAll(mainLayout, overlay, sidebar);

        showPage("dashboard");
    }

    public StackPane getRoot() { return root; }

    private HBox buildTopBar() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        bar.setPadding(new Insets(10, 25, 10, 25));

        // App Title on the Left
        Label appTitle = new Label("SmartLib");
        appTitle.setStyle("-fx-text-fill: white; -fx-font-size: 50px; -fx-font-weight: bold;");

        // Spacer to push hamburger to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Hamburger on the Right
        Label hamburger = new Label("☰");
        hamburger.getStyleClass().add("hamburger-icon");
        hamburger.setOnMouseClicked(e -> toggleMenu());

        // We removed topBadge totally as requested
        bar.getChildren().addAll(appTitle, spacer, hamburger);
        return bar;
    }

    private StackPane buildContentArea() {
        contentArea.getStyleClass().add("main-area");
        contentArea.setPadding(new Insets(20));
        return contentArea;
    }

    private void toggleMenu() {
        TranslateTransition sidebarTrans = new TranslateTransition(Duration.millis(300), sidebar);

        if (!menuVisible) {
            // OPENING MENU
            sidebarTrans.setToX(0);

            overlay.setVisible(true);
            overlay.setMouseTransparent(false); // Ensure it can be clicked

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            menuVisible = true;
        } else {
            // CLOSING MENU
            sidebarTrans.setToX(250);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), overlay);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                overlay.setVisible(false);
                overlay.setMouseTransparent(true); // Stop blocking clicks to main content
            });
            fadeOut.play();

            menuVisible = false;
        }
        sidebarTrans.play();
    }

    private VBox buildSidebar() {
        VBox sidebarBox = new VBox(10);
        // Apply the box style for visibility and shadows
        sidebarBox.getStyleClass().add("sidebar-content-box");
        sidebarBox.setPrefWidth(260);
        sidebarBox.setPadding(new Insets(20));

        // ── CLOSE BUTTON ──
        HBox closeContainer = new HBox();
        closeContainer.setAlignment(Pos.CENTER_RIGHT);
        Label closeBtn = new Label("✕");
        closeBtn.getStyleClass().add("close-menu-btn");
        closeBtn.setOnMouseClicked(e -> toggleMenu());
        closeContainer.getChildren().add(closeBtn);

        // ── MENU ITEMS ──
        VBox menuItems = new VBox(8);
        menuItems.getChildren().addAll(
                navItem("🏠", "Home", "dashboard"),
                navItem("🔔", "Notifications", "notif"),
                navItem("📖", "Library", "books"),
                navItem("🔖", "Pre-Order", "prebook"),
                navItem("📜", "History", "history"),
                navItem("📊", "Analytics", "analytics")
        );

        sidebarBox.getChildren().addAll(closeContainer, menuItems);

        // Wrap in a container to allow padding from the screen edge
        VBox wrapper = new VBox(sidebarBox);
        wrapper.setPadding(new Insets(10));
        wrapper.getStyleClass().add("sidebar-container");

        return wrapper;
    }

    private void showPage(String id) {
        contentArea.getChildren().clear();
        switch (id) {
            case "dashboard" -> {
                dashPage = new DashboardPage(this::showPage).build(); // Updated: Pass showPage reference
                contentArea.getChildren().add(dashPage);
                topBadge.setText("Overview");
            }
            case "notif" -> {
                notifPage = new NotificationsPage().build();
                contentArea.getChildren().add(notifPage);
                topBadge.setText("Alerts");
            }
            case "books" -> {
                booksPage = new BooksPage().build();
                contentArea.getChildren().add(booksPage);
                topBadge.setText("Inventory");
            }
            case "prebook" -> {
                prebookPage = new PreBookingPage().build();
                contentArea.getChildren().add(prebookPage);
                topBadge.setText("Reservations");
            }
            case "history" -> {
                // Combined view of Borrow and Return
                VBox historyBox = new VBox(20);
                historyBox.getChildren().addAll(new BorrowPage().build(), new ReturnPage().build());
                contentArea.getChildren().add(historyBox);
                topBadge.setText("Transaction History");
            }
            case "analytics" -> {
                analyticsPage = new AnalyticsPage().build();
                contentArea.getChildren().add(analyticsPage);
                topBadge.setText("Insights");
            }
        }
    }

    private HBox navItem(String icon, String label, String pageId) {
        HBox item = new HBox(12);
        item.getStyleClass().add("nav-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 15, 10, 15));

        Label ico = new Label(icon);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("nav-label");

        item.getChildren().addAll(ico, lbl);
        item.setOnMouseClicked(e -> {
            showPage(pageId);
            toggleMenu();
        });
        return item;
    }
}