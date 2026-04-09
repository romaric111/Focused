package com.focused;

import com.focused.config.AppConfig;
import com.focused.core.SessionContrroller;
import com.focused.platform.WindowManager;
import javafx.application.Application;
import javafx.stage.Stage;
import com.focused.ui.MainView;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Instantiation order matters:
        // 1. Infrastructure first — no dependencies
        WindowManager windowManager = new WindowManager();
        AppConfig config        = new AppConfig();

        // 2. Controller second — depends on infrastructure
        SessionContrroller  controller    = new SessionContrroller(windowManager);

        // 3. View last — depends on everything
        MainView view = new MainView(primaryStage, controller, config);
        view.show();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
