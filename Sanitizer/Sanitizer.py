import numpy
import wave
import struct



def everyOther (v, offset=0):
   return [v[i] for i in range(offset, len(v), 2)]

def wavLoad (fname):

#These 3 thresholds can be adjusted
   flatline_thresh = 200
   extreme_thresh = 20000 
   slew_thresh = 20000
   
   wav = wave.open (fname, "r")
   (nchannels, sampwidth, framerate, nframes, comptype, compname) = wav.getparams ()
   frames = wav.readframes (nframes * nchannels)
   out = struct.unpack_from ("%dh" % nframes * nchannels, frames)

   # Convert 2 channels to numpy arrays
   if nchannels == 2:
      left = numpy.array (list (everyOther (out, 0)))
      right = numpy.array (list  (everyOther (out, 1)))
      

   else:
      left = numpy.array (out)
      right = left #These arrays contain voltages for wav file

   count = 0
   cut_list = list()#Holds list of minutes that contain bad data
   sub_list = list()#Holds voltage for one min at time
   for x in numpy.nditer(left):
      count+=1
      min_count = count/(framerate*60)
      sub_list.append(x)
      if count % (framerate*60) == 0:  
          sub_list.sort()
          big = abs(sub_list[len(sub_list)-1])
          small = abs(sub_list[0])
          if big > small:
            val = big
          else:
            val = small

          if val < flatline_thresh:
            if min_count not in cut_list:
               cut_list.append(min_count)

         
         if val > extreme_thresh:
            if min_count not in cut_list:
               cut_list.append(min_count)


         slew_array = numpy.array(sub_list)
         slew_array = numpy.diff(slew_array)#create array of slew rates
         max_slew1 = numpy.argmax(slew_array)
         max_slew2 = numpy.abs(numpy.argmin(slew_array))
         if max_slew1 > max_slew2:
            max_slew = max_slew1
         else:
            max_slew = max_slew2
         if max_slew > slew_thresh:
            if min_count not in cut_list:
               cut_list.append(min_count)

         sub_list = []


   #Write all original data to new file, exclude faulty data
   cut_list.sort()
   new_wav_data = wav.readframes(0)
   for x in range(0,len(cut_list)):
       if x == 0:
         if cut_list[x] == 1:
            wav.setpos(framerate*60)
         else:
            new_wav_data += wav.readframes((cut_list[x]-1)*framerate*60)
            wav.setpos(cut_list[x]*framerate*60)
       else:
          wav.setpos((cut_list[x-1]+1)*framerate*60)
          if x < len(cut_list):
            new_wav_data += wav.readframes((cut_list[x]-cut_list[x-1]-1)*framerate*60)
          else:
            new_wav_data += wav.readframes(nframes - (cut_list[x]+1)*framerate*60)

       
   #Create New Wav file, write appropriate data to new file.
   new_wav_audio = wave.open('output.wav','w')
   new_wav_audio.setnchannels(nchannels)
   new_wav_audio.setsampwidth(sampwidth)
   new_wav_audio.setframerate(framerate)
   new_wav_audio.writeframes(new_wav_data)
   new_wav_audio.close()




wavLoad("test.wav")
