package com.smartlib.ui.pages;

import com.smartlib.model.Transaction;
import com.smartlib.service.LibraryService;
import com.smartlib.ui.UiFactory;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;

public class NotificationsPage {

    private final LibraryService svc = LibraryService.getInstance();

    public Node build() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(22, 24, 22, 24));
        page.getStyleClass().add("page");

        VBox card = UiFactory.card("🔔  Smart Notification Center");
        VBox body = new VBox(8);
        body.setPadding(new Insets(14, 14, 14, 14));

        boolean anyAlert = false;

        for (Transaction t : svc.getTransactions()) {
            if (t.isReturned()) continue;
            long d = t.daysLeft();

            if (d < 0) {
                body.getChildren().add(UiFactory.alertRow("🚨",
                    "OVERDUE: \"" + t.getBookTitle() + "\" — " + t.getStudentName(),
                    "Was due " + t.getDueDate() + " · Overdue by " + Math.abs(d) + " days · Estimated fine: ৳" + (int)t.getFine(),
                    "notif-red"));
                anyAlert = true;
            } else if (d == 0) {
                body.getChildren().add(UiFactory.alertRow("⏰",
                    "DUE TODAY: \"" + t.getBookTitle() + "\" — " + t.getStudentName(),
                    "Return by end of today (" + t.getDueDate() + ") to avoid fine",
                    "notif-warn"));
                anyAlert = true;
            } else if (d <= 3) {
                body.getChildren().add(UiFactory.alertRow("⏰",
                    "DUE SOON: \"" + t.getBookTitle() + "\" — " + t.getStudentName(),
                    "Due in " + d + " day(s) — " + t.getDueDate(),
                    "notif-warn"));
                anyAlert = true;
            }
        }

        // Returned notifications
        svc.getTransactions().stream().filter(Transaction::isReturned).forEach(t ->
            body.getChildren().add(UiFactory.alertRow("✅",
                "RETURNED: \"" + t.getBookTitle() + "\" — " + t.getStudentName(),
                "Returned · " + t.getDueDate() + " · No fine",
                "notif-green"))
        );

        // Pre-booking confirmations
        svc.getPreBookings().forEach(pb ->
            body.getChildren().add(UiFactory.alertRow("🔖",
                "PRE-BOOKING CONFIRMED: \"" + pb.getBookTitle() + "\" — " + pb.getStudentName(),
                "Pickup scheduled: " + pb.getPickupDate(),
                "notif-green"))
        );

        if (body.getChildren().isEmpty()) {
            body.getChildren().add(UiFactory.alertRow("✅","All clear!","No pending notifications.","notif-green"));
        }

        card.getChildren().add(body);
        VBox.setVgrow(card, Priority.ALWAYS);
        page.getChildren().add(card);
        return page;
    }
}
