package com.focused;

import javafx.application.Application;
import javafx.stage.Stage;
import com.focused.ui.MainView;

/**
 * Entry point for Focused.
 *
 * This class does ONE thing: start JavaFX.
 * No logic, no UI construction, no config loading — all of that
 * belongs in dedicated classes. Keep this file clean forever.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        new MainView(primaryStage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
