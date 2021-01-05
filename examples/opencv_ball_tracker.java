///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.openpnp:opencv:4.3.0-3
//DEPS org.openjfx:javafx-controls:11.0.2:${os.detected.jfxname}

import static java.lang.System.*;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class opencv_ball_tracker extends Application {

    // Adjust this depending on which webcam device you want to use
    // Typically 0 but if you have more cams then 1, 2, 3 etc
    private static final int webcamDeviceIndex = 0;

    private ImageView originalFrame;
    private VideoCapture capture;
    private Stage parent;
    private Timer timer;

    public static void main(String... args) throws Exception{
        nu.pattern.OpenCV.loadLocally();
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        parent = stage;
        stage.setTitle("Tennis Ball Tracker");
        capture = new VideoCapture();
        capture.open(webcamDeviceIndex);
        stage.setHeight(480);
        stage.setWidth(640);
        originalFrame = new ImageView(grabFrame());
        Scene scene = new Scene(new StackPane(originalFrame));
        stage.setScene(scene);
        stage.show();

        timer = new Timer();
        timer.schedule(new FrameGrabber(), 0, 100);
    }

    @Override
    public void stop() {
        timer.cancel();
        capture.release();
        System.exit(0);
    }

    /**
     * Captures a frame a applies the tracking transformations
     * 
     * @return the {@link Image} to show
     */
    private Image grabFrame() {
        Image imageToShow = null;
        Mat frame = new Mat();
        if (this.capture.isOpened()) {
            try {
                this.capture.read(frame);
                // init
                Mat blurredImage = new Mat();
                Mat hsvImage = new Mat();
                Mat mask = new Mat();
                Mat morphOutput = new Mat();
                
                // remove some noise
                Imgproc.blur(frame, blurredImage, new Size(7, 7));
                
                // convert the frame to HSV
                Imgproc.cvtColor(blurredImage, hsvImage, Imgproc.COLOR_BGR2HSV);

                // Ball tracking thresholding values
                // remember: H ranges 0-180, S and V range 0-255
                // Adjust these values if you have a different ball color
                // or lighting conditions
                Scalar minValues = new Scalar(20, 60, 50);
                Scalar maxValues = new Scalar(50,200, 255);

                Core.inRange(hsvImage, minValues, maxValues, mask);
                
                // morphological operators
                // dilate with large element, erode with small ones
                Mat dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(24, 24));
                Mat erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(12, 12));
                
                Imgproc.erode(mask, morphOutput, erodeElement);
                Imgproc.erode(morphOutput, morphOutput, erodeElement);
                
                Imgproc.dilate(morphOutput, morphOutput, dilateElement);
                Imgproc.dilate(morphOutput, morphOutput, dilateElement);
                
                // find the tennis ball(s) contours and show them
                frame = this.findAndDrawBalls(morphOutput, frame);

                Imgproc.resize(frame, frame, new Size(parent.getWidth(), parent.getHeight()));
                Imgproc.resize(morphOutput, morphOutput, new Size(parent.getWidth(), parent.getHeight()));
                if (!frame.empty()) {
                    imageToShow = mat2Image(frame);
                }
            } catch(Exception ex) {
                err.print("ERROR");
                ex.printStackTrace();
                System.exit(1);
            }
        }
        return imageToShow;
    }

    /**
     * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
     * 
     * @param frame
     *            the {@link Mat} representing the current frame
     * @return the {@link Image} to show
     */
    private Image mat2Image(Mat frame) {
        // create a temporary buffer
        MatOfByte buffer = new MatOfByte();
        // encode the frame in the buffer, according to the PNG format
        Imgcodecs.imencode(".png", frame, buffer);
        // build and return an Image created from the image encoded in the
        // buffer
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    /**
     * Given a binary image containing one or more closed surfaces, use it as a
     * mask to find and highlight the objects contours
     * 
     * @param maskedImage
     *            the binary image to be used as a mask
     * @param frame
     *            the original {@link Mat} image to be used for drawing the
     *            objects contours
     * @return the {@link Mat} image with the objects contours framed
     */
    private Mat findAndDrawBalls(Mat maskedImage, Mat frame) {
        // init
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        
        // find contours
        Imgproc.findContours(maskedImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);
        
        // if any contour exist...
        if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            // for each contour, display it in blue
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                Imgproc.drawContours(frame, contours, idx, new Scalar(0, 0, 255));
            }
        }

        return frame;
    }

    private class FrameGrabber extends TimerTask {
        @Override
        public void run() {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    originalFrame.setImage(grabFrame());
                }
            });
        }
    }
}
