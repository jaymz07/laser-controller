/* Audio output thread. Recieves points from GraphicalLaserOutput.java, creates a path from them, and outputs the path parametrically to the L/R soundcard channels.
   The path taken by the laser is to sweep through the points in order and then in reverse to avoid connecting the beggining and end points of the path.
   */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.util.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class LaserOutputThread extends Thread {

    /*Main control of thread is done through these volatile variables.
     *Points is set externally to be an arraylist of normalized Point classes.
     * Normalized, in the sense that neither abs(x) nor abs(y) exceed 1.0.
     * ie the points can fit in a 1X1 box....
     */
    public volatile ArrayList<Point> points = new ArrayList<Point>();
    public volatile 	boolean terminateStream = false;
    public volatile int outNumber = 0;
    public volatile boolean lightning = false;
    public volatile boolean filter = false;
    public volatile double lowPassX = 1.0;
    public volatile double highPassX = 1.0;
    public volatile double filterFreqX =200.0;
    public volatile double lowPassY = 1.0;
    public volatile double highPassY = 1.0;
    public volatile double filterFreqY =200.0;
    public volatile boolean recalc = true;
    public volatile boolean invertSignal = false;

    /*------Audio samples per draw point-------*/
    //public int samplesPerPoint = 1;
    
    public double maxDist = 0.1;

    /*--------Audio Format Options for output----------*/
    public int		nSampleSizeInBits = 16;
    public String	strMixerName = null;
    public int		nInternalBufferSize = AudioSystem.NOT_SPECIFIED;
    public int 		frameSize = 4;
    public float	fSampleRate = 44100.0F;
    public boolean 	bBigEndian = false;
    public int		writeBufferSizeBytes = 512;

    //constant for data type conversion. The (0.999) ceiling factor avoids clipping the output.
    public float maxByte =  (float) (0.999*Math.pow(2, nSampleSizeInBits - 1));

    private AudioFormat audioFormat;
    private SourceDataLine line;

    private static boolean DEBUG = false;
    private int currentOffset=0;
    
    private byte[] streamData= new byte[4];

    public LaserOutputThread(ArrayList<Point> pts) {
        points = pts;
        audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fSampleRate, nSampleSizeInBits, 2, frameSize, fSampleRate, bBigEndian);
        line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);
    }

    public void run() {

        if (line == null) {
            System.out.println("AudioPlayer: cannot get SourceDataLine for format " + audioFormat);
            System.exit(1);
        }

        //Tell line to pass data to output device (soundcard).
        line.start();

        //This thread executes as infinite loop. Control is done externally by setting the "points" variable.
        while(!terminateStream) {
	    if(recalc)
		  recomputeStream();
		  recalc=false;
            outputToLaser();
        }
        line.drain();
        line.close();

    }
    public void outputToLaser() { //throws exception

	if(streamData.length>8) {
		int currentOffset = 0;
		int currentNum = outNumber;
		terminateStream = false;
		while(currentOffset < (streamData.length - writeBufferSizeBytes)) {
		    if(currentNum != outNumber)
		      break;
		    //Write to audio in chunks.
		    currentOffset += line.write(streamData, currentOffset, writeBufferSizeBytes);
		    try {
			int sleepTime = Math.round(1000.0f/fSampleRate*writeBufferSizeBytes/frameSize);
			if(!lightning)
			  sleepTime -= 10;
			else
			  sleepTime += 5;
			if(sleepTime >0)
			  Thread.sleep(sleepTime);
		    }
		    catch(InterruptedException e) {
		    }
		    // line.drain();
		}
		if(currentNum == outNumber)
		  currentOffset += line.write(streamData, currentOffset, streamData.length - currentOffset);
		
		//for(byte b : streamData) System.out.println(b);
		//System.out.println("\n\n\n"+currentOffset+"\n"+streamData.length);
	}
    }
    public void recomputeStream() {
	//Since "points" is modified outside of this thread, create local copy to avoid runtime errors.
        ArrayList<Point> localPoints = new ArrayList<Point>(points);
        int numPoints = localPoints.size();
	 if(numPoints > 2) {
		/*generate stream data to send to soundcard. Must use byte array format */

		ArrayList<Point> interpPath = interpolatePath(localPoints);
		ArrayList<Point> xSignal = new ArrayList<Point>();
		ArrayList<Point> ySignal = new ArrayList<Point>();
		
		if(filter) {
		      for(int i =0; i<interpPath.size(); i++) {
			  xSignal.add(new Point(((double)i)/fSampleRate,interpPath.get(i).x));
			  ySignal.add(new Point(((double)i)/fSampleRate,interpPath.get(i).y));
		      }
		      xSignal = (new DataSet(xSignal)).crossoverFilter2(filterFreqX,lowPassX,highPassX).data;
		      ySignal = (new DataSet(ySignal)).crossoverFilter2(filterFreqY,lowPassY,highPassY).data;
		      for(int i =0; i<interpPath.size(); i++)
			  interpPath.set(i,new Point(xSignal.get(i).y,ySignal.get(i).y));
		}
		numPoints = interpPath.size();
		//backwards path:
		for(int i =numPoints -1;i>0; i--)
		  interpPath.add(interpPath.get(i));
		
		//convert to integers
		ArrayList<Integer> integerPath = new ArrayList<Integer>();

		int invFactor = 1;
		if(invertSignal)
		  invFactor=-1;
		for(Point p: interpPath) {
		  integerPath.add(Math.round((float)(invFactor*p.y*maxByte)));
		  integerPath.add(Math.round((float)(invFactor*p.x*maxByte)));
		}

		int bytesPerInteger = 2;
		streamData = new byte[integerPath.size() * bytesPerInteger - 4];
		for(int i = 0; i < integerPath.size() - 2; i+=2)
		{
		    //Manual conversion from integer to byte:
		    int nValueX = integerPath.get(i), nValueY = integerPath.get(i+1);
		    //left channel:
		    streamData[i*2 + 0] = (byte) (nValueX & 0xFF);
		    streamData[i*2 + 1] = (byte) ((nValueX >>> 8) & 0xFF);
		    //right channel:
		    streamData[i*2 + 2] = (byte) (nValueY & 0xFF);
		    streamData[i*2 + 3] = (byte) ((nValueY >>> 8) & 0xFF);
		}
	}
    }
    public ArrayList<Point> interpolatePath(ArrayList<Point> pointsIn) {
      ArrayList<Point> out = new ArrayList<Point>();
      for(int i =0 ; i<pointsIn.size() - 1; i++) {
	int numPointsBetween = (int)(pointsIn.get(i).getDist	(pointsIn.get(i+1))/maxDist);
	Point pt = pointsIn.get(i), ptNext = pointsIn.get(i+1);
	out.add(pt);
	for(int j=0;j<numPointsBetween;j++) {
	  out.add(new Point(pt.x + (ptNext.x - pt.x)*((double)j)/numPointsBetween,pt.y + (ptNext.y - pt.y)*((double)j)/numPointsBetween));
	}
      }
      out.add(pointsIn.get(pointsIn.size()-1));
      return out;
    }
   
    
    //----------Function taken nearly verbatim from examples in JavaDocs-------------------
    private static SourceDataLine getSourceDataLine(String strMixerName, AudioFormat audioFormat, int nBufferSize)
    {
        /*	Asking for a line is a rather tricky thing.
         *	We have to construct an Info object that specifies
         *	the desired properties for the line.
         *	First, we have to say which kind of line we want. The
         *	possibilities are: SourceDataLine (for playback), Clip
         *	(for repeated playback)	and TargetDataLine (for
         *	 recording).
         *	Here, we want to do normal playback, so we ask for
         *	a SourceDataLine.
         *	Then, we have to pass an AudioFormat object, so that
         *	the Line knows which format the data passed to it
         *	will have.
         *	Furthermore, we can give Java Sound a hint about how
         *	big the internal buffer for the line should be. This
         *	isn't used here, signaling that we
         *	don't care about the exact size. Java Sound will use
         *	some default value for the buffer size.
         */
        SourceDataLine	line = null;
        DataLine.Info	info = new DataLine.Info(SourceDataLine.class,
                audioFormat, nBufferSize);
        try {
            if (strMixerName != null)
            {
                Mixer.Info	mixerInfo = AudioCommon.getMixerInfo(strMixerName);
                if (mixerInfo == null) {
                    System.out.println("AudioPlayer: mixer not found: " + strMixerName);
                    System.exit(1);
                }
                Mixer	mixer = AudioSystem.getMixer(mixerInfo);
                line = (SourceDataLine) mixer.getLine(info);
            }
            else {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }

            /*
             *	The line is there, but it is not yet ready to
             *	receive audio data. We have to open the line.
             */
            line.open(audioFormat, nBufferSize);
        }
        catch (LineUnavailableException e) {
            System.out.println("Can't get an audio line!");
            if (DEBUG) e.printStackTrace();
        }
        catch (Exception e) {
            System.out.println("General error while opening audio line.");
            if (DEBUG) e.printStackTrace();
        }
        return line;
    }
}





