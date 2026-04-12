package com.focused.ui;

import com.focused.config.AppConfig;
import com.focused.controll.SessionController;
import javafx.stage.Stage;

/**
 * MainView — owns the entire JavaFX UI.
 * No other class need access
 */
public class MainView {

    private final Stage stage;
    private final SessionController controller;
    private final AppConfig config;

    public MainView(Stage stage, SessionController controller, AppConfig config) {
        this.stage      = stage;
        this.controller = controller;
        this.config     = config;
    }
    /**
     * Builds and shows the main window.
     * Session 4 will implement this.
     */
    public void show() {
        // TODO: soon
        stage.setTitle("Focused");
        stage.show();
    }
}
