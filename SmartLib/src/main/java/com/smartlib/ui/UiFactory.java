package com.smartlib.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class UiFactory {

    // ── Stat Card with Accent Colors ──
    public static VBox statCard(String label, String value, String sub, String statusType) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(20));
        // This adds .stat-card-box and .card-blue (or green, etc)
        card.getStyleClass().addAll("stat-card-box", "card-" + statusType);

        Label lbl = new Label(label.toUpperCase());
        lbl.getStyleClass().add("stat-label");

        Label val = new Label(value);
        val.getStyleClass().add("stat-value");

        card.getChildren().addAll(lbl, val);

        if (sub != null) {
            Label s = new Label(sub);
            s.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            card.getChildren().add(s);
        }
        return card;
    }

    // ── Alert Row for Notifications & Dashboard ──
    public static HBox alertRow(String icon, String title, String sub, String styleClass) {
        HBox row = new HBox(15);
        row.getStyleClass().addAll("alert-card-box", styleClass);
        row.setPadding(new Insets(15));
        row.setAlignment(Pos.CENTER_LEFT);

        Label ico = new Label(icon);
        ico.getStyleClass().add("notif-icon");

        VBox text = new VBox(2);
        Label t = new Label(title);
        t.getStyleClass().add("notif-title");
        Label s = new Label(sub);
        s.getStyleClass().add("notif-sub");

        text.getChildren().addAll(t, s);
        HBox.setHgrow(text, Priority.ALWAYS);

        row.getChildren().addAll(ico, text);
        return row;
    }

    // ── Basic Card Container ──
    public static VBox card(String title) {
        VBox card = new VBox();
        card.getStyleClass().add("alert-card-box");
        card.setPadding(new Insets(15));

        Label t = new Label(title);
        t.getStyleClass().add("card-title-main"); // Updated class

        card.getChildren().add(t);
        return card;
    }

    // ── Card with Header Actions (Required by Books/Return Pages) ──
    public static VBox cardWithAction(String title, Node... actions) {
        VBox card = new VBox();
        card.getStyleClass().add("alert-card-box");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15));

        Label t = new Label(title);
        t.getStyleClass().add("card-title-main");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().add(t);
        header.getChildren().add(spacer);
        header.getChildren().addAll(actions);

        card.getChildren().add(header);
        return card;
    }

    // ── Status Pill for Tables ──
    public static Label statusPill(String text) {
        Label pill = new Label(text.toUpperCase());
        pill.getStyleClass().add("status-pill");

        String lower = text.toLowerCase();
        if (lower.contains("available") || lower.contains("returned")) {
            pill.getStyleClass().add("pill-available");
        } else if (lower.contains("reserved") || lower.contains("pending")) {
            pill.getStyleClass().add("pill-reserved");
        } else if (lower.contains("overdue") || lower.contains("unavailable")) {
            pill.getStyleClass().add("pill-unavail"); // Changed to pill-unavail
        }
        return pill;
    }

    // ── Sidebar Section Label ──
    public static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("nav-section");
        return l;
    }
}