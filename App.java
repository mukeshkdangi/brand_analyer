package csci576;
import java.io.File;
import org.openimaj.image.MBFImage;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.xuggle.XuggleVideo;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.statistics.distribution.MultidimensionalHistogram;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.processing.motion.GridMotionEstimator;
import org.openimaj.video.processing.motion.MotionEstimator;
import org.openimaj.video.processing.motion.MotionEstimatorAlgorithm;
import org.openimaj.video.translator.FImageToMBFImageVideoTranslator;
import org.openimaj.video.translator.MBFImageToFImageVideoTranslator;
import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;


/**
 * 
 * CSCI576 FINAL PROJECT
 *
 */
public class App {
	
	/*
	 * Method that will plot the calculated motion vectors between frames
	 * 
	 * @param video The input video file to process
	 * 
	 */
	
	public static void displayMotionVectors(XuggleVideo video) {
		
		final MotionEstimator me = new GridMotionEstimator(
				new MBFImageToFImageVideoTranslator(video),
				new MotionEstimatorAlgorithm.PHASE_CORRELATION(), 10, 10, true);

		final VideoDisplay<MBFImage> vd = VideoDisplay.createVideoDisplay(
				new FImageToMBFImageVideoTranslator(me));
		vd.addVideoListener(new VideoDisplayListener<MBFImage>()
		{
			@Override
			public void afterUpdate(VideoDisplay<MBFImage> display)
			{
			}

			@Override
			public void beforeUpdate(MBFImage frame)
			{
				for (final Point2d p : me.motionVectors.keySet())
				{
					final Point2d p2 = me.motionVectors.get(p);
					frame.drawLine((int) p.getX(), (int) p.getY(),
							(int) (p.getX() + p2.getX()),
							(int) (p.getY() + p2.getY()),
							2, new Float[] { 1f, 0f, 0f });
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
	
	public static MultidimensionalHistogram generateHistogram(MBFImage frame) {
		
		HistogramModel model = new HistogramModel(2, 2, 2);
		model.estimateModel(frame);
		MultidimensionalHistogram histogram = model.histogram;
		
		return histogram;
		
	}
	
	/*
	 * Method that will calculate the MSD of a current frame and a previous frame 
	 * 
	 * @param current The current frame
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
	
	/*
	 * Method that will determine where the shot boundaries.
	 * It uses MSD and histograms to compare to a threshold.
	 * 
	 * @param video The input video file to process. 
	 * @param threshold The threshold to determine a shot boundary.
	 * @param minFrames The minimum allowable frames per shot. 
	 * 
	 */
	
	
	public static void shotBoundaryDetector(XuggleVideo video, float threshold){
		
		// get the first frame
		MBFImage lastFrame = video.getNextFrame();
		MultidimensionalHistogram lastHistogram = generateHistogram(lastFrame);
		FImage last = lastFrame.flatten();
		
		//Initialize frame count
		float lastFrameNo = 0;
		float currentFrameNo = 1;
		
		// iterate through the frames
		for (final MBFImage currentFrame : video) {
			final FImage current = currentFrame.flatten();
			
			float meanVal = meanSquareDifference(current, last);
			
			MultidimensionalHistogram currentHistogram = generateHistogram(currentFrame);
			double distanceScore = currentHistogram.compare(lastHistogram, DoubleFVComparison.EUCLIDEAN);
			
			//System.out.println("Current Histogram: " + currentHistogram.toString());
			//System.out.println("Last Histogram: " + lastHistogram.toString());
			
			// might need adjust threshold:
			if (meanVal > threshold && distanceScore > threshold) {
				System.out.println("New Shot detected");
				DisplayUtilities.displayName(current, "debugCurrent");
				DisplayUtilities.displayName(last, "debugLast");

			}
			
			System.out.println("From frame " + lastFrameNo + ", to " + currentFrameNo + ", meanVal = " + meanVal + ", histogramScore = " + distanceScore);					
			
			// set the current frame to the last frame
			
			last = current;
			lastFrame = currentFrame;
			lastHistogram = currentHistogram;
			
			lastFrameNo ++;
			currentFrameNo ++;
			
		}
						
		
	}
	

    public static void main( String[] args ) {

    	XuggleVideo video = new XuggleVideo(new File("/Users/mukesh/Downloads/dataset/Videos/data_test1_cmp.avi"));
    	
    	//VideoDisplay<MBFImage> display = VideoDisplay.createVideoDisplay(video);
    	
    	shotBoundaryDetector(video, 0.055f);
		
//    	for (MBFImage mbfImage : video) {
//    	    DisplayUtilities.displayName(mbfImage.process(new CannyEdgeDetector()), "videoFrames");
//    	}
    	
    }
}
