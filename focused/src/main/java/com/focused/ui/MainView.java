package com.focused.ui;

import javafx.stage.Stage;

/**
 * MainView — owns the entire JavaFX UI.
 *
 * Only this class (and classes in the ui/ package) are allowed
 * to import JavaFX. The core and platform layers never touch the UI.
 *
 * Session 4 will implement the full interface.
 * For now this stub compiles and holds the correct structure.
 */
public class MainView {

    private final Stage stage;

    public MainView(Stage stage) {
        this.stage = stage;
    }

    /**
     * Builds and shows the main window.
     * Session 4 will implement this.
     */
    public void show() {
        // TODO: implement in Session 4
        stage.setTitle("Focused");
        stage.show();
    }
}
