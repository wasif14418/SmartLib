package com.smartlib.ui.pages;

import com.smartlib.service.LibraryService;
import com.smartlib.ui.UiFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;

public class AnalyticsPage {

    private final LibraryService svc = LibraryService.getInstance();

    public Node build() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(22, 24, 22, 24));
        page.getStyleClass().add("page");

        // Top stats
        HBox row = new HBox(14);
        VBox s1 = UiFactory.statCard("Total Borrows This Month", "89",  null, "blue");
        VBox s2 = UiFactory.statCard("Most Borrowed",  "Data Structures", null, "green");
        VBox s3 = UiFactory.statCard("On-Time Return Rate", "87%", null, "green");
        for (VBox v : new VBox[]{s1,s2,s3}) HBox.setHgrow(v, Priority.ALWAYS);
        row.getChildren().addAll(s1, s2, s3);

        // Most borrowed
        VBox borrowChart = UiFactory.card("📊  Most Borrowed Books");
        VBox bBody = new VBox(10);
        bBody.setPadding(new Insets(18));
        int[][] borrowData = {{34},{28},{25},{20},{16},{12}};
        String[] borrowLabels = {
            "Data Structures & Algorithms","Operating Systems",
            "Java: The Complete Reference","Database Systems",
            "Computer Networks","Discrete Mathematics"
        };
        int maxB = 34;
        for (int i = 0; i < borrowLabels.length; i++) {
            bBody.getChildren().add(barRow(borrowLabels[i], borrowData[i][0], maxB, false));
        }
        borrowChart.getChildren().add(bBody);

        // Most reserved
        VBox reserveChart = UiFactory.card("📈  Frequently Reserved (Pre-bookings)");
        VBox rBody = new VBox(10);
        rBody.setPadding(new Insets(18));
        String[] rLabels = {"Operating Systems","Discrete Mathematics","Computer Networks"};
        int[]    rCounts = {18, 13, 9};
        int maxR = 18;
        for (int i = 0; i < rLabels.length; i++) {
            rBody.getChildren().add(barRow(rLabels[i], rCounts[i], maxR, true));
        }
        reserveChart.getChildren().add(rBody);

        page.getChildren().addAll(row, borrowChart, reserveChart);
        return page;
    }

    private HBox barRow(String label, int count, int max, boolean yellow) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("bar-label");
        lbl.setPrefWidth(220);
        lbl.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;");

        // Track
        StackPane track = new StackPane();
        track.setMinHeight(18);
        track.setStyle("-fx-background-color:#161d2e;-fx-background-radius:4;");
        HBox.setHgrow(track, Priority.ALWAYS);

        // Fill
        double pct = (double) count / max;
        Rectangle fill = new Rectangle();
        fill.setHeight(18);
        fill.setArcWidth(6);
        fill.setArcHeight(6);

        if (yellow) {
            fill.setFill(new LinearGradient(0,0,1,0,true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fbbf24")), new Stop(1, Color.web("#f59e0b"))));
        } else {
            fill.setFill(new LinearGradient(0,0,1,0,true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6ee7b7")), new Stop(1, Color.web("#34d399"))));
        }

        track.widthProperty().addListener((obs, o, w) -> fill.setWidth(w.doubleValue() * pct));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);

        Label cnt = new Label(String.valueOf(count));
        cnt.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;");
        cnt.setPrefWidth(36);
        cnt.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(lbl, track, cnt);
        return row;
    }
}
