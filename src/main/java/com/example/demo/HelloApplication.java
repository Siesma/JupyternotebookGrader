package com.example.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Parent root = loader.load();

        // Pass CLI args to controller
        HelloController controller = loader.getController();
        controller.initializeWithArgs(getParameters().getRaw());

        stage.setTitle("Notebook grader");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("dark-theme.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

}
