package csci576;

public class Scene
{
	
	public Scene(float endFrameNumber, float shotLength)
	{
		this.endFrameNumber = endFrameNumber;
	}
	
	public Scene(float endFrameNumber, float shotLength, float msd, double histogramScore)
	{
		this.endFrameNumber = endFrameNumber;
		this.sceneLength = shotLength;
		this.msd = msd;
		this.histogramScore = histogramScore;
	}
	
	//End of current shot
	private float endFrameNumber;
	
	//How many frames the shot spanned
	private float sceneLength;
	
	//Mean Square Difference between this shot and a previous shot
	//Might need to be updated if the video is edited and the previous frame is removed
	private float msd; 
	
	//Difference between the start frame and the previous shot end frame
	private double histogramScore;

	//Store the histogram the relates to the start frame
	//MultidimensionalHistogram histogram;
	
	
	public void setEndFrameNumber(float newFrameNumber) {
		this.endFrameNumber = newFrameNumber;
	}
	
	public float getEndFrameNumber() {
		return this.endFrameNumber;
	}
	
	public void setSceneLength(float newShotLength) {
		this.sceneLength = newShotLength;
	}
	
	public float getSceneLength() {
		return this.sceneLength;
	}
	
	public void setMSD(float newMSD) {
		this.msd = newMSD;
	}
	
	public float getMSD() {
		return this.msd;
	}
	
	public void setHistogramScore(float newHistogramScore) {
		this.msd = newHistogramScore;
	}
	
	public double gethistogramScore() {
		return this.histogramScore;
	}
	
}

