## The Grid Image Reconstructor
This program is, roughly, a simpler version of [Electrical Impedance Tomography](https://en.wikipedia.org/wiki/Electrical_impedance_tomography), or EIT. The difference is that while usually EIT  works with a continuous surface, this program assumes discrete, linear, independent sensors that comprise a grid.
This program connects over serial to a microprocessor that simply prints the readings from each sensor over serial. Each reading is then associated with a column or row. Then the program assumes every column and row intersects, and based on a weighted average of the two, generates an image of the sensor array.
This program is programmed to work with strain gauges, sensors that increase in resistance as they deform, meaning that the image generated should approximate the strain on the sensor array at that moment.

# Running the program
To run this program, there are several options:
1. Clone or pull this repository and open it in your favorite IDE, and adding the [dependecies](#dependencies).
2. Download the files in [out/artifacts/GridImageReconstructor](out/artifacts/GridImageReconstructor) and run the runnable jar (using the jnlp file, the jar itself or the command line). Make sure you have Java 8.

# Dependencies
This program was tested with jssc 2.8.0 (in [the repo](out/artifacts/GridImageReconstructor/jssc-2.8.0.jar) or from [their github repo](https://github.com/scream3r/java-simple-serial-connector/releases).
This program was also compiled against JRE 1.8 aka [Java 8](http://www.oracle.com/technetwork/java/javase/overview/java8-2100321.html)
