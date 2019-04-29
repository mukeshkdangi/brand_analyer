import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioVideoProcessor {

 

    public static class Shot {
        public int      start, end;
        public Category category;
        double          avgAmp;
        Logo            logo;
        // sampleCount = length()/30*audio frame rate
        int             sampleCount;
        // signChangeFreq measure frequencies per frame
        double          signChangeFreq;
        int             logos[];

        public Shot(int s, int e) {
            start = s;
            end = e;
            category = Category.EITHER;
            avgAmp = 0;
            sampleCount = 0;
            logo = Logo.NONE;
            logos = new int[4];
        }

        public int length() {
            return end - start;
        }

        public void addSample(double s) {
            avgAmp += s;
            ++sampleCount;
        }

        public void avgSample() {
            if (sampleCount != 0) {
                avgAmp /= sampleCount;
            }
        }
    }

    public enum Category {
        NO_ADVRT, ADVRT, EITHER
    }

    public enum Logo {
        STARBUCKS(0), SUBWAY(1), NFL(2), MCDONALDS(3), NONE(-1);

        int key;

        Logo(int k) {
            key = k;
        }
    }
    
    public static final int WIDTH  = 480;
    public static final int HEIGHT = 270;

    

    /**
     * Analyzes the audio stream that is matched with the video stream to get
     * average amplitudes of shots for better distinguishing.
     */
    public static Shot[] analyzeAudio(String audiopath, Shot[] shots)
            throws UnsupportedAudioFileException, IOException {
        File audio = new File(audiopath);

        AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
        AudioFormat format = stream.getFormat();
        double framerate = format.getFrameRate();
        int framesize = format.getFrameSize();
        boolean bigend = format.isBigEndian();

        byte[] buffer = new byte[framesize];

        int read = 0;
        int offset = 0;
        int x, y;
        int shotOffset = 0;

        int sign = 1;
        int signCount = 0;
        for (int curFrame = 0; (read = stream.read(buffer)) > 0; ++curFrame) {

            if (read != framesize) {
                offset = read;
                while (offset < framesize && (read = stream.read(buffer, offset, framesize - offset)) >= 0) {
                    offset += read;
                }
            }

            if (!bigend) {
                x = buffer[1] << 8;
                y = buffer[0];
            } else {
                x = buffer[0] << 8;
                y = buffer[1];
            }
            double xy = x | y;

            // sample frame 0 maps to frame 1, sample frame 48000 (framerate)
            // maps to frame 31
            int videoFrame = (int) ((curFrame / framerate) * 30) + 1;
            if (videoFrame > shots[shotOffset].end) {
                shots[shotOffset].signChangeFreq = signCount * 1.0 / shots[shotOffset].length();
                signCount = 0;
                sign = 1;
                ++shotOffset;
            }

            shots[shotOffset].addSample(Math.abs(xy / Short.MAX_VALUE));
            if (sign * xy < 0) {
                sign = (xy < 0 ? -1 : 1);
                ++signCount;
            }
        }

        shots[shotOffset].signChangeFreq = signCount * 1.0 / shots[shotOffset].length();

        for (Shot s : shots) {
            s.avgSample();
        }

        return shots;
    }

    /**
     * Cuts out the ad frames in a video
     */
    public static void removeAdsFromVideo(String videoIn, String videoOut, Shot[] shots) throws IOException {

        File f = new File(videoIn);
        InputStream videoStream = new FileInputStream(f);

        FileOutputStream outStream = new FileOutputStream(videoOut);

        // Some initial data for the for loop
        int numRead = 0;
        int frame = 1;
        int curShot = -1;
        byte bytes[] = new byte[3 * WIDTH * HEIGHT];

        for (frame = 1; numRead != -1; ++frame) {
            int offset = 0;
            while (offset < bytes.length && (numRead = videoStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (curShot == -1 || shots[curShot].end < frame) {
                ++curShot;
                if (curShot == shots.length) {
                    break;
                }
            }
            //  Considering only other than ads to include in the stream 
            if (shots[curShot].category == Category.NO_ADVRT) {
                outStream.write(bytes);
            }
        }

        
        videoStream.close();
        outStream.close();
    }

    public static void removeAdsFromAudio(String audioIn, String audioOut, Shot[] shots)
            throws UnsupportedAudioFileException, IOException {
        
        File audio = new File(audioIn);
        AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
        AudioFormat format = stream.getFormat();
        
        double framerate = format.getFrameRate();
        int framesize = format.getFrameSize();

        byte buffer[] = new byte[(int) (framerate * framesize / 30)];
        FileOutputStream fout = new FileOutputStream(audioOut + ".temp");
        int read = 0;
        int offset = 0;
        int curShot = -1;
        int length = 0;
        for (int frame = 1; (read = stream.read(buffer)) > 0; ++frame) {
            if (read != framesize) {
                offset = read;
                while (offset < framesize && (read = stream.read(buffer, offset, framesize - offset)) >= 0) {
                    offset += read;
                }
            }

            if (curShot == -1 || shots[curShot].end < frame) {
                ++curShot;
                if (curShot == shots.length) {
                    break;
                }
            }

            if (shots[curShot].category == Category.NO_ADVRT) {
                fout.write(buffer);
                length += (int) (framerate / 30);
            }
        }
        stream.close();
        fout.close();

        // Take the data in the temp file and write it to an actual file
        File out = new File(audioOut);
        FileInputStream fin = new FileInputStream(audioOut + ".temp");
        AudioInputStream as = new AudioInputStream(fin, format, length);
        AudioSystem.write(as, AudioFileFormat.Type.WAVE, out);
        fin.close();
        as.close();
    }

    public static String[] adVideos = { "dataset/Ads/Starbucks_Ad_15s.rgb", "dataset/Ads/Subway_Ad_15s.rgb",
            "dataset2/Ads/nfl_Ad_15s.rgb", "dataset2/Ads/mcd_Ad_15s.rgb" };

    public static String[] adAudios = { "dataset/Ads/Starbucks_Ad_15s.wav", "dataset/Ads/Subway_Ad_15s.wav",
            "dataset2/Ads/nfl_Ad_15s.wav", "dataset2/Ads/mcd_Ad_15s.wav" };

    public static String   prePath  = "⁨/Users/mukesh/Downloads/";

    public static void main(String[] args) throws Exception {
        // Get input file name
        String videopath = prePath + "dataset2/Videos/data_test1.rgb";
        String audiopath = prePath + "dataset2/Videos/data_test1.wav";
        String videoout = "video.rgb";
        String audioout = "audio.wav";

        if (args.length > 0) {
            videopath = args[0];
        }

        if (args.length > 1) {
            audiopath = args[1];
        }
        if (args.length > 2) {
            videoout = args[2];
        }
        if (args.length > 3) {
            audioout = args[3];
        }

        // Get shots from video by analyzing video and audio
        System.out.println("Analyzing vidoe shot...");
        Shot[] shots = analyzeVideo(videopath);
        
        System.out.println("Analyzing Audio to get more accurate  video shot cutting...");
        //analyzeAudio(audiopath, shots);

        System.out.println("Labeling shots...");
        // Pass 1: look for isolated shots and join labels with neighbors
        for (int index = 0; index < shots.length; ++index) {
            Set<Category> neighbors = new HashSet<Category>();
            if (index != 0) {
                neighbors.add(shots[index - 1].category);
            }
            if (index != shots.length - 1) {

                neighbors.add(shots[index + 1].category);
            }

            if (shots[index].category == Category.EITHER || shots[index].category == Category.ADVRT) {
                // If both are NO_ADVRT
                if (neighbors.contains(Category.NO_ADVRT) && neighbors.size() == 1) {
                    shots[index].category = Category.NO_ADVRT;
                }
                // If both are ADVRT
                else if (neighbors.contains(Category.ADVRT) && neighbors.size() == 1) {
                    shots[index].category = Category.ADVRT;
                }
            }

        }

        // Pass 2: Compare average amplitudes with neighbors. Choose closest
        // neighbor
        for (int index = 0; index < shots.length; ++index) {
            if (index != 0 && shots[index].category == Category.EITHER) {
                if (Math.abs(shots[index].avgAmp - shots[index - 1].avgAmp) <= 0.01) {
                    shots[index].category = shots[index - 1].category;
                }
            }

            if (index != shots.length - 1 && shots[index].category == Category.EITHER) {
                if (Math.abs(shots[index].avgAmp - shots[index + 1].avgAmp) <= 0.01) {
                    shots[index].category = shots[index + 1].category;
                }
            }
        }

        // Pass 3: Compare my signChangeFreq .
        for (int index = 1; index < shots.length - 1; ++index) {
            if (shots[index].category == Category.EITHER) {
                if (Math.abs(shots[index].signChangeFreq - shots[index - 1].signChangeFreq) < Math
                        .abs(shots[index].signChangeFreq - shots[index + 1].signChangeFreq)) {
                    shots[index].category = shots[index - 1].category;
                } else {
                    shots[index].category = shots[index+ 1].category;
                }
            }
        }

        for (int index = 0; index < shots.length; ++index) {
            if (shots[index].category == Category.EITHER) {
                for (int j = 1; j <= shots.length; ++j) {
                    if (index - j >= 0) {
                        if (shots[j].category != Category.EITHER) {
                            shots[index].category = shots[j].category;
                            break;
                        }
                    }
                    if (index + j < shots.length) {
                        if (shots[j].category != Category.EITHER) {
                            shots[index].category = shots[j].category;
                            break;
                        }
                    }
                }
            }
        }

        System.out.println("remving  ad video...");
      //  removeAdsFromVideo(videopath, videoout, shots);
        System.out.println("removing ad audio...");
      //  removeAdsFromAudio(audiopath, audioout, shots);
    }
 
 private static processShotLabelingFromAudioMetrics(List<Shot> shots){
        for (int index = 0; index < shots.size(); ++index) {
            Set<Category> neighbors = new HashSet<Category>();
            if (index != 0) {
                neighbors.add(shots.get(index - 1).category);
            }
            if (index != shots.size() - 1) {

                neighbors.add(shots.get(index + 1).category);
            }

            if (shots.get(index).category == Category.EITHER || shots.get(index).category == Category.ADVRT) {
                // If both are NO_ADVRT
                if (neighbors.contains(Category.NO_ADVRT) && neighbors.size() == 1) {
                    shots.get(index).category = Category.NO_ADVRT;
                }
                // If both are ADVRT
                else if (neighbors.contains(Category.ADVRT) && neighbors.size() == 1) {
                    shots.get(index).category = Category.ADVRT;
                }
            }

        }

        // Pruning Stage 2: Compare average amplitudes with neighbors frames. Choose closest
        // neighbor, with least avgAmp difference
        for (int index = 0; index < shots.size(); ++index) {
            if (index != 0 && shots.get(index).category == Category.EITHER) {
                if (Math.abs(shots.get(index).avgAmp - shots.get(index - 1).avgAmp) <= 0.01) {
                    shots.get(index).category = shots.get(index - 1).category;
                }
            }

            if (index != shots.size() - 1 && shots.get(index).category == Category.EITHER) {
                if (Math.abs(shots.get(index).avgAmp - shots.get(index + 1).avgAmp) <= 0.01) {
                    shots.get(index).category = shots.get(index + 1).category;
                }
            }
        }

        // Pruning Stag 3: Compare audio shot signChangeFreq .
        for (int index = 1; index < shots.size() - 1; ++index) {
            if (shots.get(index).category == Category.EITHER) {
                if (Math.abs(shots.get(index).signChangeFreq - shots.get(index - 1).signChangeFreq) < Math
                        .abs(shots.get(index).signChangeFreq - shots.get(index + 1).signChangeFreq)) {
                    shots.get(index).category = shots.get(index - 1).category;
                } else {
                    shots.get(index).category = shots.get(index+ 1).category;
                }
            }
        }

        for (int index = 0; index < shots.size(); ++index) {
            if (shots.get(index).category == Category.EITHER) {
                for (int j = 1; j <= shots.size(); ++j) {
                    if (index - j >= 0) {
                        if (shots.get(j).category != Category.EITHER) {
                            shots.get(index).category = shots.get(j).category;
                            break;
                        }
                    }
                    if (index + j < shots.size()) {
                        if (shots.get(j).category != Category.EITHER) {
                            shots.get(index).category = shots.get(j).category;
                            break;
                        }
                    }
                }
            }
        }

}
