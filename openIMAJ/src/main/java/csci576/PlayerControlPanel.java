
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

public class PlayerControlPanel extends JButton {

    public PlayerControlPanel(String label) {
        this.setFont(new Font("TimeNewRoman", Font.BOLD, 14));
        this.setText(label);
        this.addMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        pressTheButton(getText());
                    }
                }
        );
    }

    public void pressTheButton(String label) {

        if (label.equals(Constants.START)) {
            VideoPlayer.resume();
            AudioPlayer.resumeAudio();
        } else if (label.equals(Constants.PAUSE)) {
            VideoPlayer.suspend();
            AudioPlayer.pauseAudio();
        } else if (label.equals(Constants.STOP)) {
            VideoPlayer.stop();
            AudioPlayer.stop();
        }
    }

}


