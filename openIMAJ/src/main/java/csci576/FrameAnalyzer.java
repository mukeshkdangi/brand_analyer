package csci576;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.openimaj.feature.DoubleFVComparison;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
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

import csci576.Shot.Category;

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

	/*
	 * Method that will determine where the shot boundaries. It uses MSD and
	 * histograms to compare to a threshold.
	 * 
	 * @param video The input video file to process.
	 * 
	 * @param threshold The threshold to determine a shot boundary.
	 * 
	 * @param minFrames The minimum allowable frames per shot.
	 * 
	 */

	public static List<Shot> detectShotBoundaries(XuggleVideo video, float threshold, int minFrames, Boolean verbose) {

		// Get the first frame
		MBFImage lastFrame = video.getNextFrame();
		FImage last = lastFrame.flatten();

		// Initialize frame count
		int lastFrameNo = 0;
		float currentFrameNo = 1;

		// Initialize histogram
		int nBins = video.getWidth();
		MultidimensionalHistogram lastHistogram = generateHistogram(lastFrame, nBins);

		// Initialize output list
		List<Shot> outputList = new ArrayList<Shot>();

		// Iterate through the frames
		for (final MBFImage currentFrame : video) {
			final FImage current = currentFrame.flatten();

			float meanVal = meanSquareDifference(current, last);

			MultidimensionalHistogram currentHistogram = generateHistogram(currentFrame, nBins);
			double distanceScore = currentHistogram.compare(lastHistogram, DoubleFVComparison.EUCLIDEAN);

			if (currentFrameNo % 20 == 0) {
				//Call Logo detector here 
				byte[] currentBytes = current.toByteImage();
				
			}
			
			// Might need adjust threshold:
			if (meanVal > threshold && distanceScore > threshold) {

				if (!outputList.isEmpty()) {

					Shot lastShot = outputList.get(outputList.size() - 1);
					int previousShotEndFrame = lastShot.getEndFrameNumber(); //// !!!!

					if (verbose) {
						System.out.println("Frames in between: " + (lastFrameNo - previousShotEndFrame));
					}

					int framesInBetween = lastFrameNo - previousShotEndFrame;
					int startFrameNo = previousShotEndFrame + 1;
					Shot shot = new Shot(startFrameNo, lastFrameNo, framesInBetween);
					shot.setHistogramScore(distanceScore);
					shot.setMSD(meanVal);

					if (framesInBetween > minFrames) {
						if (verbose) {
							System.out.println("New Shot detected");
							DisplayUtilities.displayName(current, "debug: Current Frame");
							DisplayUtilities.displayName(last, "debug: Last Frame");
						}
						outputList.add(shot);
						if (verbose) {
							System.out.println("EndFrameNo: " + shot.getEndFrameNumber() + ", framesInBetween: "
									+ shot.getShotLength() + ", MSD: " + meanVal + ", Hist Score: "
									+ distanceScore);
						}
					}
					
					//Initial characterization 
					
					if (framesInBetween < 150) {
						shot.category = Category.ADVRT;
					} else if (framesInBetween > 310) {
						shot.category = Category.NO_ADVRT;
					}
					if (verbose) {
						System.out.println("Shot category: " + shot.category);
					}
				}

				else {
					// First entry
					outputList.add(new Shot(0, lastFrameNo, (int) currentFrameNo));
				}

			}

//			if (verbose) {
//				System.out.println("From frame " + lastFrameNo + ", to " + currentFrameNo + ", meanVal = " + meanVal
//						+ ", histogramScore = " + distanceScore);
//			}

			// Set the current frame to the last frame
			last = current;
			lastFrame = currentFrame;
			lastHistogram = currentHistogram;
			lastFrameNo++;
			currentFrameNo++;

		}

		return outputList;

	}

	/**
	 * Analyzes the audio stream that is matched with the video stream to get
	 * average amplitudes of shots for better distinguishing.
	 */
	public static List<Shot> analyzeAudio(String audiopath, List<Shot> shotList)
			throws UnsupportedAudioFileException, IOException {
		File audio = new File(audiopath);
		// Shot[] shots = new Shot[shotList.size()];
		// shots = shotList.toArray(shots);
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
		for (int curFrame = 0; (read = stream.read(buffer)) > 0; curFrame++) {

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
			if (videoFrame > shotList.get(shotOffset).getEndFrameNumber()) {
				shotList.get(shotOffset).setSignChangeFreq(signCount * 1.0 / shotList.get(shotOffset).getShotLength());
				signCount = 0;
				sign = 1;
				++shotOffset;
			}

			shotList.get(shotOffset).addSample(Math.abs(xy / Short.MAX_VALUE));
			if (sign * xy < 0) {
				sign = (xy < 0 ? -1 : 1);
				++signCount;
			}
		}

		shotList.get(shotOffset).setSignChangeFreq(signCount * 1.0 / shotList.get(shotOffset).getShotLength());

		for (Shot s : shotList) {
			s.avgSample();
		}

		return shotList;
	}

	 public static void processShotLabelingFromAudioMetrics(List<Shot> shots){
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
	                if (Math.abs(shots.get(index).getAvgAmp() - shots.get(index - 1).getAvgAmp()) <= 0.01) {
	                    shots.get(index).category = shots.get(index - 1).category;
	                }
	            }

	            if (index != shots.size() - 1 && shots.get(index).category == Category.EITHER) {
	                if (Math.abs(shots.get(index).getAvgAmp() - shots.get(index + 1).getAvgAmp()) <= 0.01) {
	                    shots.get(index).category = shots.get(index + 1).category;
	                }
	            }
	        }

	        // Pruning Stag 3: Compare audio shot signChangeFreq .
	        for (int index = 1; index < shots.size() - 1; ++index) {
	            if (shots.get(index).category == Category.EITHER) {
	                if (Math.abs(shots.get(index).getSignChangeFreq() - shots.get(index - 1).getStartFrameNumber()) < Math
	                        .abs(shots.get(index).getSignChangeFreq() - shots.get(index + 1).getSignChangeFreq())) {
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
}
	
//	public static void audioAnalyzer(List<Shot> shotList, XuggleAudio audio, float framesPerSample) {
//		
//		//AudioPlayer.createAudioPlayer(audio).run();
//		
//		final AudioWaveform vis = new AudioWaveform( 400, 1600 );
//		vis.showWindow( "Waveform" );
//		
//		SampleChunk sampleChunk = null;
//		
//		//30 frames/sec
//		//44100 samples/sec
//		//44100/30 = 1470 samples/frame
//		//for(final Shot shot : shotList) {
//		Shot shot = shotList.get(0);
//			
//			//float endFrameNumber = shot.getEndFrameNumber();
//			//float startFrameNumber = endFrameNumber - shot.getShotLength(); 
//			
//			//float startTime = (float) (startFrameNumber / 30.0);
//			//float endTime = (float) (endFrameNumber / 30.0);
//			//long timestamp = (long) startTime*1000;
//			//audio.seek(timestamp);
//			
//			
//			//System.out.println("endFrameNumber = " + endFrameNumber + ", startFrameNumber = " + startFrameNumber);
//			//System.out.println("startTime = " + startTime + ", endTime = " + endTime + ", timestamp = " + timestamp);
//					
//			float shotSamples = 0;
//			SampleChunk shotChunk = null;
//			while (shotSamples <= (shot.getShotLength()*framesPerSample)){
//				sampleChunk = audio.nextSampleChunk();
//				
//				if (shotChunk == null) {
//					shotChunk  = sampleChunk;
//				}
//				
//				else {
//					shotChunk.append(sampleChunk);
//				}
//				
//				vis.setData(shotChunk.getSampleBuffer());
//				
//				shotSamples = shotSamples + sampleChunk.getNumberOfSamples();
//				System.out.println("shotSamples = " + shotSamples);

	// }

//			System.out.println("length of SampleChunk = " + shotChunk.getNumberOfSamples() + ", samples in shot = " + (shot.getShotLength()*framesPerSample));

//			//information about the rate of change in the different spectrum bands.
//			PowerCepstrumTransform pct = new PowerCepstrumTransform();
//			pct.
//			
//			int windowSizeMillis = (int) ((endTime - startTime) * 1000);
//			int overlapMillis = 0;
//			EffectiveSoundPressure esp = new EffectiveSoundPressure(audio, windowSizeMillis, overlapMillis);
//			double soundPressure = esp.getEffectiveSoundPressure();
//			
//			int count = windowSizeMillis * 44100;
//			SampleChunk sc = null;
//			float[] old_fftData;
//			int err = 0;
//			while(count > 0 && ((sc = audio.nextSampleChunk()) != null)) {
//				FourierTransform fft = new FourierTransform();	
//				fft.process(sc);
//				float[][] fftData = fft.getMagnitudes();
//				for(int j=0; j< fftData[0].length; j++) {
//					//err += Math.abs(fftData[0][j] - old_fftData[0][j]);
//					//old_fftData[0][j] = fftData[0][j];
//				}
//			}

//		}
//		
//	}

