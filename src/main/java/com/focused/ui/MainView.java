package com.focused.ui;

import com.focused.config.AppConfig;
import com.focused.controll.SessionController;
import com.focused.platform.WindowManager;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.StageStyle;

import java.util.List;


/**
 * MainView — owns the entire JavaFX UI.
 * No other class need access
 * Understand user actions and update itself base on the controller
 */
public class MainView {
    //dependencies
    private final Stage stage;
    private final SessionController controller;
    private final AppConfig config;
    private final WindowManager windowManager;

    // UI nodes for the construction
    private ListView<String> windowList;
    private TextField keywordField;
    private Label countdownLabel;
    private Arc progressArc;
    private StackPane root;
    private VBox idleView;
    private VBox activeView;

    //constructor
    public MainView(Stage stage, SessionController controller, AppConfig config, WindowManager windowManager) {
        this.stage      = stage;
        this.controller = controller;
        this.windowManager = windowManager;
        this.config     = config;
    }

    /**
     * Builds and shows the main window.
     */
    public void show() {
        buildScene();
        registerCallback();
        refreshWindowList();

        stage.setTitle("{Focused}");
        stage.setWidth(420);
        stage.setHeight(580);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * THE BUILDSCENE CLASS BUILT THE IDLE AND ACTIVE UI SEPARATLY
     * AND SHOW ONE AT THE TIME USING STACKPANE
     * */
    public void buildScene(){
        idleView = buildIdleView();
        activeView = buildActiveView();
        activeView.setVisible(false); // start in idle sttate

        root = new StackPane(idleView, activeView);
        root.setStyle("-fx-background-color:#0c0c14;");

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    public VBox buildIdleView(){
        VBox view = new VBox (16);
        view.setPadding(new Insets(28));
        view.setAlignment(Pos.TOP_LEFT);

        view.getChildren().addAll(
                buildHeader(),
                buildWindowSection(),
                buildKeywordSection(),
                buildDurationSection(),
                buildLockButton()
        );
        return view;
    }
    private Label buildHeader(){
        Label title = new Label("{Focused}");
        title.setStyle(
                "-fx-text-fill: #e8e4d9;;" +
                "-fx-font-size: 30;"+
                "-fx-font-weight: bold;"
        );
        return title;
    }
    private VBox buildWindowSection(){
        Label sectionLabel = sectionLabel("My open windows");

        //Populated list with visible windows titles
        windowList = new ListView<>();
        windowList.setPrefHeight(150);
        windowList.setStyle(
                "-fx-background-color: #16161f;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-radius: 10;" +
                        "-fx-text-fill: #e8e4d9;"
        );

        //when an user select a windows,we fill the keyword field
        windowList.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if(newVal != null && keyword != null){
                        //use the full title as the default keyword
                         //user can edit it in the keyborad field
                        keywordField.setText(newVal);
                    }
        });
        Button refreshBtn = smallButton("⟳  Refresh");
        refreshBtn.setOnAction(e->refreshWindowList());

        VBox section = new VBox (8, sectionLabel, windowList, refreshBtn);

        return section;
    }

    private VBox buildKeywordSection (){
        Label sectionLabel = sectionLabel ("Lock Keyword");

        //explaination of the field to the user
        Label hint = new Label("If you have any window with this title word, it will stay open.");
        hint.setStyle("-fx-text-fill: #555; -fx-font-size: 11;");
        hint.setWrapText(true);

        keywordField = new TextField();
        keywordField.setPromptText("e.g Chrome or Vs Code");
        keywordField.setStyle(
                "-fx-background-color: #16161f;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #e8e4d9;" +
                        "-fx-prompt-text-fill: #444;" +
                        "-fx-font-size: 13;" +
                        "-fx-padding: 10 14 10 14;"
        );

        return new VBox(8, sectionLabel, hint, keywordField);
    }

    /**
     * Duration section — four preset buttons plus a custom spinner.
     * We use a ToggleGroup so only one preset can be selected at a time.
     * ToggleGroup is JavaFX's built-in mutual exclusion for ToggleButtons.
     */
    private VBox buildDurationSection(){
        Label sectionLabel = sectionLabel ("Duration");
        ToggleGroup group =new ToggleGroup();
        int [] presets= {15 * 60, 25 * 60, 45 * 60, 60 * 60};
        String[] labels    = {"15m", "25m", "45m", "1h"};
        HBox presetRow     = new HBox(8);

        for (int i = 0; i < presets.length; i++) {
            final int duration = presets[i];
            ToggleButton btn   = new ToggleButton(labels[i]);
            btn.setToggleGroup(group);
            btn.setPrefWidth(80);
            btn.setPrefHeight(36);
            btn.setUserData(duration); // store seconds as metadata
            btn.setStyle(toggleStyle(false));
            btn.selectedProperty().addListener((obs, o, selected) ->
                    btn.setStyle(toggleStyle(selected))
            );
            HBox.setHgrow(btn, Priority.ALWAYS);
            presetRow.getChildren().add(btn);

            // Default: select 25 minutes
            if (i == 1) btn.setSelected(true);
        }


    }
}
