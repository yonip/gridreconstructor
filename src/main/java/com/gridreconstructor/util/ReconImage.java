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
    private String name;
    private static final int RED = 0xFFFF0000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLUE = 0xFF0000FF;
    private int width;
    private int height;
    private int wscale;
    private int hscale;
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
    private double rscale;
    private double cscale;
    private double[] baselineCols;
    private double[] baselineRows;
    private LinkedList<double[]> rowBuffer;
    private LinkedList<double[]> colBuffer;
    private double significant = 5.0/64;
    private long delay;
    private long imgPauseStart;
    private State[][] state;
    private double[][] grid;
    private double thresholdToRebasline = 2;
    @FXML
    private ImageView gridImg;
    @FXML
    private ImageView hardImage;
    @FXML
    private ImageView mediumImage;
    @FXML
    private ImageView softImage;
    @FXML
    private ImageView noneImage;
    @FXML
    private Text title;
    @FXML
    private Text redText;
    @FXML
    private Text greenText;
    @FXML
    private Text blueText;
    @FXML
    private Slider redSlider;
    @FXML
    private Slider greenSlider;
    @FXML
    private Slider blueSlider;
    @FXML
    private VBox rowLables;
    @FXML
    private HBox colLables;
    @FXML
    private Canvas rowCanvas;
    @FXML
    private Canvas colCanvas;
    @FXML
    private ChoiceBox rowGraphPicker;
    @FXML
    private ChoiceBox colGraphPicker;
    @FXML
    private ImageView faceImg;
    @FXML
    private CheckBox maxOnly;
    @FXML
    private CheckBox graphBaseline;
    @FXML
    private CheckBox freeze;
    @FXML
    private CheckBox amplifyMax;
    @FXML
    private CheckBox exitMax;
    @FXML
    private TextField minBox;
    @FXML
    private TextField maxBox;
    private ObjectProperty<WritableImage> img;
    private int dataPointsOnGraph = 100;
    private boolean autoMax;
    private Image angry;
    private Image neutral;
    private int maxRow;
    private int maxCol;
    private double maxVal;
    private volatile boolean event;
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

    private static Pane paneWithText(String text) {
        StackPane p = new StackPane();
        if (text != null && !text.isEmpty()) {
            Text t = new Text(text);
            p.getChildren().add(t);
            StackPane.setAlignment(t, Pos.CENTER);
        }
        return p;
    }

    private static Image getImageWithColor(int width, int height, int color) {
        WritableImage img = new WritableImage(width, height);
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                img.getPixelWriter().setArgb(c, r, color);
            }
        }
        return img;
    }

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
            for (int i = 0; i< cols.length; i++) {
                this.baselineCols[i] = cols[i];
            }
            for (int i = 0; i< rows.length; i++) {
                this.baselineRows[i] = rows[i];
            }
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

    private static double delta(LinkedList<double[]> buffer, int index, int amount, double cur) {
        Iterator<double[]> itr = buffer.descendingIterator();
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

    private enum State {
        SOFT, NORMAL, HARD, NONE
    }
}
