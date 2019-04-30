import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.*;


public class VideoPlayer implements Runnable {
    private int currentFrame;

    private AudioPlayer audioPlayer;
    private static InputStream inputStream;
    private byte[] bytes;
    private String videoFileName;

    private static final Object lock = new Object();

    private static boolean suspended;
    private static boolean stop;

    static BufferedImage bufferedImage;

    public VideoPlayer(String videoFilename, AudioPlayer playSound) {
        this.videoFileName = videoFilename;
        this.audioPlayer = playSound;
    }

    @Override
    public void run() {
        try {
            playVideo();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void playVideo() throws InterruptedException {

        currentFrame = 0;

        bufferedImage = new BufferedImage(Constants.WIDTH, Constants.HEIGHT, BufferedImage.TYPE_INT_RGB);

        try {
            File videoFile = new File(videoFileName);
            inputStream = new FileInputStream(videoFile);
            long numberOfFrames = videoFile.length() / Constants.PIXELS_PER_FRAME;
            bytes = new byte[(int) Constants.PIXELS_PER_FRAME];

            PlayVideoComponent component = new PlayVideoComponent();
            double numberOfSamplesPerFrame = audioPlayer.getSampleRate() / 30;

            int offset = 0;
            int counter = 0;
            double frameMovementDifference = Math.round(audioPlayer.getFramePosition()) / numberOfSamplesPerFrame;

            if (!isStop()) {
                while (counter < frameMovementDifference) {
                    processFrameAhead(component);
                    counter++;
                }

                for (int nextFrameCounter = counter; nextFrameCounter < numberOfFrames; nextFrameCounter++) {
                    while (nextFrameCounter < frameMovementDifference) {
                        processFrameAhead(component);
                        nextFrameCounter++;
                    }
                    processFrameAhead(component);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processFrameAhead(PlayVideoComponent component) {
        readBytes();
        component.setImg(bufferedImage);
        Player.controlOptionPanel.add(component);
        Player.controlOptionPanel.repaint();
        Player.controlOptionPanel.setVisible(true);
    }

    private void readBytes() {
        currentFrame++;
        synchronized (this) {
            while (suspended) {
                Thread.interrupted();
            }
        }

        try {
            int offset = 0;
            int numRead = 0;

            while (offset < bytes.length && (numRead = inputStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            int ind = 0;
            for (int y = 0; y < Constants.HEIGHT; y++) {
                for (int x = 0; x < Constants.WIDTH; x++) {
                    byte r = bytes[ind];
                    byte g = bytes[ind + Constants.HEIGHT * Constants.WIDTH];
                    byte b = bytes[ind + Constants.HEIGHT * Constants.WIDTH * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    bufferedImage.setRGB(x, y, pix);
                    ind++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isStop() {
        return stop;
    }

    public static void stop() {
        try {
            inputStream.close();
            stop = true;
            System.exit(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void suspend() {
        suspended = true;
    }

    public static void resume() {
        synchronized (lock) {
            suspended = false;
            lock.notify();
        }
    }


    public class PlayVideoComponent extends JComponent {

        private BufferedImage img;

        public void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.drawImage(img, 0, 0, this);
        }

        public void setImg(BufferedImage img) {
            this.img = img;
        }

    }
}