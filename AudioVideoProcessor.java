package csci576;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;

import com.google.protobuf.ByteString;

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
     * Analyzes entropies in a video and divides it into shots
     */
    public static Shot[] analyzeVideo(String videopath) throws IOException {
        // Analyze Video

        // Turn file into path
        File f = new File(videopath);
        InputStream videoStream = new FileInputStream(f);

        // Previous values for calculating differences
        double prevEntY = 0, prevEntR = 0, prevEntG = 0, prevEntB = 0;
        double prevDifY = 0, prevDifR = 0, prevDifG = 0, prevDifB = 0;

        // Get the shot transition frames; shots are between these frames
        ArrayList<Integer> borders = new ArrayList<Integer>();

        int numRead = 0;
        int frame = 1;
        byte bytes[] = new byte[3 * WIDTH * HEIGHT];

        for (frame = 1; numRead != -1; ++frame) {

            int offset = 0;
            while (offset < bytes.length && (numRead = videoStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            double entY = 0;
            double entR = 0;
            double sumG = 0;
            double sumB = 0;
            // Space of each component to calculate frequencies
            int[] YSpace = new int[256];
            int[] RSpace = new int[256];
            int[] GSpace = new int[256];
            int[] BSpace = new int[256];

            int ind = 0;
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    // Read RGB from buffer for frame
                    int r = bytes[ind];
                    int g = bytes[ind + HEIGHT * WIDTH];
                    int b = bytes[ind + HEIGHT * WIDTH * 2];

                    int Y = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                    // Normalize values
                    Y = Math.max(0, Y);
                    Y = Math.min(255, Y);
                    r = Math.max(0, r);
                    r = Math.min(255, r);
                    g = Math.max(0, g);
                    g = Math.min(255, g);
                    b = Math.max(0, b);
                    b = Math.min(255, b);

                    // Increase frequency for those values in their respective
                    // spaces
                    ++YSpace[Y];
                    ++RSpace[r];
                    ++GSpace[g];
                    ++BSpace[b];

                    ind++;
                }
            }

            // Calculate entropies for each space
            for (int index : YSpace) {
                if (index != 0) {
                    double prob = index * 1.0 / (WIDTH * HEIGHT);
                    entY += prob * Math.log(index) / Math.log(2);
                }
            }
            for (int index : RSpace) {
                if (index != 0) {
                    double prob = index * 1.0 / (WIDTH * HEIGHT);
                    entR += prob * Math.log(index) / Math.log(2);
                }
            }
            for (int index : GSpace) {
                if (index != 0) {
                    double prob = index * 1.0 / (WIDTH * HEIGHT);
                    sumG += prob * Math.log(index) / Math.log(2);
                }
            }
            for (int index : BSpace) {
                if (index != 0) {
                    double prob = index * 1.0 / (WIDTH * HEIGHT);
                    sumB += prob * Math.log(index) / Math.log(2);
                }
            }

            // Calculate the change in entropies with the previous values
            double difY = Math.abs(prevEntY - entY);
            double difR = Math.abs(prevEntR - entR);
            double difG = Math.abs(prevEntG - sumG);
            double difB = Math.abs(prevEntB - sumB);

            boolean checked = true;

            // If the change in entropy is above a certain small threshold
            if (difY > 0.4 || (difR > 0.35 || difG > 0.35 || difB > 0.35)) {
                // Then if this change is significantly bigger than previous
                // changes
                if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 100)
                        || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 100)
                        || (prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 100)
                        || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 100)) {
                    // Add to shot borders list
                    checked = false;
                    borders.add(frame);
                }
            }
            // If the change in entropy is above a certain medium threshold
            if (difY > 0.5 || (difR > 0.5 || difG > 0.5 || difB > 0.5)) {
                // Then if this change is moderately bigger than previous
                // changes
                if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 50)
                        || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 50)
                        || (prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 50)
                        || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 50)) {
                    // If we haven't already added this value
                    if (checked) {
                        checked = false;
                        borders.add(frame);
                    }
                }
            }
            // If the change in entropy is above a certain large threshold
            if (difY > 0.7 || (difR > 0.7 || difG > 0.7 || difB > 0.7)) {
                // Then if this change is slightly bigger than previous changes
                if ((prevDifY == 0 || prevDifY != 0 && difY / prevDifY > 10)
                        || (prevDifR == 0 || prevDifR != 0 && difR / prevDifR > 10)
                        || (prevDifG == 0 || prevDifG != 0 && difG / prevDifG > 10)
                        || (prevDifB == 0 || prevDifB != 0 && difB / prevDifB > 10)) {
                    // If we haven't already added this value
                    if (checked) {
                        borders.add(frame);
                    }
                }
            }

            // Set previous values
            prevEntY = entY;
            prevDifY = difY;
            prevDifR = difR;
            prevDifG = difG;
            prevDifB = difB;
            prevEntR = entR;
            prevEntG = sumG;
            prevEntB = sumB;
        }

        // Close the input stream like a responsible adult
        videoStream.close();

        // Last shot ends when video ends
        borders.add(frame);

        // N borders means N-1 shots
        Shot shots[] = new Shot[borders.size() - 1];

        // Convert borders to shots
        int start = borders.get(0);
        for (int i = 1; i < borders.size(); ++i) {
            int end = borders.get(i);
            shots[(i - 1)] = new Shot(start, end - 1);

            start = end;
        }

        for (Shot shot : shots) {
            if (shot.length() < 120) {
                shot.category = Category.ADVRT;
            } else if (shot.length() > 300) {
                shot.category = Category.NO_ADVRT;
            }
        }

        return shots;
    }

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

            double xy = buffer[bigend ? 0 : 1] << 8 | buffer[bigend ? 1 : 0];

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

        for (Shot s : shots)
            s.avgSample();

        return shots;
    }

    /**
     * Cuts out the ad frames in a video
     */
    public static void removeAdVideo(String videoIn, String videoOut, Shot[] shots) throws Exception {

        File f = new File(videoIn);
        InputStream videoStream = new FileInputStream(f);

        FileOutputStream outStream = new FileOutputStream(videoOut);

        // Some initial data for the for loop
        int numRead = 0;
        int frame = 1;
        int curShot = -1;
        byte bytes[] = new byte[3 * WIDTH * HEIGHT];

       // List<String> logos = LogoDetection.run();
        int lastAdDetectedFrame = -1;
        for (frame = 1; numRead != -1; ++frame) {

            int offset = 0;
            while (offset < bytes.length && (numRead = videoStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            if (frame % 20 == 0) {
                File file = new File("/Users/mukesh/Downloads/dataset/image.jpg");
                MBFImage mbfImage = new MBFImage(WIDTH, HEIGHT);
                mbfImage.internalAssign(bytes, WIDTH, HEIGHT);
                ImageUtilities.write(mbfImage, file);
                List<String> brandsDetected = LogoDetection.matchLogoToImage(
                        ByteString.readFrom(new FileInputStream(file)));

                for (String brand : brandsDetected) {
                    if (brand.contains(Logo.NFL.name().toLowerCase())) {
                        shots[curShot].logo = Logo.NFL;
                        lastAdDetectedFrame = curShot;
                    } else if (brand.contains(Logo.MCDONALDS.name().toLowerCase())) {
                        shots[curShot].logo = Logo.MCDONALDS;
                        lastAdDetectedFrame = curShot;
                    } else if (brand.contains(Logo.STARBUCKS.name().toLowerCase())) {
                        shots[curShot].logo = Logo.STARBUCKS;
                        lastAdDetectedFrame = curShot;
                    }

                    else if (brand.contains(Logo.SUBWAY.name().toLowerCase())) {
                        shots[curShot].logo = Logo.SUBWAY;
                        lastAdDetectedFrame = curShot;
                    }

                }
            }

            // curShot starts at -1 to ensure this block gets called whenever a
            // scene starts
            boolean flag = curShot == -1;
            if (flag || shots[curShot].end < frame) {
                
             if(!flag){
                System.out.println(
                        "shots[curShot].logo *******lastAdDetectedFrame " + lastAdDetectedFrame + "curShot : " + curShot + " Logo " + shots[curShot].logo);

                if (shots[curShot].category == Category.ADVRT && lastAdDetectedFrame != -1  && shots[lastAdDetectedFrame].logo != Logo.NONE ) {
                    insertRelevantAdVideo(shots[lastAdDetectedFrame].logo, outStream);
                    lastAdDetectedFrame = -1;
                }
             }
                
                ++curShot;
                if (curShot == shots.length) {
                    break;
                }
                
                if (shots[curShot].category == Category.NO_ADVRT) {
                    outStream.write(bytes);
                }
            }

        }

        // Close the streams like a responsible adult
        videoStream.close();
        outStream.close();

    }

    public static void removeAdAudio(String audioIn, String audioOut, Shot[] shots)
            throws UnsupportedAudioFileException, IOException {
        // Cut Audio
        File audio = new File(audioIn);

        // Get info about audio file
        AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
        AudioFormat format = stream.getFormat();
        double framerate = format.getFrameRate();
        int framesize = format.getFrameSize();

        // buffer holds a frame of audio
        byte buffer[] = new byte[(int) (framerate * framesize / 30)];
        // Temporary audio out file
        FileOutputStream fout = new FileOutputStream(audioOut + ".temp");

        // Init variables for loop
        int read = 0;
        int offset = 0;
        int curShot = -1;
        int length = 0;
        int lastLogoDetectedShot =-1;
        for (int frame = 1; (read = stream.read(buffer)) > 0; ++frame) {
            if (read != framesize) {
                offset = read;
                while (offset < framesize && (read = stream.read(buffer, offset, framesize - offset)) >= 0) {
                    offset += read;
                }
            }
            
            if(curShot !=-1 && shots[curShot].logo != Logo.NONE)
            {
                lastLogoDetectedShot = curShot;
            }

            // curShot starts at -1 to ensure this block gets called whenever a
            // scene starts
            boolean flag = curShot == -1;
            if (flag || shots[curShot].end < frame) {
                
               

                // On every shot transition, if we get an ad shot labeled with a
                // logo, add the new ad
                if(!flag){
                    if (shots[curShot].category == Category.ADVRT && lastLogoDetectedShot!=-1 && shots[lastLogoDetectedShot].logo != Logo.NONE ) {
                        length += insertRelevantAdAudio(shots[lastLogoDetectedShot].logo, fout);
                        lastLogoDetectedShot =-1;
                    }
                }
                
                
                ++curShot;
                // If we somehow go past the end
                if (curShot == shots.length) {
                    break;
                }
                
                if (shots[curShot].category == Category.NO_ADVRT) {
                    fout.write(buffer);
                    length += (int) (framerate / 30);
                }
            }

            
        }
        // Close these streams
        stream.close();
        fout.close();

        File out = new File(audioOut);
        FileInputStream fin = new FileInputStream(audioOut + ".temp");
        AudioInputStream as = new AudioInputStream(fin, format, length);
        AudioSystem.write(as, AudioFileFormat.Type.WAVE, out);
        fin.close();
        as.close();
    }

    /**
     * Insert an ad into a video
     */

    public static void insertRelevantAdVideo(Logo logo, FileOutputStream vid) throws IOException {

        System.out.println("inserting ad for logo " + adVideos[logo.key]);
        InputStream videoStream = new FileInputStream(adVideos[logo.key]);
        // InputStream videoStream = new
        // FileInputStream("/Users/mukesh/Downloads/dataset/Videos/Starbucks_Ad_15s.rgb");

        int numRead = 0;
        byte bytes[] = new byte[3 * WIDTH * HEIGHT];
        while (numRead != -1) {
            int offset = 0;
            while (offset < bytes.length && (numRead = videoStream.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }
            vid.write(bytes);
        }
        videoStream.close();
    }

    /**
     * Insert an ad into the audio and return the length of the added ad
     */
    public static int insertRelevantAdAudio(Logo logo, FileOutputStream fout)
            throws IOException, UnsupportedAudioFileException {
        System.out.println("Adding  audio ads for logo ...." + logo.name());
        File audio = new File(adAudios[logo.key]);
        // File audio = new
        // File("/Users/mukesh/Downloads/dataset/Videos/Starbucks_Ad_15s.wav");

        AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
        AudioFormat format = stream.getFormat();
        double framerate = format.getFrameRate();
        int framesize = format.getFrameSize();

        byte buffer[] = new byte[(int) (framerate * framesize / 30)];
        int read = 0;
        int offset = 0;
        int length = 0;
        while ((read = stream.read(buffer)) > 0) {
            if (read != framesize) {
                offset = read;
                while (offset < framesize && (read = stream.read(buffer, offset, framesize - offset)) >= 0) {
                    offset += read;
                }
            }

            fout.write(buffer);
            length += (int) (framerate / 30);
        }
        stream.close();
        return length;
    }

    public static Random   gen       = new Random();

    public static String[] adVideos  = { "/Users/mukesh/Downloads/dataset/Videos/Starbucks_Ad_15s.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/Subway_Ad_15s.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/nfl_Ad_15s.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/mcd_Ad_15s.rgb" };

    public static String[] adAudios  = { "/Users/mukesh/Downloads/dataset/Videos/Starbucks_Ad_15s.wav",
            "/Users/mukesh/Downloads/dataset/Videos/Subway_Ad_15s.wav",
            "/Users/mukesh/Downloads/dataset/Videos/nfl_Ad_15s.wav",
            "/Users/mukesh/Downloads/dataset/Videos/mcd_Ad_15s.wav" };

    public static String   prePath   = "⁨/Users/mukesh/Downloads/";
    public static String   videopath = "dataset/Videos/data_test2.rgb";
    public static String   audiopath = "dataset/Videos/data_test2.wav";

    public static void main(String[] args) throws Exception {

        String videoOutput = "video.rgb";
        String audioOutput = "audio.wav";

        if (args.length > 0) {
            videopath = args[0];
        }
        if (args.length > 1) {
            audiopath = args[1];
        }
        if (args.length > 2) {
            videoOutput = args[2];
        }
        if (args.length > 3) {
            audioOutput = args[3];
        }

        // Get shots from video by analyzing video and audio
        System.out.println("Analyzing Video for shots...");
        Shot[] shots = analyzeVideo(videopath);
        System.out.println("Analyzing Audio to supplement video shot bounds...");
        analyzeAudio(audiopath, shots);

        System.out.println("Labeling shots...");
        // Step 1: look for isolated shots and join labels with neighbors
        for (int index = 0; index < shots.length; ++index) {
            Set<Category> neighbors = new HashSet<Category>();
            if (index != 0) {
                neighbors.add(shots[index - 1].category);
            }
            if (index != shots.length - 1) {
                neighbors.add(shots[index + 1].category);
            }

            if (shots[index].category == Category.EITHER || shots[index].category == Category.ADVRT) {
                if (neighbors.contains(Category.NO_ADVRT) && neighbors.size() == 1) {
                    shots[index].category = Category.NO_ADVRT;
                } else if (neighbors.contains(Category.ADVRT) && neighbors.size() == 1) {
                    shots[index].category = Category.ADVRT;
                }
            }

        }

        // Now we'll take audio file help to Categorise EITHER shots
        // Step 2: Compare average amplitudes with neighbors. Choose closest
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

        // Step 3: Compare my signChangeFreq frequencies.
        for (int index = 1; index < shots.length - 1; ++index) {
            if (shots[index].category == Category.EITHER) {
                if (Math.abs(shots[index].signChangeFreq - shots[index - 1].signChangeFreq) < Math
                        .abs(shots[index].signChangeFreq - shots[index + 1].signChangeFreq)) {
                    shots[index].category = shots[index - 1].category;
                } else {
                    shots[index].category = shots[index + 1].category;
                }
            }
        }

        for (int index = 0; index < shots.length; ++index) {
            if (shots[index].category == Category.EITHER) {
                for (int j = 1; j <= shots.length; ++j) {
                    if (index - j >= 0) {
                        if (shots[index].category != Category.EITHER) {
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
        // processLogos(videopath, shots);
        System.out.println("Removing the ad video...");
        removeAdVideo(videopath, videoOutput, shots);
        System.out.println("Remvong the ad audio...");
        removeAdAudio(audiopath, audioOutput, shots);
    }

    public static void processLogos(String videopath, Shot[] shots) {
        // Detect logos for shots
        for (int i = 0; i < shots.length; ++i) {
            if (shots[i].logos[Logo.SUBWAY.key] != 0
                    && shots[i].logos[Logo.SUBWAY.key] > shots[i].logos[Logo.STARBUCKS.key]
                    && shots[i].logos[Logo.SUBWAY.key] > shots[i].logos[Logo.NFL.key]) {
                shots[i].logo = Logo.SUBWAY;
            }
            // Will go here if subway count is zero, sub < star, or sub < nfl
            // so if star != 0, then star > nfl would also mean star > sub
            // possibilities are star > nfl > sub, star > sub > nfl, nfl > star
            // > sub, nfl > sub > star
            else if (shots[i].logos[Logo.STARBUCKS.key] != 0
                    && shots[i].logos[Logo.STARBUCKS.key] > shots[i].logos[Logo.NFL.key]) {
                shots[i].logo = Logo.STARBUCKS;
            }
            // If we're here, then as long as nfl isn't 0 we're good
            // Either other two are 0, or nfl >= starbucks
            else if (shots[i].logos[Logo.NFL.key] != 0) {
                shots[i].logo = Logo.NFL;
            }
            // McDonalds has the most false positives so just put it at lowest
            // priority
            else if (shots[i].logos[Logo.MCDONALDS.key] != 0) {
                shots[i].logo = Logo.MCDONALDS;
            }
        }

        // For each ad beginning shot, find the nearest logo
        Queue<Logo> logoBacklog = new LinkedList<Logo>();
        Queue<Integer> adBacklog = new LinkedList<Integer>();
        for (int i = 0; i < shots.length; ++i) {
            if (shots[i].logo != Logo.NONE) {
                logoBacklog.add(shots[i].logo);
                shots[i].logo = Logo.NONE;

                if (!adBacklog.isEmpty()) {
                    shots[adBacklog.poll()].logo = logoBacklog.poll();
                }
            }
            if (shots[i].category == Category.NO_ADVRT) {
                if ((i == 0 || shots[i - 1].category != Category.NO_ADVRT) && shots[i].logo == Logo.NONE) {
                    if (!logoBacklog.isEmpty()) {
                        shots[i].logo = logoBacklog.poll();
                    } else {
                        adBacklog.add(i);
                    }
                }
            }
        }

        // default value if above cases fail to detect
        for (int i = 0; i < shots.length; ++i) {
            if (shots[i].category == Category.NO_ADVRT) {
                if ((i == 0 || shots[i - 1].category != Category.NO_ADVRT) && shots[i].logo == Logo.NONE) {
                    switch (gen.nextInt(4)) {
                    case 0:
                        shots[i].logo = Logo.STARBUCKS;
                        break;
                    case 1:
                        shots[i].logo = Logo.SUBWAY;
                        break;
                    case 2:
                        shots[i].logo = Logo.NFL;
                        break;
                    default:
                        shots[i].logo = Logo.MCDONALDS;
                    }
                }
            }
        }
    }

}
