package com.gridreconstructor;

import com.gridreconstructor.util.ReconImage;
import com.gridreconstructor.util.Util;
import javafx.fxml.FXML;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * A controller to control the scene displaying the image reconstructors. Also listens to serial events and sends data
 * to the reconstructors.
 */
public class GridReconstructor implements SerialPortEventListener {
    /**
     * the {@link ReconImage} for voltage. Will be assigned by the FXML loader.
     */
    @FXML
    private ReconImage voltage;

    /**
     * a BufferedReader to convert bytes to characters
     */
    private SerialPort input;

    /**
     * the number of vertical sensors, ie the number of columns in the image
     */
    private static final int IMG_WIDTH = 8;

    /**
     * the number of horizontal sensors, ie the number of rows in the image
     */
    private static final int IMG_HEIGHT = 8;

    /**
     * how many times to scale all images horizontally, to help with visibility
     */
    private static final int IMG_WIDTH_SCALE = 50;

    /**
     * how many times to scale all images vertically, to help with visibility
     */
    private static final int IMG_HEIGHT_SCALE = 50;

    /**
     * the number of samples to keep in a buffer before switching buffers and updating the image
     */
    private static final int BUFFER_SIZE = 1;

    /**
     * This array holds 2 "buffers" of samples, one to be read from the create images, the other to write samples onto.
     * Each buffer contains {@link #BUFFER_SIZE} samples, and each sample is an array of size {@link #IMG_WIDTH} +
     * {@link #IMG_HEIGHT}, and assumed the data from the vertical sensors is first (from left to right) and after which
     * are the horizontal sensors (from the top down). <br/><br/>
     * For examples, if IMG_HEIGHT was 4 and IMG_WIDTH was 4, each sample will look like this: <code>[0, 1,
     * 2, 3, 4, 5, 6, 7]</code> (where each number is the id of a sensor) and the program will recreate an image
     * assuming the sensors are arranged like so: <br/>
     * <code>0 1 2 3</code><br/>
     * <code>+-+-+-+ 4</code><br/>
     * <code>+-+-+-+ 5</code><br/>
     * <code>+-+-+-+ 6</code><br/>
     * <code>+-+-+-+ 7</code><br/>
     */
    private double[][][] buffers = new double[2][BUFFER_SIZE][IMG_WIDTH + IMG_HEIGHT];

    /**
     * the index of the buffer that is currently being written to. Add one and mod by two to get the buffer being used
     * for drawing
     */
    private int currentBuffer = 0;

    /**
     * the number of samples in the current buffer.
     */
    private int bufferSize = 0;

    /**
     * a StringBuilder to construct the input from the serial port into a string that can be parsed
     */
    private StringBuilder builder = new StringBuilder();

    /**
     * The JavaFX initialization method, which is called when the current scene is loaded. At this point we know all the
     * elements in the current scene have been instantiated, so we can now pass them relevant state.
     */
    @FXML
    public void initialize() {
        voltage.setContext("Voltage", IMG_WIDTH, IMG_HEIGHT, IMG_WIDTH_SCALE, IMG_HEIGHT_SCALE, Util.settings.optDouble("graphMax", 900),
                Util.settings.optDouble("graphMin", 550), 1, 1, false, 1000);
    }

    /**
     * sets the serial port to be used as input, since this object is instantiated by JavaFX and not by {@link Main}
     * @param input a SerialPort object to use to read from the serial port
     */
    public void setInput(SerialPort input) {
        this.input = input;
    }

    public void postInit() {
        voltage.postInit();
    }

    /**
     * gives the index of the buffer that isn't the current buffer.<br/>
     * can be used to easily swap buffers by setting {@link #currentBuffer} to the value retrieved from this method
     * @return the index of the buffer that isn't the current buffer
     */
    private int otherBuffer() {
        return (currentBuffer + 1) % 2;
    }

    /**
     * This method checks if the current buffer is filled. If it is, the buffers are swapped and the now "other" buffer
     * (that contains all the data from this sampling session) is averaged and decomposed into average row readings and
     * average column readings. These two sets are then sent to the {@link ReconImage}s that will then display the data
     * onto the screen.
     */
    private void checkBuffers() {
        if (bufferSize >= BUFFER_SIZE) {
            currentBuffer = otherBuffer(); // "swap" buffers
            // clear the new buffer
            buffers[currentBuffer] = new double[BUFFER_SIZE][IMG_WIDTH + IMG_HEIGHT];
            bufferSize = 0;
            double[] columns = new double[IMG_WIDTH];
            double[] rows = new double[IMG_HEIGHT];
            for (int i = 0; i < columns.length; i++) {
                columns[i] = avgColumn(buffers[otherBuffer()], i);
            }
            for (int i = 0; i < rows.length; i++) {
                rows[i] = avgColumn(buffers[otherBuffer()], i + columns.length);
            }
            // update the images using columns and rows
            // should probably send copies of the arrays
            voltage.update(columns, rows);
        }
    }

    /**
     * this is a utility method for averaging a column of a matrix.
     * @param matrix a 2D array
     * @param index the index of the column (zero indexed)
     * @return a double that is the average of all the values in the given column of the given matrix
     */
    private double avgColumn(double[][] matrix, int index) {
        double avg = 0;
        for (double[] r : matrix) {
            avg += r[index];
        }
        return avg / matrix.length;
    }

    /**
     * This method listens for a serial event. In this implementation, it builds the message from the serial port using
     * an internal {@link StringBuilder}. If a carriage return (<code>'\r'</code>) or a new line (<code>'\n'</code>) is
     * read from the serial, it is not added to the StringBuilder and instead passes the current string in the builder
     * to {@link #parseString(String)} and clears the builder. If the builder is empty, then nothing happens.
     * @param serialPortEvent an event passed by the jssc library detailing what serial event happened.
     */
    @Override
    public void serialEvent(SerialPortEvent serialPortEvent) {
        if (serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0) {
            // make sure we didn't somehow get here when the buffer is full
            try {
                byte[] bytes = input.readBytes();
                for (byte b : bytes) {
                    if ((b == '\r' || b == '\n')) {
                        if (builder.length() > 0) {
                            parseString(builder.toString());
                            builder.setLength(0);
                        }
                    } else {
                        builder.append((char)b);
                    }
                }
            } catch (SerialPortException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method takes a string, theoretically representing a reading sent over serial and parses it. The data is in
     * the string assumed to be delimited by commas (<code>','</code>). The sample is first checked for an accurate number
     * of readings ({@link #IMG_WIDTH} + {@link #IMG_HEIGHT}). If the number of readings is incorrect, a warning is printed
     * and the method stops there. <br/>
     * At this point the input is assumed to be correct, so the data is converted from a string to an array of doubles,
     * added to the current buffer, and {@link #bufferSize} is incremented. <br/>
     * The method also checks at the beginning and the end to see if the current buffer is full using {@link #checkBuffers()}
     * @param inputLine
     */
    private void parseString(String inputLine) {
        checkBuffers();
        //inputLine = "1," + inputLine;
        System.out.println(inputLine + " " + inputLine.length());
        String[] split = inputLine.split("\t");
        if (split.length != IMG_WIDTH + IMG_HEIGHT) {
            System.out.println("Wrong amount of readings from serial. Expected " + (IMG_WIDTH + IMG_HEIGHT) +
                    " but got " + split.length + ".");
            return;
        }
        // fill in the information
        for (int i = 0; i < split.length; i++) {
            buffers[currentBuffer][bufferSize][i] = Double.parseDouble(split[i]);
        }
        //System.out.println(Arrays.toString(buffers[currentBuffer][bufferSize]));
        // we used another slot in our buffer!
        bufferSize++;
        checkBuffers();
    }
}
