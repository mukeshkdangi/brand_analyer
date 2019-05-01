package csci576;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.processing.motion.GridMotionEstimator;
import org.openimaj.video.processing.motion.MotionEstimator;
import org.openimaj.video.processing.motion.MotionEstimatorAlgorithm;
import org.openimaj.video.translator.FImageToMBFImageVideoTranslator;
import org.openimaj.video.translator.MBFImageToFImageVideoTranslator;
import org.openimaj.video.xuggle.XuggleVideo;

import com.google.protobuf.ByteString;


public class FrameAnalyzer {

    /*
     * Method that will plot the calculated motion vectors between frames
     * 
     * @param video The input video file to process
     * 
     */

    public static void displayMotionVectors(XuggleVideo video) {

        final MotionEstimator me = new GridMotionEstimator(new MBFImageToFImageVideoTranslator(video),
                new MotionEstimatorAlgorithm.PHASE_CORRELATION(), 10, 10, true);

        final VideoDisplay<MBFImage> vd = VideoDisplay.createVideoDisplay(new FImageToMBFImageVideoTranslator(me));
        vd.addVideoListener(new VideoDisplayListener<MBFImage>() {
            @Override
            public void afterUpdate(VideoDisplay<MBFImage> display) {
            }

            @Override
            public void beforeUpdate(MBFImage frame) {
                for (final Point2d p : me.motionVectors.keySet()) {
                    final Point2d p2 = me.motionVectors.get(p);
                    frame.drawLine((int) p.getX(), (int) p.getY(), (int) (p.getX() + p2.getX()),
                            (int) (p.getY() + p2.getY()), 2, new Float[] { 1f, 0f, 0f });
                }
            }
        });

    }

    /*
     * Method that will generate a histogram from an input frame.
     * 
     * @param frame The input frame to process
     * 
     */

    public static MultidimensionalHistogram generateHistogram(MBFImage frame, int nBins) {

        HistogramModel model = new HistogramModel(8, 8, 8);
        model.estimateModel(frame);
        MultidimensionalHistogram histogram = model.histogram;

        return histogram;

    }

    /*
     * Method that will calculate the MSD of a current frame and a previous frame
     * 
     * @param current The current frame
     * 
     * @param last The previous frame
     * 
     */

    public static float meanSquareDifference(FImage current, FImage last) {

        // compute the squared difference from the last frame
        float val = 0;
        for (int y = 0; y < current.height; y++) {
            for (int x = 0; x < current.width; x++) {
                final float diff = (current.pixels[y][x] - last.pixels[y][x]);
                val += diff * diff;
            }
        }

        float meanVal = val / (current.height * current.width);

        return meanVal;

    }
    
    
    public static LinkedList<String> detectLogos(XuggleVideo video, float threshold, String path) throws Exception{
        

        // Get the first frame
        MBFImage lastFrame = video.getNextFrame();
        FImage last = lastFrame.flatten();

        // Initialize frame count
        int lastFrameNo = 0;
        float currentFrameNo = 1;

        // Initialize output list
        LinkedList<String> outputList = new LinkedList<String>();
        
//      LogoDetection ld = new LogoDetection();
//      List<String> logos = ld.run();

        long numOfFrames = video.countFrames();
        List<String> logos = LogoDetection.run(path);
        logos.stream().forEach(x->{
            System.out.println("logo Found  in folder " + x);
        });
        
        // Iterate through the frames
        for (MBFImage currentFrame : video) {
            if(currentFrameNo == 8399) {
                break;
            }
        
            final FImage current = currentFrame.flatten();

        
            if (currentFrameNo % 20 == 0) {
                //Call Logo detector here 
                //System.out.println("20th frame!!!");
                File outputFile = new File("/Users/mukesh/Downloads/dataset/image.jpg");
                ImageUtilities.write(current, "JPG", outputFile);
                ByteString byteString = ByteString.readFrom(new FileInputStream(outputFile));
                
                outputList.addAll(LogoDetection.matchLogoToImage(null, System.out, byteString, logos));
            }
            
            
            //System.out.println(lastFrameNo + " out of " + numOfFrames + " frames" );
            
            // Set the current frame to the last frame
            last = current;
            lastFrame = currentFrame;
            lastFrameNo++;
            currentFrameNo++;

        }
          Object[] array = outputList.toArray();
        
          // print the array
          for (int i = 0; i < outputList.size(); i++) {
             System.out.println("Logos for this shot:" + array[i]);
          }
    return outputList;      
    }
    

    
    
}
