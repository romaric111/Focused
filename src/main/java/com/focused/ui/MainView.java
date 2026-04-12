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
import javafx.scene.Node;
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
    private ToggleGroup durationGroup;
    private Spinner<Integer> customSpinner;

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
                    if(newVal != null && keywordField != null){
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
        durationGroup = new ToggleGroup();
        customSpinner = new Spinner<>(1,480,25);
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
        //custom duration spinner from 1 to 480 min
        Label customLabel = new Label("Custom(min):");
        customLabel. setStyle("-fx-text-fill: #555; -fx-font-size: 11;");
        customSpinner.setEditable(true);
        customSpinner.setPrefWidth(100);
        customSpinner.setStyle("-fx-background-color: #16161f;");

        //when the user select custom duration it deselect les presets duration
        customSpinner.valueProperty().addListener((obs, o, n) ->
                group.selectToggle(null)
        );

        // Tag the spinner on the group so getLockDuration() can read it
        group.setUserData(customSpinner);

        HBox customRow =new HBox(10,customLabel, customSpinner);
        customRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(10, sectionLabel, presetRow, customRow);
    }
    private Button buildLockButton(){
        Button btn = new Button(" Get Focus now ");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(50);
        btn .setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #ff4628, #ff7a3d);" +
                        "-fx-background-radius: 14;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnAction(e -> handleLockAction());
        return btn;
    }

    //Active View
    private VBox buildActiveView(){
        VBox view = new VBox(20);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(40));
        view.setStyle("-fx-background-color: #0c0c14;");

        Label lockIcon = new Label("🔐");
        lockIcon.setStyle("fx-font-size: 52;");

        Label lockedLabel = new Label("Your focus session is now active");
        lockedLabel.setStyle(
                "-fx-text-fill: #ff6040;" +
                        "-fx-font-size: 20;" +
                        "-fx-font-weight: bold;"
        );

        // Progress ring — Arc sweeps from 0 to -360 as session progresses
        // Why Arc and not a ProgressBar?
        // A circular arc matches the "focus timer" mental model better.
        // ProgressBar is horizontal — fine for loading, wrong feel for a countdown.
        Arc track = new Arc(0, 0, 60, 60, 90, 360);
        track.setType(ArcType.OPEN);
        track.setStroke(Color.web("#1e1e2e"));
        track.setStrokeWidth(7);
        track.setFill(Color.TRANSPARENT);

        progressArc = new Arc(0, 0, 60, 60, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setStroke(Color.web("#ff4628"));
        progressArc.setStrokeWidth(7);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);
        progressArc.setFill(Color.TRANSPARENT);

        Pane ringPane = new Pane(track, progressArc);
        ringPane.setPrefSize(140, 140);
        // Center the arcs in the pane
        track.centerXProperty().bind(ringPane.widthProperty().divide(2));
        track.centerYProperty().bind(ringPane.heightProperty().divide(2));
        progressArc.centerXProperty().bind(ringPane.widthProperty().divide(2));
        progressArc.centerYProperty().bind(ringPane.heightProperty().divide(2));

        countdownLabel = new Label("00:00");
        countdownLabel.setStyle(
                "-fx-text-fill: #e8e4d9;" +
                        "-fx-font-size: 52;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-family: monospace;"
        );

        Label remainingLabel = new Label("Remaining Time");
        remainingLabel.setStyle(
                "-fx-text-fill: #333;" +
                        "-fx-font-size: 10;" +
                        "-fx-font-family: monospace;" +
                        "-fx-letter-spacing: 2;"
        );

        Button endBtn = new Button("End session early");
        endBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #2a2a3a;" +
                        "-fx-border-radius: 100;" +
                        "-fx-background-radius: 100;" +
                        "-fx-text-fill: #555;" +
                        "-fx-font-size: 11;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 24 8 24;"
        );
        endBtn.setOnAction(e -> controller.stop());

        view.getChildren().addAll(
                lockIcon, lockedLabel, ringPane,
                countdownLabel, remainingLabel, endBtn
        );
        return view;
    }

    //COntrollers callback
    /**
     * Registers the two callbacks the controller will call during a session.
     * Called once, before show(). Must be set before any session can start.
     */
    private void registerCallback(){
        //onTick will be called every second on the Fx Thread, so it can update the UI directly
        controller.setOnTick(session -> {
            int s = session.getSecondsLeft();
            countdownLabel.setText(
                    String.format("%02d:%02d", s / 60, s % 60)
            );
            //Negative length will be clockwise sweep in javaFX arc
            progressArc.setLength(-360.0 * session.getProgress());
        });

        //onDone called on FX Thread after releaseAll() completes
        controller.setOnDone(() ->{
            idleView.setVisible(true);
            activeView.setVisible(false);
            //Restore stage visibility after locking done
            stage.show();
            stage.toFront();
        });
    }

    // User actions
    /**
     * Called when the user clicks "Get focus Now".
     * Validates inputs, saves last-used values to config,
     * switches to active view, then starts the session.
     */
    private void handleLockAction(){
        String keyword = keywordField.getText().trim();
        if(keyword.isEmpty()){
            showError("Enter a keyword to lock to.");
            return;
        }
        String selectedWindow = windowList.getSelectionModel().getSelectedItem();
        if(selectedWindow == null) selectedWindow = keyword;
        int seconds = getLockDurationSeconds();
        if(seconds <= 0){
            showError("You must select or enter a duration.");
            return;
        }

        // Persist last-used values so next session pre-fills them
        config.set(AppConfig.KEY_LAST_WINDOW,   keyword);
        config.set(AppConfig.KEY_LAST_DURATION, seconds);

        // Switch to active view
        idleView.setVisible(false);
        activeView.setVisible(true);

        // Minimize our own window — user committed to the lock
        // We use Platform.runLater to ensure the stage hide happens after
        // the scene graph update completes on the FX thread
        Platform.runLater(() -> stage.setIconified(true));

        // Start the session
        controller.start(selectedWindow, keyword, seconds);
    }

    // ── Helpers

    /**
     * Populates the window list with currently visible windows.
     * Pre-fills keyword with the last-used value from config if available.
     */
    private void refreshWindowList() {
        List<String> titles = windowManager.getVisibleWindows()
                .stream()
                .map(w -> w.title())
                .toList();

        windowList.setItems(FXCollections.observableArrayList(titles));

        // Restore last-used keyword from config
        String lastKeyword = config.get(AppConfig.KEY_LAST_WINDOW, "");
        if (!lastKeyword.isEmpty() && keywordField != null) {
            keywordField.setText(lastKeyword);
        }
    }
    /**
     * Reads the selected duration in seconds.
     * Prefers the ToggleGroup selection; falls back to the custom spinner.
     */
    @SuppressWarnings("unchecked")
    private int getLockDurationSeconds() {
        // This is attached to the group in buildDurationSection()
        // We recover it via getUserData() on the group
        Toggle selected =durationGroup.getSelectedToggle();
        if(selected != null){
            return (int) selected.getUserData();
        }
       // Fall back to custom spinner value(min to seconds)
        return customSpinner.getValue()*60;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle(
                "-fx-text-fill: #444;" +
                        "-fx-font-size: 10;" +
                        "-fx-font-family: monospace;" +
                        "-fx-letter-spacing: 2;"
        );
        return l;
    }

    private static Button smallButton(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #1e1e2e;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-text-fill: #555;" +
                        "-fx-font-size: 11;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 12 4 12;"
        );
        return b;
    }

    private static String toggleStyle(boolean selected) {
        return selected
                ? "-fx-background-color: rgba(255,70,40,0.15);" +
                "-fx-border-color: rgba(255,70,40,0.5);" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-text-fill: #ff6040;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;"
                : "-fx-background-color: #16161f;" +
                "-fx-border-color: #1e1e2e;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-text-fill: #888;" +
                "-fx-cursor: hand;";
    }





}
