package Gifreader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import Gifreader.GifReader;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.awt.Color;
import java.util.Arrays;

import javafx.stage.Stage;

public class Main {

//    @Override
//    public void start(Stage primaryStage) throws Exception{
//        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
//        primaryStage.setTitle("Gif reader");
//        primaryStage.setScene(new Scene(root, 300, 275));
//        primaryStage.show();
//    }

    public static void main(String[] args) {
        //launch(args);
        String fileName = "src/Gifreader/sample_4.gif";
        File file = new File(fileName);
        GifReader gifReader = new GifReader(file);
    }



}
