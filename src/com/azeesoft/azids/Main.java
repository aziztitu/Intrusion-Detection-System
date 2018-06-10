package com.azeesoft.azids;

import com.azeesoft.azids.controllers.CaptureController;
import com.azeesoft.azids.controllers.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    MainController mainController;

    public static CaptureController captureController;

    public static String selectedDevString="";

    public static Stage mainStage;
    @Override
    public void start(Stage primaryStage) throws Exception{
        JNetWrapper.init();
        mainStage=primaryStage;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("res/main_window.fxml"));
        Parent root = fxmlLoader.load();
        mainController = fxmlLoader.getController();
        mainStage.setTitle("IDS");
        mainStage.setScene(new Scene(root));
        mainStage.show();
    }


    public static void main(String[] args) {
        launch(args);


    }
}
