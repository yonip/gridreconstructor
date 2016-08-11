package com.gridreconstructor.util;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A class to help calculate and render an image
 * Created by yonipedersen on 7/30/16.
 */
public class ReconImage extends TabPane {
    /**
     * the label to put over the graph
     */
    private String name;

    /**
     * The color red packed into an int using the ARGB color format. <br/>
     * Used to make color code slightly more readable.
     */
    private static final int RED = 0xFFFF0000;
    /**
     * The color white packed into an int using the ARGB color format. <br/>
     * Used to make color code slightly more readable.
     */
    private static final int WHITE = 0xFFFFFFFF;
    /**
     * The color blue packed into an int using the ARGB color format. <br/>
     * Used to make color code slightly more readable.
     */
    private static final int BLUE = 0xFF0000FF;

    /**
     * The number of column sensors in the grid.
     */
    private int width;
    /**
     * The number of row sensors in the grid.
     */
    private int height;
    /**
     * How many pixels wide each column should appear in the image. <br/>
     * The image's total width is wscale * {@link #width}
     */
    private int wscale;
    /**
     * How many pixels tall each row should appear in the image. <br/>
     * The image's total height is hscale * {@link #height}
     */
    private int hscale;

    /**
     * The minimum value that should be plotted on the graph of each sensor.
     */
    private DoubleProperty min = new DoublePropertyBase() {
        @Override
        public Object getBean() {
            return this;
        }

        @Override
        public String getName() {
            return "min";
        }
    };
    /**
     * The maximum value that should be plotted on the graph of each sensor.
     */
    private DoubleProperty max = new DoublePropertyBase() {
        @Override
        public Object getBean() {
            return this;
        }

        @Override
        public String getName() {
            return "max";
        }
    };

    /**
     * How much weight should be given to the value read from each row sensor. Should probably be between 0 and 1.
     */
    private double rscale;
    /**
     * How much weight should be given to the value read from each column sensor. Should probably be between 0 and 1.
     */
    private double cscale;

    /**
     * The baseline values of each column sensor, in order, where index 0 is the leftmost column sensor, and index length-1
     * is the rightmost column sensor. <br/>
     * The length of this array will be declared to be {@link #width}
     */
    private double[] baselineCols;
    /**
     * The baseline values of each column sensor, in order, where index 0 is the topmost row sensor, and index length-1
     * is the bottom row sensor. <br/>
     * The length of this array will be declared to be {@link #height}
     */
    private double[] baselineRows;

    /**
     * A buffer containing the values read from the sensors in the past, where the last element of the list is the most
     * recent reading. <br/>
     * Each array that is an element of this list is in the usual format, where the size of the array matches the number
     * of row sensors (see {@link #height}, and the first value is the topmost row sensor, and the last is the bottommost
     * row sensor.
     */
    private LinkedList<double[]> rowBuffer;
    /**
     * A buffer containing the values read from the sensors in the past, where the last element of the list is the most
     * recent reading. <br/>
     * Each array that is an element of this list is in the usual format, where the size of the array matches the number
     * of column sensors (see {@link #width}, and the first value is the leftmost column sensor, and the last is the rightmost
     * column sensor.
     */
    private LinkedList<double[]> colBuffer;
    /**
     * The fraction of the image that is needed at minimum to trigger an "event" where the whole graph is displayed, the
     * graph is frozen for a second and the face is "angry".
     */
    private double significant = 5.0/64;
    /**
     * The amount of time the graph and face should be frozen for after an "event" (in milis)
     */
    private long delay;
    /**
     * When the last "event" occured (in milis)
     */
    private long imgPauseStart;
    /**
     * A representation of the grid that can easily be converted to colors, as each State is associate with a color.
     */
    private State[][] state;
    /**
     * The old representation of the grid, which just stored the assumed value of each pixel to be converted to a color
     * gradient, determined by {@link #min} and {@link #max}
     */
    private double[][] grid;
    /**
     * The drop that needs to be observed to rebaseline a sensor.
     */
    private double thresholdToRebasline = 2;
    /**
     * A reference to the ImageView in the fxml that will show the recreated image from the grid of sensors.
     */
    @FXML
    private ImageView gridImg;
    /**
     * A reference to the ImageView in the fxml that will show the color associated with a hard touch.
     */
    @FXML
    private ImageView hardImage;
    /**
     * A reference to the ImageView in the fxml that will show the color associated with a medium touch.
     */
    @FXML
    private ImageView mediumImage;
    /**
     * A reference to the ImageView in the fxml that will show the color associated with a soft touch.
     */
    @FXML
    private ImageView softImage;
    /**
     * A reference to the ImageView in the fxml that will show the color associated with no touch.
     */
    @FXML
    private ImageView noneImage;
    /**
     * A reference to the Text in the fxml that will show the name in of this object, as set in {@link #name}.
     */
    @FXML
    private Text title;
    /**
     * A reference to the Text in the fxml that will show what the {@link #redSlider} is set to.
     */
    @FXML
    private Text redText;
    /**
     * A reference to the Text in the fxml that will show what the {@link #greenSlider} is set to.
     */
    @FXML
    private Text greenText;
    /**
     * A reference to the Text in the fxml that will show what the {@link #blueSlider} is set to.
     */
    @FXML
    private Text blueText;
    /**
     * A reference to the Slider in the fxml that will dictate what difference from the baseline will be counted as a
     * {@link State#HARD} value, and therefore appear to be red.
     */
    @FXML
    private Slider redSlider;
    /**
     * A reference to the Slider in the fxml that will dictate what difference from the baseline will be counted as a
     * {@link State#NORMAL} value, and therefore appear to be green.
     */
    @FXML
    private Slider greenSlider;
    /**
     * A reference to the Slider in the fxml that will dictate what difference from the baseline will be counted as a
     * {@link State#SOFT} value, and therefore appear to be blue.
     */
    @FXML
    private Slider blueSlider;
    /**
     * A reference to the VBox in the fxml that will later be filled with the labels for the rows.
     */
    @FXML
    private VBox rowLables;
    /**
     * A reference to the HBox in the fxml that will later be filled with the labels for the columns.
     */
    @FXML
    private HBox colLables;
    /**
     * A reference to the Canvas in the fxml that will be used to graph the values of a row sensor selected by {@link #rowGraphPicker}.
     */
    @FXML
    private Canvas rowCanvas;
    /**
     * A reference to the Canvas in the fxml that will be used to graph the values of a column sensor selected by {@link #colGraphPicker}.
     */
    @FXML
    private Canvas colCanvas;
    /**
     * A reference to the ChoiceBox in the fxml that will be used to select the row sensor to graph on {@link #rowCanvas}.
     */
    @FXML
    private ChoiceBox rowGraphPicker;
    /**
     * A reference to the ChoiceBox in the fxml that will be used to select the column sensor to graph on {@link #colCanvas}.
     */
    @FXML
    private ChoiceBox colGraphPicker;
    /**
     * A reference to the ImageView in the fxml that will be used to show the "face" of the program/robot.
     */
    @FXML
    private ImageView faceImg;
    /**
     * A reference to the CheckBox in the fxml that, when checked, will only show the maximum value on the grid.
     */
    @FXML
    private CheckBox maxOnly;
    /**
     * A reference to the CheckBox in the fxml that, when checked, will graph the baseline of the sensor on the same graph
     * as its current value.
     */
    @FXML
    private CheckBox graphBaseline;
    /**
     * A reference to the CheckBox in the fxml that, when checked, will mae the program freeze after an "event," to emphasize
     * it.
     */
    @FXML
    private CheckBox freeze;
    /**
     * A reference to the CheckBox in the fxml that, when checked, will amplify the value of the maximum when {@link #maxOnly}
     * is not selected (and if the max isn't {@link State#NONE}.
     */
    @FXML
    private CheckBox amplifyMax;
    /**
     * A reference to the CheckBox in the fxml that, when checked, will ignore {@link #maxOnly} when an event happens, and
     * will also ignore {@link #amplifyMax}.
     */
    @FXML
    private CheckBox exitMax;
    /**
     * A reference to the TextBox in the fxml that will allow the user to set {@link #min} while the program is running.
     */
    @FXML
    private TextField minBox;
    /**
     * A reference to the TextBox in the fxml that will allow the user to set {@link #max} while the program is running.
     */
    @FXML
    private TextField maxBox;
    /**
     * A WritableImage for {@link #gridImg} so that it is possible to edit the image with new data. It is wrapped in a
     * ObjectProperty so that if the image ever has to be recreated (when, for example, the  dimensions of the image
     * need to change) gridImg will update as well.
     */
    private ObjectProperty<WritableImage> img;
    /**
     * The number of data points to show on the graph of a sensor.
     */
    private int dataPointsOnGraph = 100;
    /**
     * Whether {@link #min} and {@link #max} should be updated every frame to match the minimum and maximum of each sample.
     */
    private boolean autoMax;
    /**
     * An Image that contains a reference to an "angry" face, to be used in {@link #faceImg} when the program/robot is
     * "angry"/"hurt".
     */
    private Image angry;
    /**
     * An Image that contains a reference to an "neutral" face, to be used in {@link #faceImg} when the program/robot is
     * neutral/default state
     */
    private Image neutral;
    /**
     * The row of the pixel that had the highest value in the last update.
     */
    private int maxRow;
    /**
     * The column of the pixel that had the highest value in the last update.
     */
    private int maxCol;
    /**
     * The value of the pixel that had the highest value in the last update.
     */
    private double maxVal;
    /**
     * Whether an "event" is occurring right now.
     */
    private volatile boolean event;
    /**
     * A runnable to update the grid image. This must be used because JavaFX only allows images to be updated in the JavaFX
     * thread.
     */
    private Runnable updateImage = new Runnable() {
        @Override
        public void run() {
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    /*double norm = (grid[r][c] - min.get()) / (max.get() - min.get());
                    int color;
                    //System.out.print(grid[r][c] + ", ");
                    if (grid[r][c] > max.get()) {
                        color = Color.BLACK.getRGB();
                    } else if (grid[r][c] < min.get()) {
                        color = Color.GREEN.getRGB();
                    } else {
                        color = getColor(norm);
                    }*/
                    for (int sr = r * hscale; sr < (r + 1) * hscale; sr++) {
                        for (int sc = c * wscale; sc < (c + 1) * wscale; sc++) {
                            // write the correct color to the enlarged pixel on the image
                            if (event && exitMax.isSelected()) {
                                img.get().getPixelWriter().setArgb(sc, sr, getColor(state[r][c]));
                            } else if (!maxOnly.isSelected() || (r == maxRow && c == maxCol)) {
                                if (!maxOnly.isSelected() && amplifyMax.isSelected() && (r == maxRow && c == maxCol)) {
                                    img.get().getPixelWriter().setArgb(sc, sr, getColor(nextState(state[r][c])));
                                } else {
                                    img.get().getPixelWriter().setArgb(sc, sr, getColor(state[r][c]));
                                }
                            } else {
                                img.get().getPixelWriter().setArgb(sc, sr, getColor(State.NONE));
                            }
                            // the alternative is no if statement and just setArgb(sc, sr, Color.HSBtoRGB((1-norm)*0.66, 1, 1);
                            // which will result with transition through the spectrum, from blue to green, yellow, orange then red
                            // as norm increases
                        }
                    }
                }
            }
        }
    };
    /**
     * A runnable to update the graphs image. This must be used because JavaFX only allows images to be updated in the JavaFX
     * thread.
     */
    private Runnable updateGraphs = new Runnable() {
        @Override
        public void run() {
            int i = 0;
            Iterator<double[]> itr = rowBuffer.descendingIterator();
            GraphicsContext gc = rowCanvas.getGraphicsContext2D();
            int addr = (Integer)(rowGraphPicker.getValue())-1;
            gc.setFill(Paint.valueOf("white"));
            gc.fillRect(0, 0, rowCanvas.getWidth(), rowCanvas.getHeight());
            gc.setStroke(Paint.valueOf("black"));
            gc.beginPath();
            while(itr.hasNext() && i < dataPointsOnGraph) {
                gc.lineTo(rowCanvas.getWidth()-(i*rowCanvas.getWidth()/(dataPointsOnGraph-1)), rowCanvas.getHeight()-(itr.next()[addr]-min.get())/(max.get()-min.get())*rowCanvas.getHeight());
                i++;
            }
            //gc.closePath();
            gc.stroke();
            gc.setStroke(Paint.valueOf("red"));
            if (graphBaseline.isSelected()) {
                double y = rowCanvas.getHeight() - (baselineRows[addr]-min.get())/(max.get()-min.get())*rowCanvas.getHeight();
                gc.strokeLine(0, y, rowCanvas.getWidth(), y);
            }

            i = 0;
            itr = colBuffer.descendingIterator();
            gc = colCanvas.getGraphicsContext2D();
            addr = (Integer)(colGraphPicker.getValue())-1;
            gc.setFill(Paint.valueOf("white"));
            gc.fillRect(0, 0, colCanvas.getWidth(), colCanvas.getHeight());
            gc.setStroke(Paint.valueOf("black"));
            gc.beginPath();
            while(itr.hasNext() && i < dataPointsOnGraph) {
                gc.lineTo(colCanvas.getWidth()-(i*colCanvas.getWidth()/(dataPointsOnGraph-1)), colCanvas.getHeight()-(itr.next()[addr]-min.get())/(max.get()-min.get())*colCanvas.getHeight());
                i++;
            }
            //gc.closePath();
            gc.stroke();
            gc.setStroke(Paint.valueOf("red"));
            if (graphBaseline.isSelected()) {
                double y = colCanvas.getHeight() - (baselineCols[addr]-min.get())/(max.get()-min.get())*colCanvas.getHeight();
                gc.strokeLine(0, y, colCanvas.getWidth(), y);
            }
        }
    };

    /**
     * Gets a state that is one level higher than the current one. Used when {@link #amplifyMax} is checked.
     * @param s a State of which a higher value is wanted
     * @return a state one higher if the given state is SOFT or NORMAL. Otherwise, the same state is returned.
     */
    private static State nextState(State s) {
        switch (s) {
            case SOFT:
                return State.NORMAL;
            case NORMAL:
                return State.HARD;
            case NONE:
            case HARD:
            default:
                return s;
        }
    }

    /**
     * A constructor to make JavaFX Scene Builder notice this construct as a valid JavaFX object.
     */
    public ReconImage() {
        FXMLLoader l = new FXMLLoader(ReconImage.class.getResource("/recon_image.fxml"));
        l.setRoot(this);
        l.setController(this);

        try {
            l.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Gets a color from a linear gradient (blue to white to red) based on t, where t represents the location on the gradient
     * @param t a double between 0 and 1
     * @return a color packed into an int in the ARGB format.
     */
    private static int getColor(double t) {
        if (t > .5) {
            return lerpColor(WHITE, RED, (t-0.5)*2);
        } else {
            // if the normalized value is < .5, it should be bluish. the closer to 0, the bluer. the closer
            // to .5, the whiter.
            // B of 1 is white, and B of .5 is blue. since 0.5+0 = 0.5 (blue) and .5+.5 = 1 (white)
            // I do 0.5 + norm for brightness of color
            return lerpColor(BLUE, WHITE, t*2);
        }
    }

    /**
     * Gets the appropriate color for a State: <br/>
     * {@link State#HARD} maps to red. <br/>
     * {@link State#NORMAL} maps to green. <br/>
     * {@link State#SOFT} maps to blue. <br/>
     * {@link State#NONE} maps to black. <br/>
     * @param s a State to get the color for
     * @return the associated color packed into an int using the ARGB format
     */
    private static int getColor(State s) {
        switch (s) {
            case SOFT:
                return Color.BLUE.getRGB();
            case NORMAL:
                return Color.GREEN.getRGB();
            case HARD:
                return Color.RED.getRGB();
            case NONE:
            default:
                return Color.BLACK.getRGB();
        }
    }

    /**
     * a utility method to get the alpha value of a color packed into an int in the ARGB format
     * @param color an int representing the color to get the alpha from
     * @return an int between 0 and 255 representing the alpha of the given color
     * @see #r(int)
     * @see #g(int)
     * @see #b(int)
     */
    private static int a(int color) {
        return (color >>> 24) & 0xFF;
    }

    /**
     * a utility method to get the red value of a color packed into an int in the ARGB format
     * @param color an int representing the color to get the red from
     * @return an int between 0 and 255 representing the red of the given color
     * @see #a(int)
     * @see #g(int)
     * @see #b(int)
     */
    private static int r(int color) {
        return (color >>> 16) & 0xFF;
    }

    /**
     * a utility method to get the green value of a color packed into an int in the ARGB format
     * @param color an int representing the color to get the green from
     * @return an int between 0 and 255 representing the green of the given color
     * @see #a(int)
     * @see #r(int)
     * @see #b(int)
     */
    private static int g(int color) {
        return (color >>> 8) & 0xFF;
    }

    /**
     * a utility method to get the blue value of a color packed into an int in the ARGB format
     * @param color an int representing the color to get the blue from
     * @return an int between 0 and 255 representing the blue of the given color
     * @see #a(int)
     * @see #r(int)
     * @see #g(int)
     */
    private static int b(int color) {
        return color & 0xFF;
    }

    /**
     * a utility method used to lerp linearly between two colors
     * @param from an int representing a color in the ARGB format
     * @param to an int representing a color in the ARGB format
     * @param t a double between 0 and 1
     * @return an int representing a color in the ARGB format
     * @see #a(int)
     * @see #r(int)
     * @see #g(int)
     * @see #b(int)
     */
    private static int lerpColor(int from, int to, double t) {
        // lerp the colors
        int a = (int)(a(from) + t * (a(to) - a(from)));
        int r = (int)(r(from) + t * (r(to) - r(from)));
        int g = (int)(g(from) + t * (g(to) - g(from)));
        int b = (int)(b(from) + t * (b(to) - b(from)));
        // clamp the values
        if (a < 0) { a = 0; } else if (a > 255) { a = 255; }
        if (r < 0) { r = 0; } else if (r > 255) { r = 255; }
        if (g < 0) { g = 0; } else if (g > 255) { g = 255; }
        if (b < 0) { b = 0; } else if (b > 255) { b = 255; }
        // wrap it up and return it
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Initializes the images and sliders of this program. <br/>
     * Called when JavaFX loads this class.
     */
    @FXML
    private void initialize() {
        this.img = new ObjectPropertyBase<WritableImage>(new WritableImage(1, 1)) {
            @Override
            public Object getBean() {
                return ReconImage.this;
            }

            @Override
            public String getName() {
                return "img";
            }
        };
        this.gridImg.imageProperty().bind(this.img);
        this.redSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() < greenSlider.getValue()) {
                redSlider.setValue(greenSlider.getValue());
            }
            redText.setText("Red start " + redSlider.getValue());
        });
        this.redSlider.setMax(100);
        this.redSlider.setValue(24.6);
        this.greenSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() < blueSlider.getValue()) {
                greenSlider.setValue(blueSlider.getValue());
            } else if (newValue.doubleValue() > redSlider.getValue()) {
                greenSlider.setValue(redSlider.getValue());
            }
            greenText.setText("Green start " + greenSlider.getValue());
        });
        this.greenSlider.setMax(100);
        this.greenSlider.setValue(12);
        this.blueSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > greenSlider.getValue()) {
                blueSlider.setValue(greenSlider.getValue());
            }
            blueText.setText("Blue start " + blueSlider.getValue());
        });
        this.blueSlider.setMax(100);
        this.blueSlider.setValue(7.2);
        this.angry = new Image("angry.png");
        this.neutral = new Image("neutral.png");
        this.faceImg.setImage(neutral);
    }

    /**
     * Called by JavaFX when a certain button is pressed, and theoretically updates the min and the max.
     */
    @FXML
    private void updateSettings() {
        try {
            if (minBox.getText() != null) {
                this.min.set(Double.parseDouble(minBox.getText()));
            }
        } catch (NumberFormatException e) {
            minBox.clear();
        }
        try {
            if (maxBox.getText() != null) {
                this.max.set(Double.parseDouble(maxBox.getText()));
            }
        } catch (NumberFormatException e) {
            maxBox.clear();
        }

    }

    /**
     * A method used to set the parameters of the program that aren't set by JavaFX.
     * @param name The title/label of this graph.
     * @param width The number of column sensors in the sensor array.
     * @param height The number of row sensors in the sensor array.
     * @param wscale How many pixels wide each column should appear in the image.
     * @param hscale How many pixels tall each row should appear in the image.
     * @param max The maximum value that should be plotted on the graph of each sensor.
     * @param min The minimum value that should be plotted on the graph of each sensor.
     * @param rscale A double between 0 and 1 that dictates how much weight should be given to the value read from each row sensor.
     * @param cscale A double between 0 and 1 that dictates how much weight should be given to the value read from each column sensor.
     * @param autoMax Whether min and max should be updated every frame to match the minimum and maximum of each sample.
     * @param delay The amount of time the graph and face should be frozen for after an "event" (in milis).
     */
    public void setContext(String name, int width, int height, int wscale, int hscale, double max, double min, double rscale,
                      double cscale, boolean autoMax, long delay) {
        this.name = name;
        this.title.setText(name);
        this.width = width;
        this.height = height;
        this.wscale = wscale;
        this.hscale = hscale;
        this.max.set(max);
        this.min.set(min);
        this.rscale = rscale;
        this.cscale = cscale;
        //this.grid = new double[height][width];
        this.state = new State[height][width];
        this.img.set(new WritableImage(width * wscale, height * hscale));
        hardImage.setImage(getImageWithColor(20, height*hscale/4, getColor(State.HARD)));
        mediumImage.setImage(getImageWithColor(20, height*hscale/4, getColor(State.NORMAL)));
        softImage.setImage(getImageWithColor(20, height*hscale/4, getColor(State.SOFT)));
        noneImage.setImage(getImageWithColor(20, height*hscale/4, getColor(State.NONE)));
        this.autoMax = autoMax;
        this.baselineRows = new double[height];
        this.baselineCols = new double[width];
        this.rowBuffer = new LinkedList<>();
        this.colBuffer = new LinkedList<>();
        this.imgPauseStart = 0;
        this.delay = delay;
        ArrayList<Integer> cols = new ArrayList<>(width);
        //colLables.getChildren().add(new Pane());
        for (int i = 0; i < width; i++) {
            cols.add(i+1);
            colLables.getChildren().add(paneWithText(""+ (i+1)));
        }
        //colLables.getChildren().add(new Pane());
        for (Node n : colLables.getChildren()) {
            HBox.setHgrow(n, Priority.ALWAYS);
        }
        this.colGraphPicker.setItems(FXCollections.observableArrayList(cols));
        this.colGraphPicker.getSelectionModel().select(0);
        // its actually rows this time but dont tell anyone
        cols = new ArrayList<>(height);
        for (int i = 0; i < height; i++) {
            cols.add(i+1);
            rowLables.getChildren().add(paneWithText(""+ (i+1)));
        }
        for (Node n : rowLables.getChildren()) {
            VBox.setVgrow(n, Priority.ALWAYS);
        }
        this.rowGraphPicker.setItems(FXCollections.observableArrayList(cols));
        this.rowGraphPicker.getSelectionModel().select(0);
    }

    /**
     * A utility method that created a Pane with the given String as text, and formats the text correctly.
     * @param text A String to put inside the pane, or null or "" if it should be left blank
     * @return a Pane with the text aligned to the center
     */
    private static Pane paneWithText(String text) {
        StackPane p = new StackPane();
        if (text != null && !text.isEmpty()) {
            Text t = new Text(text);
            p.getChildren().add(t);
            StackPane.setAlignment(t, Pos.CENTER);
        }
        return p;
    }

    /**
     * Creates and Image of the given dimensions with the given color.
     * @param width the width of the image, ideally positive.
     * @param height the height of the image, ideally positive.
     * @param color the color to put in the image, packed into an int using the ARGB format.
     * @return an image of the given size and color.
     */
    private static Image getImageWithColor(int width, int height, int color) {
        WritableImage img = new WritableImage(width, height);
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                img.getPixelWriter().setArgb(c, r, color);
            }
        }
        return img;
    }

    /**
     * Updates this object with new data. If any of the arrays are null, this update is ignored.
     * @param columns an array of size {@link #width} of the values of the column sensors.
     * @param rows an array of size {@link #width} of the values of the column sensors.
     */
    public void update(final double[] columns, final double[] rows) {
        // check for null and update baselines
        if (columns == null || rows == null) {
            System.err.println(this.name + ": Either columns or rows were null, ignoring this update");
            return;
        }
        double[] cols = new double[columns.length];
        double[] row = new double[rows.length];
        System.arraycopy(columns, 0, cols, 0, columns.length);
        System.arraycopy(rows, 0, row, 0, columns.length);
        int rebaselineCount = 0;
        for (int i = 0; i < row.length; i++) {
            if (this.rowBuffer.size() == 0 || this.rowBuffer.getLast()[i] - row[i] > this.thresholdToRebasline || this.baselineRows[i] - row[i] > this.thresholdToRebasline*3
                    || delta(this.rowBuffer, i, 5, row[i]) > this.thresholdToRebasline*4) {
                this.baselineRows[i] = row[i];
                rebaselineCount++;
            }
        }
        for (int i = 0; i < cols.length; i++) {
            if (this.colBuffer.size() == 0 || this.colBuffer.getLast()[i] - cols[i] > this.thresholdToRebasline || this.baselineCols[i] - cols[i] > this.thresholdToRebasline*3
                    || delta(this.colBuffer, i, 5, cols[i]) > this.thresholdToRebasline*4) {
                this.baselineCols[i] = cols[i];
                rebaselineCount++;
            }
        }
        if (rebaselineCount > 6) {
            System.arraycopy(cols, 0, this.baselineCols, 0, cols.length);
            System.arraycopy(rows, 0, this.baselineRows, 0 , rows.length);
        }

        this.rowBuffer.add(row);
        this.colBuffer.add(cols);

        // ok, so sometime in the past we got the baseline. now to update the image

        if (autoMax) {
            max.set(-10000);
            min.set(10000);
        }
        maxVal = Double.MIN_VALUE;
        double v;
        int hardcount = 0;
        int normcount = 0;
        int softcount = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                v = ((rows[r] - baselineRows[r]) * rscale + (columns[c] - baselineCols[c]) * cscale)/2;
                //v *= v;
                //v = v/(max.get()-min.get());
                if (v > maxVal) {
                    maxRow = r;
                    maxCol = c;
                    maxVal = v;
                }
                if (v > redSlider.getValue()) {
                    state[r][c] = State.HARD;
                    hardcount++;
                } else if (v > greenSlider.getValue()) {
                    state[r][c] = State.NORMAL;
                    normcount++;
                } else if (v > blueSlider.getValue()) {
                    state[r][c] = State.SOFT;
                    softcount++;
                } else {
                    state[r][c] = State.NONE;
                }
            }
        }
        if ((double)hardcount/(height*width) > significant || (hardcount+normcount)/((double)height*width) > .8) {
            event = true;
            Platform.runLater(updateImage);
            Platform.runLater(() -> faceImg.setImage(angry));
            imgPauseStart = System.currentTimeMillis();
        } else if (System.currentTimeMillis() > imgPauseStart + delay || !freeze.isSelected()) {
            event = false;
            Platform.runLater(updateImage);
            Platform.runLater(() -> faceImg.setImage(neutral));
        }
        Platform.runLater(updateGraphs);
    }

    /**
     * Gets the total change for a given sensor over a certain period of time. If the buffer does not go far enough, then
     * this method simply stops at the end of the buffer and returns the change up until then.
     * @param buffer A LinkedList containing the history of the sensor's values.
     * @param index The index of the sensor in the arrays of the LinkedList.
     * @param amount How far back to go.
     * @param cur The current value of the sensor.
     * @return The total change of the sensor over the given range.
     */
    private static double delta(LinkedList<double[]> buffer, int index, int amount, double cur) {
        // this whole thing can probably be reduced to "return cur-buffer.get(buffer.size()-amount)" but this can probably
        // be used to make something more helpful in the future. I think. Either way, this doesn't increase runtime by much
        // as it will have to step through the linked list anyway.
        Iterator<double[]> itr = buffer.descendingIterator();
        buffer.get(1);
        int i = 0;
        double prev;
        double totalChange = 0;
        while (itr.hasNext() && i < amount) {
            prev = cur;
            cur = itr.next()[index];
            totalChange += cur-prev;
        }
        return totalChange;
    }

    /**
     * An enum used for coloring the grid.
     */
    private enum State {
        SOFT, NORMAL, HARD, NONE
    }
}
