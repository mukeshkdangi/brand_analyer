package csci576;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.openimaj.image.MBFImage;
import org.openimaj.image.pixel.statistics.HistogramModel;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.xuggle.XuggleAudio;
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
	
	//Refactor code to run on one for loop - shot and scene should be calculated on the boundaries
	//Use some metric with audio to determine if scene or shot boundary
	
	//Use some metric of audio to tell if advertisement
	
    public static void main( String[] args ) throws UnsupportedAudioFileException, IOException {

    	XuggleVideo video = new XuggleVideo(new File("/Users/skalli93/Desktop/USC Documents/CSCI576/finalProject/dataset/Videos/data_test1_cmp.avi"));
    	//XuggleVideo video = new XuggleVideo(new File("/Users/skalli93/Desktop/USC Documents/CSCI576/finalProject/dataset/Videos/data_test1_cmp.avi"));
    	
    	
    	List<Shot> shotList = FrameAnalyzer.detectShotBoundaries(video, 0.07f, 5, false);
		
    	System.out.println("Final List: ");
    	for (Shot shot : shotList) {
    		System.out.println("EndFrameNo: " + shot.getEndFrameNumber() + ", framesInBetween: " + shot.getShotLength() + ", MSD: " + shot.getMSD() 
    		+ ", Hist Score: " + shot.gethistogramScore() +", Category " + shot.category);
   	    }
    	
    	
    	    
    	String audioPath = "/Users/skalli93/Desktop/USC Documents/CSCI576/finalProject/dataset/Videos/data_test1.wav";
    	
    	FrameAnalyzer.analyzeAudio(audioPath, shotList);
    	FrameAnalyzer.processShotLabelingFromAudioMetrics(shotList);
    	
    	System.out.println("Final List:2 ");
    	for (Shot shot : shotList) {
    		System.out.println("EndFrameNo: " + shot.getEndFrameNumber() + ", framesInBetween: " + shot.getShotLength() + ", MSD: " + shot.getMSD() 
    		+ ", Hist Score: " + shot.gethistogramScore() +", Category " + shot.category);
   	    }
    	
    	
    	
    	//FrameAnalyzer.audioAnalyzer(shotList, audio, 2048f);
    	
    	//new VideoFeatureExtraction();
    	
//    	for (MBFImage mbfImage : video) {
//    	    DisplayUtilities.displayName(mbfImage.process(new CannyEdgeDetector()), "videoFrames");
//    	}
    	
    }
}
