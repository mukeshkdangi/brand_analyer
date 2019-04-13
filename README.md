# FinalProject
CSCI576 Final Project

## Motivation 
> There is an increased amount of video and audio content broadcast and streamed everywhere today. Such content needs to be frequently analyzed for a variety of reasons and applications – such as searching, indexing, summarizing, etc. One general area is in modifying content is to remove/replace specific parts of frames or even a number of frames altogether

## Problem Statement
> Design an algorithm to automatically remove advertisements from the video (and corresponding audio) which is interspersed with advertisements. Furthermore, design a process based on specific brand images to detect the brand in the video and if present, replace the original advertisement with a corresponding topical advertisement.

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
