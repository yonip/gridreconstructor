package com.gridreconstructor;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortList;

/**
 * A controller to control the intro screen for the app. <br/>
 * Currently allows for the selection of the port from one of the ones available.
 */
public class Controller {
    @FXML
    private Button okButton;
    @FXML
    private ListView<String> portlist;

    /**
     * an instance of the FXML loader, so state can be reached later, if needed (currently can be a local variable)
     */
    private FXMLLoader reconFxmlLoader;

    /**
     * an object to connect to the serial port to get information from the arduino
     */
    static SerialPort port;

    /**
     * default bits per second (should match what was declared on the arduino)
     */
    private static final int DATA_RATE = 9600;

    /**
     * method called when this scene is initialized. when this is called, all @FXML variables should be instantiated and
     * can now be configured
     */
    @FXML
    public void initialize() {
        // put the possible serial ports in the observable list so one can be selected to communicate with
        portlist.setItems(FXCollections.observableArrayList(SerialPortList.getPortNames()));
    }

    /**
     * function called when the ok button is pressed (set in the fxml)<br/>
     * instantiates the port to communicate with and then sets up the grid reconstructor on screen with its controller
     * (a {@link GridReconstructor} object) as a listener to serial events.
     */
    @FXML
    public void okPressed() {
        String portId = portlist.getSelectionModel().getSelectedItem();
        //if (portId == null || portId.isEmpty()) {
        //    return;
        //}
        port = null;
        try {
            // open serial port
            if (portId != null && !portId.isEmpty()) {
                port = new SerialPort(portId);
                port.openPort();

                // set port parameters
                port.setParams(DATA_RATE,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
            }
            // setup the data presentation screen
            reconFxmlLoader = new FXMLLoader(getClass().getResource("/reconstructors.fxml"));
            ((Stage)okButton.getScene().getWindow()).setScene(new Scene(reconFxmlLoader.load()));
            GridReconstructor gr = reconFxmlLoader.<GridReconstructor>getController();
            if (port != null) {
                gr.setInput(port);
                port.addEventListener(gr);
            }
            gr.postInit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
