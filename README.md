# CSCI576 Final Project
					Credits : Our hard work :)	

## Problem Statement
> Design an  develope a System to automatically remove advertisements from the video (and corresponding audio) which is interspersed with advertisements. Furthermore, design a process based on specific brand images to detect the brand in the video and if present, replace the original advertisement with a corresponding topical advertisement.

a. Remove ads from Video
b. Replace ads with relevant ads which are determined by the logos detections from the video

## API and Algorithms:
1. Google Vision [https://cloud.google.com/vision/]
2. OpemImaj [http://openimaj.org/]
3. Javax Sound 
4. XuggleVideo
Some other MultiMedia concepts like motion compensation, entropy change

## Testing Audio Video Sycn code 

```C++
javac *java 
```
> Run Audio Video Sync
```C++
java Player  ~/Downloads/Subway_Ad_15s.rgb ~/Downloads/Subway_Ad_15s.wav
```

> Run logo Detection and Ad replacement Algo
``` C++
java AudioVideoProcessor /Users/mukesh/Downloads/dataset/Videos/data_test3.rgb /Users/mukesh/Downloads/dataset/Videos/data_test3.wav  /Users/mukesh/Downloads/dataset/Videos/video3.rgb /Users/mukesh/Downloads/dataset/Videos/audio3.wav
```

## Motivation 
> There is an increased amount of video and audio content broadcast and streamed everywhere today. Such content needs to be frequently analyzed for a variety of reasons and applications – such as searching, indexing, summarizing, etc. One general area is in modifying content is to remove/replace specific parts of frames or even a number of frames altogether


## Keywords
• Frame: a single still image from a video, eg NTSC - 30 frames/second, film – 24frames/second
• Shot: sequence of frames recorded in a single camera operation
• Sequence or Scenes: collection of shots forming a semantic unit which conceptually may be shot at a single time and place

## Process

Here is a list to give you an idea of concrete tasks that your project needs to achieve:
1. Read in the input video/audio – remember you might not be able to fit the entire content in memory for processing.
2. Break the input video into a list of logical segments – shots (see anatomy of a video below) How can you achieve this?
3. Give each shot a variety of quantitative weights such as – length of shot, motion characteristics in the shot, audio levels, color statistics etc.
4. Using the above characteristics, decide whether a shot or a group of adjacent shots might be an advertisement
5. Remove the shots that correspond to the advertisement. Write out the new video/audio file.
6. If brands are detected, replace the old advertisement with a new advertisement to
write out the new video/audio file.

## Note
You are welcome to use any language you feel familiar for this project.

About the RefrenceCode folder:
	This reference code only provides you some reference about how to play audio file. You need to program your own part to make the audio part and video part synchronized. As long as you can find ways to read *.rgb format and *.wav format, and then synchronize the video and audio parts, it will be okay. You can use libraries to read them, especially for the *.wav file. 
	
	Actually the purpose of audio data is making index for the comparison. Therefore, as long as you can make a index from audio data & do the comparison. You don't need to worry about data type you are using.

About the dataset folder: (you can download from Google Driver as shown in dataset.txt)
	For Video Part:
	  Video Frame Size: 480*270
	  Video Data format: RGB format similar with assignments
	  Video FPS: 30 frames/second

	For Audio Part:
	  Auido Sampling Rate: 48000 HZ
	  Audio Channels : 1 mono
	  Bits per sample: 16
	  
  "Videos" folder: 
    include the video files you need to process. There are two ads originally inserted here. Logs are also included in the video file.
	data_test1: a 5 minutes video you need to process your tasks
	   5 minutes video part: data_test1.rgb
	   5 minutes Audio part: data_test1.wav
	   5 minutes Audio/Video AVI: data_test1_cmp.avi (this is the compressed video only for your reference to review the video. You should use .rgb and .wav for your processing)
	   
  "Ads" folder: 
    include ads you need to insert into the processing videos.
	Subway_Ad_15s: a 15 seconds subway ad
	   15 seconds video part: Subway_Ad_15s.rgb
	   15 seconds Audio part: Subway_Ad_15s.wav
	   15 seconds Audio/Video AVI: Subway_Ad_15s_cmp.avi (this is the compressed video only for your reference to review the video. You should use .rgb and .wav for your processing)

	Starbucks_Ad_15s:  a 15 seconds starbucks ad
	   15 seconds video part: Starbucks_Ad_15s.rgb
	   15 seconds Audio part: Starbucks_Ad_15s.wav
	   15 seconds Audio/Video AVI: Starbucks_Ad_15s_cmp.avi (this is the compressed video only for your reference to review the video. You should use .rgb and .wav for your processing)
	   
  "Brand Images" folder: 
    Include brand/logo images in rgb format with 480*270 as resolution. 
	The BMP files are only for your reference to review the images and you should use the .rgb files.
	
	
## Compile and Run Audio Video Sync Code :
java Player ~/Downloads/Starbucks_Ad_15s.rgb ~/Downloads/Starbucks_Ad_15s.wav
