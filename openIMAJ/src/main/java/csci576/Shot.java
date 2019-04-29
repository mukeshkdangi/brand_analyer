package csci576;

public class Shot
{
	
	public Shot(int endFrameNumber, float shotLength)
	{
		this.endFrameNumber = endFrameNumber;
	}
	
	public Shot(int startFrameNumber, int endFrameNumber, int shotLength)
	{
		this.startFrameNumber = startFrameNumber;
		this.endFrameNumber = endFrameNumber;
		this.shotLength = shotLength;
        category = Category.EITHER;
        
		//this.msd = msd;
		//this.histogramScore = histogramScore;
	}
	
	//Type of shot
    public Category category;
	
	//Select of current shot
	private int startFrameNumber;
	
	//End of current shot
	private int endFrameNumber;
	
	//How many frames the shot spanned
	private int shotLength;
	
	//Mean Square Difference between this shot and a previous shot
	//Might need to be updated if the video is edited and the previous frame is removed
	private float msd; 
	
	//Difference between the start frame and the previous shot end frame
	private double histogramScore;

	//Store the histogram the relates to the start frame
	//MultidimensionalHistogram histogram;
	
	private double avgAmp;
	
	private int sampleCount;
	
	private double signChangeFreq;
	
	public enum Category {
        NO_ADVRT, ADVRT, EITHER
    }

	public void setStartFrameNumber(int newStartFrameNumber) {
		this.startFrameNumber = newStartFrameNumber;	
	}
	
	public int getStartFrameNumber() {
		return this.startFrameNumber;
	}
	
	public void setEndFrameNumber(int newFrameNumber) {
		this.endFrameNumber = newFrameNumber;
	}
	
	public int getEndFrameNumber() {
		return this.endFrameNumber;
	}
	
	public void setShotLength(int newShotLength) {
		this.shotLength = newShotLength;
	}
	
	public int getShotLength() {
		return this.shotLength;
	}
	
	public void setMSD(float newMSD) {
		this.msd = newMSD;
	}
	
	public float getMSD() {
		return this.msd;
	}
	
	public void setHistogramScore(double newHistogramScore) {
		this.histogramScore = newHistogramScore;
	}
	
	public double gethistogramScore() {
		return this.histogramScore;
	}
	
	public void setAvgAmp(float newAvgAmp) {
		this.avgAmp = newAvgAmp;
	}
	
	public double getAvgAmp() {
		return this.avgAmp;
	}

	public int getSampleCount() {
		return this.sampleCount;
	}
	
	public void setSignChangeFreq(double newSignChangeFreq) {
		this.avgAmp = signChangeFreq;
	}
	
	public double getSignChangeFreq() {
		return this.signChangeFreq;
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

