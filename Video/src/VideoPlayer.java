import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

public class VideoPlayer {
    private static String filename = "";

    private static int analysis = 0;
    private static int antiAliasing = 0;

    private static float scaleHeight = 1.0f;
    private static float scaleWidth = 1.0f;

    private static float fps = 10.0f;
    static int IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;

    private static int originalWidth = 960;
    private static int originalHeighht = 540;

    private static void preProcessing(String[] args) {
        filename = args[0];
    }

    public static void main(String[] args) {
        preProcessing(args);
        GraphicalUserInterface graphicalUserInterface = new GraphicalUserInterface();

        int modifiedWidth = originalWidth;
        int modifiedHeight = originalHeighht;

        boolean isScaled = false;

        //Update the new width
        if (scaleWidth < 1.0) {
            modifiedWidth = (int) Math.floor(scaleWidth * originalWidth);
            isScaled = true;
        }

        //Update the new getHeight
        if (scaleHeight < 1.0) {
            modifiedHeight = (int) Math.floor(scaleHeight * originalHeighht);
            isScaled = true;
        }


        VideoFrameController videoCapture = new VideoFrameController(filename, originalWidth, originalHeighht);
        ImageInfo originalFrame = new ImageInfo(originalWidth, originalHeighht, IMAGE_TYPE);
        ImageInfo modifiedFrame = new ImageInfo(modifiedWidth, modifiedHeight, IMAGE_TYPE);

        videoCapture.openFile();
        int totalFrames = videoCapture.getNumOfFrames();


        if (!videoCapture.isFileOpened()) {
            System.err.println("Unable to open the vidoe stream .... for file : " + filename);
            return;
        }

        if (scaleHeight > 1 && scaleWidth > 1 && analysis == 2) {
            System.out.println("Seam carving uses to reduce the image size based on energy of the pixels. scaling up is  not possible");
            return;
        }

        List<BufferedImage> video = new ArrayList<BufferedImage>();
        int frameCounter = 1;

        System.out.println("Processing " + filename + "...Please Wait");
        while (videoCapture.readContent(originalFrame)) {
            System.out.print(".");

            ImageInfo.resize(originalFrame, modifiedFrame, scaleWidth, scaleHeight, antiAliasing, analysis);
            video.add(modifiedFrame.copyData());

            frameCounter++;
        }
        System.out.println("\nTotal Frames ......" + video.size());
        System.out.println("Started showing Frames ......");
        long initialTime = System.currentTimeMillis();
        long sleep = (long) Math.floor((double) 1000 / fps);
        for (int idx = 0; idx < video.size(); idx++) {
            graphicalUserInterface.showFrame(video.get(idx), idx);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Done.......Total Time took to display  " + video.size() + " frames is(sec) " + (endTime - (initialTime + (video.size() * sleep) / 1000)) / 1000);
    }
}

class GraphicalUserInterface {
    private JFrame window;
    private JLabel label;

    public GraphicalUserInterface() {
        this.label = new JLabel();

        this.window = new JFrame();
        window.getContentPane().add(label, BorderLayout.CENTER);
        window.pack();
        window.setVisible(true);
    }

    public void showFrame(BufferedImage bufferedImage, int frameCount) {
        this.label.setIcon(new ImageIcon(bufferedImage));
        window.pack();
    }
}


class VideoFrameController {
    private byte[] vidFrameData;
    private String filename;
    private boolean isFileOpened;

    private int width;
    private int colorModel;
    private int height;


    private int length;
    private InputStream inputStream;
    private int perFrameDataLength;
    private int numOfFrames;


    public boolean retrieveFrame(ImageInfo frame) {
        frame.setBufferedImage(this.vidFrameData);
        return true;
    }

    public boolean readContent(ImageInfo imageInfo) {
        return grabFrame() ? retrieveFrame(imageInfo) : false;
    }

    public void openFile() {
        File file = new File(this.filename);
        try {
            this.inputStream = new FileInputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.length = (int) file.length();
        this.perFrameDataLength = this.height * this.width * 3;
        this.numOfFrames = this.length / perFrameDataLength;
        isFileOpened = true;
    }

    public VideoFrameController(String filename, int width, int height) {
        this.width = width;
        this.height = height;
        this.colorModel = BufferedImage.TYPE_INT_RGB;
        this.filename = filename;
    }

    public boolean grabFrame() {
        try {
            int offset = 0, numRead = 0;
            this.vidFrameData = new byte[this.perFrameDataLength];
            while (offset < this.vidFrameData.length &&
                    (numRead = this.inputStream.read(this.vidFrameData, offset, this.vidFrameData.length - offset)) >= 0) {
                offset = offset + numRead;
            }
            return numRead > 0 ? true : false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isFileOpened() {
        return this.isFileOpened;
    }

    public int getNumOfFrames() {
        return numOfFrames;
    }

}
