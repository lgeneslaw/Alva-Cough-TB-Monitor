
************************************************************************************************
                                Audio Sanitizer
                                Author - Karthic Aragam
*************************************************************************************************
                                1. Introduction
*************************************************************************************************

The Audio Sanitizer was created in order to both save resources and improve  Professor 
Brian Tracey's cough detection algorithm. In order to test this algorithm there is a need for 
clean cough data in the form of wav files. In this data collection process, it is very easy to 
get subpar data due to faulty recording techniques. In order to avoid wasting the time and 
resources necessary to collect this data, it is necessary to know immediately if the cough 
data collected is adequate for testing. 

The Sanitizer tests the cough data for three primary cases of subpar data: Flatlining values, 
extreme values, and extreme slew rate. The Sanitizer  extricates these errors from the wav files.

*************************************************************************************************
                                2. How It Works
*************************************************************************************************

The first step the Sanitizer takes is taking in a wav file and producing an array containing 
the voltages of every frame. This is done using the python "WAV" module which allows us to get values
from the wav file such as its frame rate, number of frames, number of channels, and other useful information.

After this information is obtained, the Sanitizer loops through the voltages minute by minute.
If a particular minute is seen to exceed any of the threshold values set for the three primary cases
it is added to a "cut_list", which contains all the minutes of data that contain errors. 

The final step is to extricate the faulty data by writing all of the data from the original file
into the newly created sanitized file but excluding all the minutes  listed in the "cut_list". All 
other parameters (frame rate, number of frames, number of channels, etc.) from the original wav file
are written to the newly output wav file.

	
