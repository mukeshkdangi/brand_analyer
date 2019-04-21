package csci576;

public class Shot
{
	
	public Shot(float startFrameNumber, float shotLength, float msd, float histogramScore)
	{
		this.startFrameNumber = startFrameNumber;
		this.shotLength = shotLength;
		this.msd = msd;
		this.histogramScore = histogramScore;
	}

	//Start of current shot
	float startFrameNumber;
	
	//How many frames the shot goes
	float shotLength;
	
	//Mean Square Difference between this shot and a previous shot
	//Might need to be updated if the video is edited and the previous frame is removed
	float msd; 
	
	//Difference between the start frame and the previous shot end frame
	float histogramScore;

	//Store the histogram the relates to the start frame
	//MultidimensionalHistogram histogram;
	
	
	
	
}

