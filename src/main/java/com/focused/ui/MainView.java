package com.focused.ui;

import com.focused.controll.SessionController;
import com.focused.config.AppConfig;
import com.focused.platform.WindowManager;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.awt.PopupMenu;
import java.awt.MenuItem;
import java.util.List;
import java.util.Objects;

/**
 * MainView — owns the entire JavaFX scene graph.
 *
 * Design decisions made here:
 *
 * 1. UNDECORATED stage — we draw our own title bar and close button.
 *    This gives us a borderless modern window. Drag-to-move is implemented
 *    manually via mouse event handlers on the title bar region.
 *
 * 2. CSS stylesheet — all visual styling is in styles.css (resources/).
 *    MainView only assigns style classes. No inline style strings.
 *
 * 3. StackPane root — idle view and active view are stacked. We toggle
 *    visibility between them instead of rebuilding the scene graph.
 *    This is more performant than swapping children — the nodes stay
 *    in memory, only their visibility bit changes.
 *
 * 4. System tray — AWT tray, not JavaFX. AWT callbacks fire on the AWT
 *    Event Dispatch Thread. All UI updates from tray callbacks go through
 *    Platform.runLater() to get back to the JavaFX Application Thread.
 *
 * What this class never does:
 *   - Call WindowManager directly (except refreshWindowList — see note there)
 *   - Read Session fields directly (only via onTick callback)
 *   - Contain session logic or timing logic
 */
public class MainView {

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final Stage             stage;
    private final SessionController controller;
    private final AppConfig         config;
    private final WindowManager     windowManager;

    // ── UI state fields ───────────────────────────────────────────────────────
    // Only the nodes we need to read or update after initial construction.
    // Nodes that are write-once (built and never touched again) stay local
    // to the build method that creates them.

    private ListView<String> windowList;
    private TextField        keywordField;
    private ToggleGroup      durationGroup;
    private Spinner<Integer> customSpinner;
    private Label            statusPill;
    private Label            lockedWindowLabel;
    private Label            countdownLabel;
    private Arc              progressArc;
    private VBox             idleView;
    private VBox             activeView;

    // ── Drag support ──────────────────────────────────────────────────────────
    // We track the mouse offset from the window's top-left corner
    // so the window moves relative to where the user grabbed it,
    // not jumping to center the window on the cursor.

    private double dragOffsetX;
    private double dragOffsetY;

    // ── Constructor ───────────────────────────────────────────────────────────

    public MainView(Stage stage, SessionController controller,
                    AppConfig config, WindowManager windowManager) {
        this.stage         = stage;
        this.controller    = controller;
        this.config        = config;
        this.windowManager = windowManager;
    }

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Called once from Main.start(). Builds everything and shows the window.
     *
     * Order matters:
     *   1. Stage style must be set BEFORE the scene is set.
     *      After show() is called, initStyle() throws IllegalStateException.
     *      Reference: https://docs.oracle.com/javase/8/javafx/api/javafx/stage/Stage.html#initStyle
     *   2. Callbacks must be registered before any session can start.
     *   3. Window list is populated last — it's the most expensive operation.
     */
    public void show() {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);
        stage.setAlwaysOnTop(false);

        buildScene();
        registerCallbacks();
        setupSystemTray();
        refreshWindowList();

        stage.show();
    }

    // ── Scene construction ────────────────────────────────────────────────────

    private void buildScene() {
        idleView   = buildIdleView();
        activeView = buildActiveView();
        activeView.setVisible(false);

        // StackPane layers children — last child is on top.
        // Both views exist simultaneously; only one is visible.
        StackPane root = new StackPane(idleView, activeView);

        Scene scene = new Scene(root, 400, 500);

        // Load our CSS file from the resources/ folder.
        // getResource() searches the classpath — Maven puts resources/ on the classpath.
        // Objects.requireNonNull gives a clear error if the file is missing
        // instead of a silent NullPointerException later.
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/styles.css"),
                        "styles.css not found in resources/"
                ).toExternalForm()
        );

        stage.setScene(scene);
    }

    // ── Idle view ─────────────────────────────────────────────────────────────

    private VBox buildIdleView() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(0, 24, 24, 24));

        view.getChildren().addAll(
                buildTitleBar(),
                buildWindowSection(),
                buildKeywordSection(),
                buildDurationSection(),
                buildLockButton()
        );
        return view;
    }

    /**
     * Custom title bar — replaces the OS title bar we removed with UNDECORATED.
     *
     * Contains:
     *   - App name (left)
     *   - Status pill (center-right)
     *   - Close button (far right)
     *
     * Drag-to-move is wired on this bar. Why only the title bar and not
     * the whole window? Because interactive controls (ListView, buttons)
     * need their own mouse events. Wiring drag on the root would intercept
     * clicks on those controls.
     */
    private HBox buildTitleBar() {
        Label appName = new Label("{Focused}");
        appName.getStyleClass().add("app-title");

        statusPill = new Label("IDLE");
        statusPill.getStyleClass().add("pill-idle");

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("close-button");
        closeBtn.setOnAction(e -> handleClose());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, appName, spacer, statusPill, closeBtn);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("title-bar");

        // ── Drag to move ──────────────────────────────────────────────────────
        // onMousePressed: record where inside the title bar the user clicked.
        // onMouseDragged: move the stage so that point stays under the cursor.
        //
        // e.getScreenX/Y() = cursor position in screen coordinates
        // stage.getX/Y()   = window top-left in screen coordinates
        // The offset is the gap between them — stays constant during drag.

        bar.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });

        bar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        return bar;
    }

    private VBox buildWindowSection() {
        Label label = new Label("My open windows");
        label.getStyleClass().add("section-label");

        windowList = new ListView<>();
        windowList.setPrefHeight(130);
        windowList.getStyleClass().add("window-list");

        // When the user selects a window, auto-fill the keyword field
        // with the full title. They can then trim it to just the app name.
        // This is a UX decision — saves typing for the common case.
        windowList.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && keywordField != null) {
                        keywordField.setText(newVal);
                    }
                });

        Button refreshBtn = new Button("⟳  Refresh");
        refreshBtn.getStyleClass().add("small-button");
        refreshBtn.setOnAction(e -> refreshWindowList());

        VBox section = new VBox(8, label, windowList, refreshBtn);
        return section;
    }

    private VBox buildKeywordSection() {
        Label label = new Label("Lock keyword");
        label.getStyleClass().add("section-label");

        Label hint = new Label(
                "Any window that contain this keyword title will stays open."
        );
        hint.getStyleClass().add("hint-label");
        hint.setWrapText(true);

        keywordField = new TextField();
        keywordField.setPromptText("e.g.   IntelliJ or Spotify");
        keywordField.getStyleClass().add("keyword-field");

        return new VBox(6, label, hint, keywordField);
    }

    /**
     * Duration section — four preset ToggleButtons and a custom Spinner.
     *
     * Key design decision: store the duration in seconds as userData on each
     * ToggleButton. getLockDurationSeconds() reads it back with a cast.
     * This avoids a parallel Map<ToggleButton, Integer> and keeps the data
     * co-located with the button that owns it.
     *
     * The Spinner deselects all presets when changed — mutual exclusion
     * between presets and custom is enforced by this listener, not by
     * ToggleGroup (ToggleGroup only manages its own buttons).
     */
    private VBox buildDurationSection() {
        Label label = new Label("Duration");
        label.getStyleClass().add("section-label");

        durationGroup = new ToggleGroup();

        int[]    seconds = {15 * 60, 25 * 60, 45 * 60, 60 * 60};
        String[] labels  = {"15m",   "25m",   "45m",   "1h"   };

        HBox presetRow = new HBox(8);
        for (int i = 0; i < seconds.length; i++) {
            ToggleButton btn = new ToggleButton(labels[i]);
            btn.getStyleClass().add("duration-toggle");
            btn.setToggleGroup(durationGroup);
            btn.setUserData(seconds[i]);
            btn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(btn, Priority.ALWAYS);
            presetRow.getChildren().add(btn);
            if (i == 1) btn.setSelected(true); // 25m default
        }

        Label customLabel = new Label("Customized Duration (min):");
        customLabel.getStyleClass().add("hint-label");

        customSpinner = new Spinner<>(1, 480, 25);
        customSpinner.setEditable(true);
        customSpinner.setPrefWidth(90);
        // Deselect presets when user types a custom value
        customSpinner.valueProperty().addListener(
                (obs, o, n) -> durationGroup.selectToggle(null)
        );

        HBox customRow = new HBox(10, customLabel, customSpinner);
        customRow.setAlignment(Pos.CENTER_LEFT);

        return new VBox(10, label, presetRow, customRow);
    }

    private Button buildLockButton() {
        Button btn = new Button("🔒   Get yourself lock");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("lock-button");
        btn.setOnAction(e -> handleLockAction());
        return btn;
    }

    // ── Active view ───────────────────────────────────────────────────────────

    /**
     * The active view shown during a session.
     *
     * Layout from top to bottom:
     *   lock emoji → locked window name → progress ring → countdown → end button
     *
     * The progress ring is built from two Arc nodes:
     *   - track arc  : full circle, dim color, static
     *   - progress arc: sweeps from 0 to -360 as session runs
     *
     * Why two arcs instead of a ProgressIndicator?
     * JavaFX's built-in ProgressIndicator has limited styling support.
     * Custom arcs give us full control over stroke width, color, and
     * line caps with no hacks needed.
     */
    private VBox buildActiveView() {
        VBox view = new VBox(16);
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(40, 24, 40, 24));
        view.getStyleClass().add("active-overlay");

        Label lockEmoji = new Label("🔐");
        lockEmoji.setStyle("-fx-font-size: 44px;");

        lockedWindowLabel = new Label("—");
        lockedWindowLabel.getStyleClass().add("locked-title");
        lockedWindowLabel.setWrapText(true);
        lockedWindowLabel.setAlignment(Pos.CENTER);
        lockedWindowLabel.setMaxWidth(340);

        Pane ringPane   = buildProgressRing();

        countdownLabel = new Label("00:00");
        countdownLabel.getStyleClass().add("countdown-label");

        Label remainingLabel = new Label("Remaining time");
        remainingLabel.getStyleClass().add("remaining-label");

        Button endBtn = new Button("End your session early");
        endBtn.getStyleClass().add("end-button");
        endBtn.setOnAction(e -> controller.stop());

        view.getChildren().addAll(
                lockEmoji, lockedWindowLabel,
                ringPane, countdownLabel, remainingLabel,
                endBtn
        );
        return view;
    }

    /**
     * Builds the circular progress ring.
     *
     * Arc geometry:
     *   centerX/Y : bound to the Pane's center via property binding
     *   radiusX/Y : 56 — size of the ring
     *   startAngle: 90 — 12 o'clock position (JavaFX 0° is 3 o'clock)
     *   length    : updated each tick — negative = clockwise
     *
     * Property binding (centerXProperty().bind(...)) means the arc
     * automatically recenters if the Pane is ever resized. This is more
     * robust than hardcoding pixel values.
     * Reference: https://docs.oracle.com/javase/8/javafx/api/javafx/beans/binding/package-summary.html
     */
    private Pane buildProgressRing() {
        Arc track = new Arc(0, 0, 56, 56, 90, 360);
        track.setType(ArcType.OPEN);
        track.setStroke(Color.web("#1e1e2e"));
        track.setStrokeWidth(7);
        track.setFill(Color.TRANSPARENT);
        track.setStrokeLineCap(StrokeLineCap.ROUND);

        progressArc = new Arc(0, 0, 56, 56, 90, 0);
        progressArc.setType(ArcType.OPEN);
        progressArc.setStroke(Color.web("#ff4628"));
        progressArc.setStrokeWidth(7);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStrokeLineCap(StrokeLineCap.ROUND);

        Pane pane = new Pane(track, progressArc);
        pane.setPrefSize(130, 130);

        // Bind arc centers to pane center — responsive to any layout changes
        track.centerXProperty().bind(pane.widthProperty().divide(2));
        track.centerYProperty().bind(pane.heightProperty().divide(2));
        progressArc.centerXProperty().bind(pane.widthProperty().divide(2));
        progressArc.centerYProperty().bind(pane.heightProperty().divide(2));

        return pane;
    }

    // ── Controller callbacks ──────────────────────────────────────────────────

    /**
     * Registers callbacks with the controller.
     *
     * These are the ONLY way the View learns about session state changes.
     * The View never polls the controller — it reacts to events.
     * This is the Observer pattern — push-based, not pull-based.
     *
     * Observer pattern reference: GoF Design Patterns, Chapter 5.
     */
    private void registerCallbacks() {
        controller.setOnTick(session -> {
            // Called on the FX thread (Timeline fires there) — safe to update UI
            int s = session.getSecondsLeft();
            countdownLabel.setText(
                    String.format("%02d:%02d", s / 60, s % 60)
            );
            // Negative arc length = clockwise sweep
            progressArc.setLength(-360.0 * session.getProgress());
        });

        controller.setOnDone(() -> {
            // Called on FX thread (Task.setOnSucceeded guarantees this)
            switchToIdleState();
            stage.setIconified(false);
            stage.show();
            stage.toFront();
            statusPill.setText("IDLE");
            statusPill.getStyleClass().setAll("pill-idle");
        });
    }

    // ── User action handlers ──────────────────────────────────────────────────

    /**
     * Validates inputs and starts the session.
     *
     * Validation order matters — check the most likely mistake first
     * (empty keyword) before the less likely one (no duration).
     * This minimises the number of error dialogs a user sees in the
     * normal flow.
     */
    private void handleLockAction() {
        String keyword = keywordField.getText().trim();
        if (keyword.isEmpty()) {
            showError("Enter a keyword to lock to.\n\n" +
                    "Example: type  VS Code  to lock to your editor.");
            return;
        }

        int seconds = getLockDurationSeconds();
        if (seconds <= 0) {
            showError("Select a duration or enter a custom number of minutes.");
            return;
        }

        String targetWindow = windowList.getSelectionModel().getSelectedItem();
        if (targetWindow == null) targetWindow = keyword;

        // Persist for next session
        config.set(AppConfig.KEY_LAST_WINDOW,   keyword);
        config.set(AppConfig.KEY_LAST_DURATION, seconds);

        // Update the locked window label in the active view
        lockedWindowLabel.setText(keyword);

        // Reset progress ring to empty before starting
        progressArc.setLength(0);
        countdownLabel.setText(
                String.format("%02d:%02d", seconds / 60, seconds % 60)
        );

        // Switch visual state
        switchToActiveState();

        // Minimize our window — the user committed to the lock.
        // Platform.runLater defers this until after the current
        // FX pulse completes, ensuring the scene graph update
        // (switchToActiveState) finishes rendering before we hide.
        Platform.runLater(() -> stage.setIconified(true));

        controller.start(targetWindow, keyword, seconds);
    }

    private void handleClose() {
        // If a session is active, stop it before exiting
        // so windows are restored and not left in a minimized state.
        controller.stop();
        Platform.exit();
        System.exit(0); // ensures AWT tray thread also exits
    }

    // ── State switching ───────────────────────────────────────────────────────

    private void switchToActiveState() {
        idleView.setVisible(false);
        activeView.setVisible(true);
        statusPill.setText("ACTIVE");
        statusPill.getStyleClass().setAll("pill-active");
    }

    private void switchToIdleState() {
        activeView.setVisible(false);
        idleView.setVisible(true);
    }

    // ── System tray ───────────────────────────────────────────────────────────

    /**
     * Sets up a system tray icon so the user can interact with Focused
     * while it is minimized during an active session.
     *
     * Critical threading rule:
     *   AWT tray event listeners fire on the AWT Event Dispatch Thread (EDT).
     *   All JavaFX UI operations MUST be wrapped in Platform.runLater()
     *   to marshal back to the JavaFX Application Thread.
     *   Violating this causes silent failures or IllegalStateExceptions.
     *
     * We call Platform.setImplicitExit(false) so that closing the main
     * window does not terminate the application — the tray icon keeps it alive.
     * The user explicitly exits via the tray menu or the close button.
     *
     * Reference: https://docs.oracle.com/javase/8/javafx/interoperability-tutorial/fx_interop.htm
     */
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;

        // Tell JavaFX not to exit when the last window is hidden
        Platform.setImplicitExit(false);

        // Build a minimal 16x16 tray icon — a filled orange circle.
        // Production apps would load a real .png from resources/.
        // We generate programmatically to avoid file dependencies right now.
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        g.setColor(java.awt.Color.decode("#ff4628"));
        g.fillOval(2, 2, 12, 12);
        g.dispose();

        // AWT popup menu — appears on right-click of the tray icon
        PopupMenu popup = new PopupMenu();

        MenuItem showItem = new MenuItem("Open Focused");
        showItem.addActionListener(e ->
                // AWT EDT → must marshal to FX thread
                Platform.runLater(() -> {
                    stage.setIconified(false);
                    stage.show();
                    stage.toFront();
                })
        );

        MenuItem exitItem = new MenuItem("Quit");
        exitItem.addActionListener(e -> {
            controller.stop(); // restore windows before exit
            Platform.exit();
            System.exit(0);
        });

        popup.add(showItem);
        popup.addSeparator();
        popup.add(exitItem);

        TrayIcon trayIcon = new TrayIcon(img, "Focused", popup);
        // Double-click the tray icon to show the window
        trayIcon.addActionListener(e ->
                Platform.runLater(() -> {
                    stage.setIconified(false);
                    stage.show();
                    stage.toFront();
                })
        );

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            System.err.println("[MainView] Could not add tray icon: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Populates the window list from the OS.
     *
     * Design note: this calls WindowManager directly, which technically
     * bypasses the controller. This is acceptable here because:
     *   1. It's a read-only OS query, not a session operation.
     *   2. The controller's responsibility is session lifecycle, not
     *      window enumeration for UI display purposes.
     * In a larger app, you'd add a dedicated query method on the controller.
     * For our scope, direct access is the pragmatic choice.
     *
     * Future improvement: run this on a background thread and populate
     * the list via Platform.runLater() to avoid any startup jank.
     */
    private void refreshWindowList() {
        List<String> titles = windowManager.getVisibleWindows()
                .stream()
                .map(WindowManager.WindowInfo::title)
                .toList();

        windowList.setItems(FXCollections.observableArrayList(titles));

        // Restore last-used keyword
        String last = config.get(AppConfig.KEY_LAST_WINDOW, "");
        if (!last.isEmpty()) keywordField.setText(last);
    }

    /**
     * Reads the selected duration in seconds.
     *
     * Priority order:
     *   1. If a preset ToggleButton is selected → use its userData (seconds)
     *   2. Otherwise → use the custom Spinner value (minutes → convert to seconds)
     *
     * getUserData() returns Object — we cast to Integer.
     * This is safe because we set Integer values in buildDurationSection().
     * The cast would only fail if someone added a ToggleButton with non-Integer
     * userData — which we control completely.
     */
    private int getLockDurationSeconds() {
        Toggle selected = durationGroup.getSelectedToggle();
        if (selected != null) {
            return (int) selected.getUserData();
        }
        return customSpinner.getValue() * 60;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.initOwner(stage);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}