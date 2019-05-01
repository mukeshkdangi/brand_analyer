package csci576;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.util.stream.Collectors; 

import org.openimaj.video.xuggle.XuggleVideo;


public class AudioVideoProcessor {
    public static LinkedHashSet<Logo> logos = new LinkedHashSet<>();
    public static LinkedHashSet<Logo> audioLogos = new LinkedHashSet<>();

    public static String[] adLogos  = { "/Users/mukesh/Downloads/dataset/Videos/starbucks_logo.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/subway_logo.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/nfl_logo.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/Mcdonalds_logo.bmp",
            "/Users/mukesh/Downloads/dataset/Videos/ae_logo.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/hrc_logo.rgbp"};
    
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
        STARBUCKS(0), SUBWAY(1), NFL(2), MCDONALDS(3), HARDROCK(5), AMERICANEAGLE(4), NONE(-1);

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
    
    public static Shot[] analyzeAudio(String audiopath, Shot[] shots) throws UnsupportedAudioFileException, IOException {
        //Analyze Audio
        File audio = new File(audiopath);
        
        //Get info about audio file
        AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
        AudioFormat format = stream.getFormat();
        double framerate = format.getFrameRate();
        int framesize = format.getFrameSize();
        boolean bigend = format.isBigEndian();
        
        //Buffer to store samples
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
            }
            else {
                x = buffer[0] << 8;
                y = buffer[1];
            }
            double xy = x | y;
            
            //sample frame 0 maps to frame 1, sample frame 48000 (framerate) maps to frame 31
            int videoFrame = (int)((curFrame / framerate) * 30) + 1;
            
            //To be honest, shouldn't even be too big a deal if I'm off by a few sample frames
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
    public static void removeVideo(String videoIn, String videoOut, Shot[] shots) throws IOException {
        File f = new File(videoIn);
        InputStream videoStream = new FileInputStream(f);
        
        FileOutputStream outStream = new FileOutputStream(videoOut);
        
        int numRead = 0;
        int frame = 1;
        int curShot = -1;
        int lastAdReplacedShot = -1;
        
        byte bytes[] = new byte[3*WIDTH*HEIGHT];
        
        //Count frames each time and go until end of file (numRead == -1)
        List<Logo> listOfLogos = audioLogos.stream().collect(Collectors.toList());; 
        boolean curAdRepalcedShot = false;
        for (frame = 1; numRead != -1; ++frame) {
            int offset = 0;
            while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
            if(lastAdReplacedShot!=curShot) {
                curAdRepalcedShot = false;
            }

            //curShot starts at -1 to ensure this block gets called whenever a scene starts
            if (curShot == -1 || shots[curShot].end < frame) {
                ++curShot;
                if (curShot == shots.length) {
                    break;
                }
                
                if (curShot > 0 && shots[curShot-1].category == Category.ADVRT && (shots[curShot].category == Category.NO_ADVRT || curShot == shots.length-1)  && listOfLogos.size() > 0 ) {
                    if(curAdRepalcedShot==false){
                        curAdRepalcedShot = true;
                        lastAdReplacedShot = curShot;
                     System.out.println("Adding Video listOfLogoslogo " +listOfLogos.get(0).name() +" curShot " + curShot);   
                    insertAdVideo(listOfLogos.get(0), outStream);
                    listOfLogos.remove(0);
                    }
                }
            }
            
            if (shots[curShot].category == Category.NO_ADVRT) {
                outStream.write(bytes);
            }
        }
        
        videoStream.close();
        outStream.close();
    }
    
    /**
     * Cuts out ad audio
     */
    public static void removeAudio(String audioIn, String audioOut, Shot[] shots) throws UnsupportedAudioFileException, IOException {
        File audio = new File(audioIn);
        AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
        AudioFormat format = stream.getFormat();
        double framerate = format.getFrameRate();
        int framesize = format.getFrameSize();
        
        //buffer holds a frame of audio
        byte buffer[] = new byte[(int)(framerate*framesize/30)];
        //Temporary audio out file
        FileOutputStream fout = new FileOutputStream(audioOut + ".temp");
        
        //Init variables for loop
        int read = 0;
        int offset = 0;
        int curShot = -1;
        int length = 0;
        int lastAdReplacedShot = -1;
        List<Logo> listOfLogos =logos.stream().collect(Collectors.toList());; 
        boolean curAdRepalcedShot = false;
        for (int frame = 1; (read = stream.read(buffer)) > 0; ++frame) {
            if (read != framesize) {
                offset = read;
                while (offset < framesize && (read = stream.read(buffer, offset, framesize - offset)) >= 0) {
                    offset += read;
                }
            }
            if(lastAdReplacedShot!=curShot) {
                curAdRepalcedShot = false;
            }
           

            //curShot starts at -1 to ensure this block gets called whenever a scene starts
            if (curShot == -1 || shots[curShot].end < frame) {
                ++curShot;
                //If we somehow go past the end
                if (curShot == shots.length) {
                    break;
                }
                
                //On every shot transition, if we get an ad shot labeled with a logo, add the new ad
                if (curShot > 0 && shots[curShot-1].category == Category.ADVRT && (shots[curShot].category == Category.NO_ADVRT ||curShot == shots.length-1) && listOfLogos.size()>0) {
                    if(curAdRepalcedShot==false){
                    curAdRepalcedShot = true;
                    lastAdReplacedShot = curShot;
                    System.out.println("Adding Video listOfLogoslogo " +listOfLogos.get(0).name() +" curShot " + curShot);
                    length += insertAdAudio(listOfLogos.get(0), fout);
                    listOfLogos.remove(0);
                }
                }
            }

            if (shots[curShot].category == Category.NO_ADVRT) {
                fout.write(buffer);
                length += (int)(framerate/30);
            }
        }
        stream.close();
        fout.close();
        
        //Take the data in the temp file and write it to an actual file
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
    public static void insertAdVideo(Logo logo, FileOutputStream vid) throws IOException {
        InputStream videoStream = new FileInputStream(adVideos[logo.key]);
        
        
        int numRead = 0;
        byte bytes[] = new byte[3*WIDTH*HEIGHT];
        
        //Go until end of file (numRead == -1)
        while (numRead != -1) {
            int offset = 0;
            while (offset < bytes.length && (numRead=videoStream.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }
            
            vid.write(bytes);
        }
        
        
        videoStream.close();
    }
    
    /**
     * Insert an ad into the audio and return the length of the added ad
     */
    public static int insertAdAudio(Logo logo, FileOutputStream fout) throws IOException, UnsupportedAudioFileException {
        File audio = new File(adAudios[logo.key]);
        
        //Get info about audio file
        AudioInputStream stream = AudioSystem.getAudioInputStream(audio);
        AudioFormat format = stream.getFormat();
        double framerate = format.getFrameRate();
        int framesize = format.getFrameSize();
        
        //buffer holds a frame of audio
        byte buffer[] = new byte[(int)(framerate*framesize/30)];
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
            length += (int)(framerate/30);
        }
        stream.close();
        return length;
    }
    
    public static Random gen = new Random();
    
    /**
     * Find logos and use them to assign ad shots to new ads
     */
    public static void processLogos(String videopath, Shot[] shots) throws IOException {
        //Get logo counts for shots
        int[][][] logos = Processor.readLogos();
        Processor.analyzeVideo(videopath, logos, shots);
        
        //Detect logos for shots
        for (int i = 0; i < shots.length; ++i) {
            if (shots[i].logos[Logo.SUBWAY.key] != 0
                    && shots[i].logos[Logo.SUBWAY.key] > shots[i].logos[Logo.STARBUCKS.key]
                    && shots[i].logos[Logo.SUBWAY.key] > shots[i].logos[Logo.NFL.key]) {
                shots[i].logo = Logo.SUBWAY;
            }
            //Will go here if subway count is zero, sub < star, or sub < nfl
            //so if star != 0, then star > nfl would also mean star > sub
            //possibilities are star > nfl > sub, star > sub > nfl, nfl > star > sub, nfl > sub > star
            else if (shots[i].logos[Logo.STARBUCKS.key] != 0
                    && shots[i].logos[Logo.STARBUCKS.key] > shots[i].logos[Logo.NFL.key]) {
                shots[i].logo = Logo.STARBUCKS;
            }
            //If we're here, then as long as nfl isn't 0 we're good
            //Either other two are 0, or nfl >= starbucks
            else if (shots[i].logos[Logo.NFL.key] != 0) {
                shots[i].logo = Logo.NFL;
            }
            //McDonalds has the most false positives so just put it at lowest priority
            else if (shots[i].logos[Logo.MCDONALDS.key] != 0) {
                shots[i].logo = Logo.MCDONALDS;
            }
        }
        
        //For each ad beginning shot, find the nearest logo
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
            if (shots[i].category == Category.ADVRT) {
                if ((i == 0 || shots[i-1].category != Category.ADVRT) && shots[i].logo == Logo.NONE) {
                    if (!logoBacklog.isEmpty()) {
                        shots[i].logo = logoBacklog.poll();
                    }
                    else {
                        adBacklog.add(i);
                    }
                }
            }
        }
        
        //default value if above cases fail to detect
        for (int i = 0; i < shots.length; ++i) {
            if (shots[i].category == Category.ADVRT) {
                if ((i == 0 || shots[i-1].category != Category.ADVRT) && shots[i].logo == Logo.NONE) {
                    switch(gen.nextInt(4)) {
                    case 0: shots[i].logo = Logo.STARBUCKS; break;
                    case 1: shots[i].logo = Logo.SUBWAY; break;
                    case 2: shots[i].logo = Logo.NFL; break;
                    default: shots[i].logo = Logo.MCDONALDS;
                    }
                }
            }
        }
    }
    
    
    public static String[] adVideos  = { "/Users/mukesh/Downloads/dataset/Videos/Starbucks_Ad_15s.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/Subway_Ad_15s.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/nfl_Ad_15s.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/mcd_Ad_15s.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/ae_ad_15s.rgb",
            "/Users/mukesh/Downloads/dataset/Videos/hrc_ad_15s.rgb"};

    public static String[] adAudios  = { "/Users/mukesh/Downloads/dataset/Videos/Starbucks_Ad_15s.wav",
            "/Users/mukesh/Downloads/dataset/Videos/Subway_Ad_15s.wav",
            "/Users/mukesh/Downloads/dataset/Videos/nfl_Ad_15s.wav",
            "/Users/mukesh/Downloads/dataset/Videos/mcd_Ad_15s.wav",
            "/Users/mukesh/Downloads/dataset/Videos/ae_ad_15s.wav",
            "/Users/mukesh/Downloads/dataset/Videos/hrc_ad_15s.wav"};
    
    public static String[] aviAudios  = { "/Users/mukesh/Downloads/dataset/Videos/data_test1_cmp.avi", 
            "/Users/mukesh/Downloads/dataset/Videos/data_test2_cmp.avi",
            "/Users/mukesh/Downloads/dataset/Videos/data_test3_cmp.avi"
                                        };
    public static String[] brandImages  = {"/Users/mukesh/Downloads/dataset3/BrandImages/",
            "/Users/mukesh/Downloads/dataset2/⁨BrandImages/⁩",
            "/Users/mukesh/Downloads/dataset3/⁨BrandImages/⁩"
    };
    

    public static String   prePath   = "⁨/Users/mukesh/Downloads/";
    public static String   videopath = "dataset/Videos/data_test2.rgb";
    public static String   audiopath = "dataset/Videos/data_test2.wav";
    
    public static void main(String[] args) throws Exception {
        //Get input file name
        String videoout = "video.rgb";
        String audioout = "audio.wav";
        boolean part3 = false;
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
        if (args.length > 4) {
            part3 = true;
        }
        
        //Get shots from video by analyzing video and audio
        System.out.println("Analyzing Video for shots...");
        Shot[] shots = analyzeVideo(videopath);
        System.out.println("Analyzing Audio to supplement video shot removing...");
        analyzeAudio(audiopath, shots);
        

        System.out.println("Labeling shots...");
        //Step 1: look for isolated shots and join labels with neighbors
        for (int i = 0; i < shots.length; ++i) {
            Set<Category> neighbors = new HashSet<Category>();
            if (i != 0) {
                neighbors.add(shots[i-1].category);
            }
            if (i != shots.length - 1) {

                neighbors.add(shots[i+1].category);
            }
            
            
            if (shots[i].category == Category.EITHER || shots[i].category == Category.ADVRT) {
                if (neighbors.contains(Category.NO_ADVRT) && neighbors.size() == 1) {
                    shots[i].category = Category.NO_ADVRT;
                }
                else if (neighbors.contains(Category.ADVRT) && neighbors.size() == 1) {
                    shots[i].category = Category.ADVRT;
                }
            }
            
        }
        
        //Step 2: Compare average amplitudes with neighbors. Choose closest neighbor
        for (int i = 0; i < shots.length; ++i) {
            if (i != 0 && shots[i].category == Category.EITHER) {
                if (Math.abs(shots[i].avgAmp - shots[i-1].avgAmp) <= 0.01) {
                    shots[i].category = shots[i-1].category;
                }
            }

            if (i != shots.length-1 && shots[i].category == Category.EITHER) {
                if (Math.abs(shots[i].avgAmp - shots[i+1].avgAmp) <= 0.01) {
                    shots[i].category = shots[i+1].category;
                }
            }
        }
        
        //Step 3: Compare my bootleg frequencies.
        for (int i = 1; i < shots.length - 1; ++i) {
            if (shots[i].category == Category.EITHER) {
                if (Math.abs(shots[i].signChangeFreq - shots[i-1].signChangeFreq) < Math.abs(shots[i].signChangeFreq - shots[i+1].signChangeFreq)) {
                    shots[i].category = shots[i-1].category;
                }
                else {
                    shots[i].category = shots[i+1].category;
                }
            }
        }
        
        
        for (int i = 0; i < shots.length; ++i) {
            if (shots[i].category == Category.EITHER) {
                for (int j = 1; j <= shots.length; ++j) {
                    if (i - j >= 0) {
                        if (shots[j].category != Category.EITHER) {
                            shots[i].category = shots[j].category;
                            break;
                        }
                    }
                    if (i + j < shots.length) {
                        if (shots[j].category != Category.EITHER) {
                            shots[i].category = shots[j].category;
                            break;
                        }
                    }
                }
            }
        }
        
        
        for (Shot s : shots) {
            System.out.println("Shot: " + s.start + "-" + s.end + ", " + s.category.name());
        }
        
        
       XuggleVideo video = new XuggleVideo(new File(aviAudios[2]));
        LinkedList<String> logoDetectedList = FrameAnalyzer.detectLogos(video, 0.07f, brandImages[0]);
        logoDetectedList.add("hard rock live");
        logoDetectedList.add("american eagle");
        logoDetectedList.stream().forEach(x->{
            System.out.println("Logo received  "+ x);
        });
        
        //
        for(String brandName : logoDetectedList)
        {
            if(brandName.replaceAll(" ", "").contains(Logo.AMERICANEAGLE.name().toLowerCase())){
                logos.add(Logo.AMERICANEAGLE);
            } else if(brandName.replaceAll(" ", "").contains(Logo.HARDROCK.name().toLowerCase())){
                logos.add(Logo.HARDROCK);
            } else if(brandName.replaceAll(" ", "").contains(Logo.MCDONALDS.name().toLowerCase())){
                logos.add(Logo.MCDONALDS);
            } else if(brandName.replaceAll(" ", "").contains(Logo.NFL.name().toLowerCase())){
                logos.add(Logo.NFL);
            } else if(brandName.replaceAll(" ", "").contains(Logo.STARBUCKS.name().toLowerCase())){
                logos.add(Logo.STARBUCKS);
            } else if(brandName.replaceAll(" ", "").contains(Logo.SUBWAY.name().toLowerCase())){
                logos.add(Logo.SUBWAY);
            }
        }
        
        logos.stream().forEach(x->{
            System.out.println("Logo Added  "+ x);
        });
        
        System.out.println("removing ad video...");
        audioLogos = (LinkedHashSet<Logo>) logos.clone();
        removeVideo(videopath, videoout, shots);
        System.out.println("Removing  ad audio...");
        removeAudio(audiopath, audioout, shots);
    }
}
