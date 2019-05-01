package csci576;
import java.awt.*;
import java.io.FileInputStream;

import javax.swing.*;

public class Player {

    static JFrame controlOptionPanel;


    public static void ProcessAudioVideo(String videoFileName, String audioFileName) {


    }


    public static void main(String[] args) {

        controlOptionPanel = new JFrame();
        controlOptionPanel.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        controlOptionPanel.setTitle("PlayerControlPanel");
        controlOptionPanel.setSize(600, 400);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setPreferredSize(new Dimension(100, 100));
        controlOptionPanel.getContentPane().add(buttonPanel, BorderLayout.EAST);

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        PlayerControlPanel button = new PlayerControlPanel(Constants.START);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(button);

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        button = new PlayerControlPanel(Constants.PAUSE);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(button);

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        button = new PlayerControlPanel(Constants.STOP);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.add(button);

        buttonPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        controlOptionPanel.setVisible(true);
        ProcessAudioVideo(args[0], args[1]);
        System.out.println(args[0] + args[1]);

        try {
            FileInputStream inputStream = new FileInputStream(args[1]);
            AudioPlayer playSound = new AudioPlayer(inputStream);
            VideoPlayer playVideo = new VideoPlayer(args[0], playSound);

            new Thread(playSound).start();
            Thread.sleep(200);
            new Thread(playVideo).start();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }




}