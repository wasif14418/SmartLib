package com.smartlib.ui.pages;

import com.smartlib.service.LibraryService;
import com.smartlib.ui.UiFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;

import java.util.function.Consumer; // Import Consumer

public class DashboardPage {

    private final LibraryService svc = LibraryService.getInstance();
    private final Consumer<String> pageNavigator; // Add pageNavigator field

    // Constructor to accept pageNavigator
    public DashboardPage(Consumer<String> pageNavigator) {
        this.pageNavigator = pageNavigator;
    }

    public Node build() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(22, 24, 22, 24));
        page.getStyleClass().add("page");

        // Row 1 – four stats
        HBox row1 = new HBox(14);
        VBox s1 = UiFactory.statCard("Total Books",  String.valueOf(svc.totalBooks()),    "Across all categories", "blue");
        VBox s2 = UiFactory.statCard("Available",    String.valueOf(svc.totalAvailable()),"Ready to borrow",       "green");
        VBox s3 = UiFactory.statCard("Borrowed",     String.valueOf(svc.totalBorrowed()), "Currently issued",      "yellow");
        VBox s4 = UiFactory.statCard("Overdue",      String.valueOf(svc.totalOverdue()),  "Needs attention",       "red");

        // Add click handlers for stat cards
        s1.setOnMouseClicked(e -> pageNavigator.accept("books"));
        s2.setOnMouseClicked(e -> pageNavigator.accept("books"));
        s3.setOnMouseClicked(e -> pageNavigator.accept("history"));
        s4.setOnMouseClicked(e -> pageNavigator.accept("notif"));

        for (VBox v : new VBox[]{s1,s2,s3,s4}) { HBox.setHgrow(v, Priority.ALWAYS); }
        row1.getChildren().addAll(s1, s2, s3, s4);

        // Row 2 – three stats
        HBox row2 = new HBox(14);
        VBox r1 = UiFactory.statCard("Reserved (Pre-booked)", String.valueOf(svc.totalReserved()), null, "yellow");
        VBox r2 = UiFactory.statCard("Due Today",             String.valueOf(svc.dueToday()),      null, "red");
        VBox r3 = UiFactory.statCard("Active Members",        String.valueOf(svc.activeMembers()), null, "blue");

        // Add click handlers for stat cards
        r1.setOnMouseClicked(e -> pageNavigator.accept("prebook"));
        r2.setOnMouseClicked(e -> pageNavigator.accept("notif"));
        r3.setOnMouseClicked(e -> pageNavigator.accept("analytics"));

        for (VBox v : new VBox[]{r1,r2,r3}) { HBox.setHgrow(v, Priority.ALWAYS); }
        row2.getChildren().addAll(r1, r2, r3);

        // Alerts card
        VBox alertsCard = UiFactory.card("🔔  Recent Alerts");
        VBox alertBody = new VBox(8);
        alertBody.setPadding(new Insets(12, 14, 14, 14));

        svc.getTransactions().forEach(t -> {
            HBox alertRow;
            if (!t.isReturned() && t.daysLeft() < 0) {
                alertRow = UiFactory.alertRow("⚠️",
                    "Overdue: \"" + t.getBookTitle() + "\" — " + t.getStudentName() + " (ID: " + t.getStudentId() + ")",
                    "Overdue by " + Math.abs(t.daysLeft()) + " days · Due: " + t.getDueDate(),
                    "notif-red");
            } else if (!t.isReturned() && t.daysLeft() == 0) {
                alertRow = UiFactory.alertRow("🕐",
                    "Due Today: \"" + t.getBookTitle() + "\" — " + t.getStudentName() + " (ID: " + t.getStudentId() + ")",
                    "Return by end of day to avoid fine",
                    "notif-warn");
            } else if (t.isReturned()) {
                alertRow = UiFactory.alertRow("✅",
                    "Returned: \"" + t.getBookTitle() + "\" — " + t.getStudentName() + " (ID: " + t.getStudentId() + ")",
                    "Returned on time · " + t.getDueDate(),
                    "notif-green");
            } else {
                alertRow = null; // Should not happen
            }

            if (alertRow != null) {
                alertRow.setOnMouseClicked(e -> pageNavigator.accept("notif")); // Click handler for alerts
                alertBody.getChildren().add(alertRow);
            }
        });

        if (alertBody.getChildren().isEmpty()) {
            HBox noAlertsRow = UiFactory.alertRow("✅","All books are on time!","No overdue or due-today items.","notif-green");
            noAlertsRow.setOnMouseClicked(e -> pageNavigator.accept("notif")); // Click handler for no alerts
            alertBody.getChildren().add(noAlertsRow);
        }

        alertsCard.getChildren().add(alertBody);
        VBox.setVgrow(alertsCard, Priority.ALWAYS);

        page.getChildren().addAll(row1, row2, alertsCard);
        return page;
    }
}
