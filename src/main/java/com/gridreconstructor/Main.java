package com.gridreconstructor;

import com.gridreconstructor.util.Util;
import com.sun.javafx.application.LauncherImpl;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A class to launch the application. Currently constructs the intro screen, that simply allows for selecting the port
 * to communicate on. <br/>
 * This class also has a method that is called when the application is stopped and it closes the serial port.
 */
public class Main extends Application {

    public static void main(String[] args) {
        LauncherImpl.launchApplication(Main.class, args);
    }

    /**
     * The main entry point for all JavaFX applications.
     * The start method is called after the init method has returned,
     * and after the system is ready for the application to begin running.
     * <p>
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param primaryStage the primary stage for this application, onto which
     *                     the application scene can be set. The primary stage will be embedded in
     *                     the browser if the application was launched as an applet.
     *                     Applications may create other stages, if needed, but they will not be
     *                     primary stages and will not be embedded in the browser.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
        primaryStage.setScene(new Scene(fxmlLoader.load()));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (Controller.port != null) {
            Controller.port.closePort();
        }
        Util.close();
    }
}
