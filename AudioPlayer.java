package csci576;
import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.SourceDataLine;


public class AudioPlayer implements Runnable {

    private InputStream waveStream;
    private static SourceDataLine sourceDataLine;
    private AudioFormat audioFormat;
    private static Clip audioClip;


    public AudioPlayer(InputStream waveStream) {
        this.waveStream = waveStream;
    }

    public void playAudioVideoTogether() {
        AudioInputStream audioInputStream = null;
        try {
            InputStream bufferedIn = new BufferedInputStream(this.waveStream);
            audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);
            audioClip.start();

        } catch (Exception e1) {
            e1.printStackTrace();
        }
        audioFormat = audioInputStream.getFormat();
    }

    public long getFramePosition() {
        return audioClip.getLongFramePosition();
    }

    public float getSampleRate() {
        System.out.println(audioFormat);
        return audioFormat.getFrameRate();
    }

    @Override
    public void run() {
        this.playAudioVideoTogether();
    }

    public static void pauseAudio() {
        audioClip.stop();
    }

    public static void resumeAudio() {
        audioClip.start();
    }

    public static void stop() {
        audioClip.stop();
        audioClip.setFramePosition(0);
        System.exit(0);
    }
}

