package com.gridreconstructor.util;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Created by yonipedersen on 8/11/16.
 */
public class Util {
    public static final String SETTINGS_PATH = "./config/config.json";
    public static final String LOGGING_PATH = "logging";
    public static final JSONObject settings = getObject(SETTINGS_PATH);
    private static final FileChooser fileChooser = new FileChooser();

    public static JSONObject getObject(String path) {
        //System.out.println(new File(".").getAbsoluteFile());
        Path p = new File(/*Util.class.getResource(path).toURI().getPath()*/path).toPath();

        if (!p.toFile().exists()) {
            System.out.println("Configuration file does not exist!");
            return new JSONObject();
        }

        try {
            return new JSONObject(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

    public static void writeObject(JSONObject object, String path) {
        try {
            Path p = new File(/*Util.class.getResource(path).toURI().getPath()*/path).toPath();

            if (!p.toFile().exists()) {
                System.out.println("Configuration file does not exist!");
                return;
            }
            Files.write(p, Arrays.asList(object.toString(2).split("\n")), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static synchronized File openSaveDialog(String title, Window window) {
        fileChooser.setTitle(title);
        return fileChooser.showSaveDialog(window);
    }

    public static void close() {
        writeObject(settings, SETTINGS_PATH);
    }
}
