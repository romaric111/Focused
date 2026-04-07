package com.focused;

import javafx.application.Application;
import javafx.stage.Stage;
import com.focused.ui.MainView;

/**
 * This class does ONE thing: start JavaFX.
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
