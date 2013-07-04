package client;
import common.*;

/*
	ok, this might be helpful to know (yes, you can do half sharps/flats!)
	
	 0: C
	 2: C#
	 4: D
	 6: D#
	 8: E
	10: F
	12: F#
	14: G
	16: G#
	18: A
	20: A#
	22: B
	24: C, but higher
	
	C-4 (96) is middle C.
	A-4 (114) is concert pitch A (440Hz).
	240+ is for drums.
	
	240: kick
	241: snare
	242: closed hihat
	243: open hihat
	244: hi tom (upper-left)
	245: mid tom (upper-right)
	246: low/floor tom (lower-very-right)
	247: crash
	
	-GM
*/

import java.io.*;
import javax.sound.sampled.*;

public class Sound implements Runnable
{
	// trust me, musical stuff doesn't work well with lots of Clips
	// i tried porting SPEED to Java and had to resort to a single loop
	//   for the music
	//         -GM
	//Clip[] squares;
	
	// consts
	public static final int TONE_MAX = 64; // 64 channels should be enough for anybody
	public static final int BUF_SIZE = 65536; // this is so that OpenJDK does not crash, or is it Sun's Java?
	public static final int BUF_LAG = 4096; // how much we aim to lag by
	public static final double FREQ_BASE = 22050.0; // 22kHz should be fine, gets about 10% CPU on this -GM
	public static final double SPEED_OF_SOUND = 33000.0; // "prepare your colons for the awesome"
	
	// precalc consts
	public static final double FREQ_IBASE = 1.0/FREQ_BASE;
	public static final double ISPEED_OF_SOUND = 1.0/SPEED_OF_SOUND;
	
	// precalc const tables
	public static final double[] noteTab; // frequencies
	static {
		noteTab = new double[240];
		// 12th root of 2 normally.
		// in this case, 24th root of 2 (we're doing half-sharps/flats).
		for(int i = 0; i < 240; i++)
			noteTab[i] = 440.0*Math.pow(2.0,(double)(i-114)/24.0);
	}
	
	// samples
	public static final CSSampleData smpPlateOn = new CSSampleData("plateo.wav");
	public static final CSSampleData smpPlateOff = new CSSampleData("platec.wav");
	public static final CSSampleData smpDoorOpen = new CSSampleData("dooro.wav");
	public static final CSSampleData smpDoorClose = new CSSampleData("doorc.wav");
	
	// fields
	private boolean stillAlive = true;
	private Thread thd = null;
	private SourceDataLine dsp;
	private CSTone[] tones = new CSTone[TONE_MAX];
	private byte[] buf;
	private int posX = 0, posY = 0;
	private double clipOffs = 0.0;
	
	
	// inner classes for fun and profit
	private abstract static class CSTone
	{
		protected double curPos; // in seconds
		protected double freq;
		protected int x, y;
		
		public CSTone(int x, int y, double freq)
		{
			this.x = x;
			this.y = y;
			this.freq = freq;
		}
		
		protected abstract double getTone(double pos);
		public abstract boolean isAlive(double pos);
		
		public double getPos()
		{
			return curPos;
		}
		
		public int getX()
		{
			return x;
		}
		
		public int getY()
		{
			return y;
		}
		
		public double update(double baseperiod, double offs)
		{
			double ret = getTone(curPos-offs);
			curPos += baseperiod;
			return ret;
		}
		
		public double updateNoBump(double baseperiod, double offs)
		{
			return getTone(curPos-offs);
		}
	}
	
	private class CSMelodium extends CSTone
	{
		private double vol;
		
		public CSMelodium(int x, int y, double freq, double vol)
		{
			super(x, y, freq);
			this.vol = vol;
		}
		
		protected double getTone(double pos)
		{
			return
				pos < 1.0
				? ((pos*freq) % 1.0 < 0.5
					? 0.9
					: -0.9
				)*(0.05/(0.06+pos))*(1.0-pos)*vol
				: 0.0;
		}
		
		public boolean isAlive(double pos)
		{
			return pos < 1.0;
		}
	}
	
	private class CSSample extends CSTone
	{
		private final byte[] data;
		private final double vol;
		public CSSample(int x, int y, CSSampleData sd, double vol)
		{
			super(x, y, sd.freq);
			if(sd.data == null)
				data = new byte[0];
			else
				data = sd.data;
			this.vol = vol;
		}
		
		protected double getTone(double pos)
		{
			return
				pos*freq < data.length && pos >= 0.0
				? (double)data[(int)(pos*freq)]*vol/127.0
				: 0.0;
		}
		
		public boolean isAlive(double pos)
		{
			return pos*freq < data.length;
		}
	}
	
	private class CSKickDrum extends CSTone
	{
		private double vol;
		
		public CSKickDrum(int x, int y, double vol)
		{
			super(x, y, 150.0);
			this.vol = vol;
		}
		
		protected double getTone(double pos)
		{
			return
				pos < 0.25
				? ((pos*freq*0.25/(0.25+pos)) % 1.0 < 0.5
					? 0.9
					: -0.9
				)*(0.05/(0.06+pos))*4.0*(0.25-pos)*vol
				: 0.0;
		}
		
		public boolean isAlive(double pos)
		{
			return pos < 0.25;
		}
	}
	
	private class CSTomDrum extends CSTone
	{
		private double vol;
		
		public CSTomDrum(int x, int y, double freq, double vol)
		{
			super(x, y, freq);
			this.vol = vol;
		}
		
		protected double getTone(double pos)
		{
			return
				pos < 0.25
				? ((pos*freq*0.63/(0.63+pos)) % 1.0 < 0.5
					? 0.9
					: -0.9
				)*(0.05/(0.06+pos))*4.0*(0.25-pos)*vol
				: 0.0;
		}
		
		public boolean isAlive(double pos)
		{
			return pos < 0.25;
		}
	}
	
	private static class CSNoise extends CSTone
	{
		private double vol;
		private double len, ilen;
		
		private static final boolean[] waveForm;
		static {
			waveForm = new boolean[57337];
			int p = 0x0001;
			for(int i = 0; i < 57337; i++)
			{
				waveForm[i] = (p&1) != 0;
				
				p = (
					(p&1) == 0
					? p >> 1
					: ((p>>1)^0x9000)
				);
			}
		}
		
		public CSNoise(int x, int y, double freq, double len, double vol)
		{
			super(x, y, freq);
			this.len = len;
			this.ilen = 1.0/len;
			this.vol = vol;
		}
		
		protected double getTone(double pos)
		{
			return
				pos < len && pos >= 0.0
				? (waveForm[(int)(pos*freq) % 57337]
					? 0.9
					: -0.9
				)*(0.05/(0.06+pos))*ilen*(len-pos)*vol
				: 0.0;
		}
		
		public boolean isAlive(double pos)
		{
			return pos < len;
		}
	}
	
	private static class CSSampleData
	{
		public final double freq;
		public final byte[] data;
		
		public CSSampleData(String fname)
		{
			byte[] retdata = null;
			double retfreq = 44100.0;
			try
			{
				ByteArrayOutputStream bfp = new ByteArrayOutputStream();
				InputStream fp = Sound.class.getResourceAsStream(fname);
				
				// SKIP HEADER, ASSUME 44kHz 16-bit little-endian signed mono!!! -GM
				byte[] hdr = new byte[44];
				fp.read(hdr);
				while(true)
				{
					int v = fp.read();
					if(v == -1)
						break;
					v = fp.read();
					if(v == -1)
						break;
					bfp.write((byte)v);
				}
				fp.close();
				retdata = bfp.toByteArray();
			} catch(IOException ex) {
				System.err.printf("IOException loading file \"%s\"\n",fname);
				retfreq = 1.0;
				retdata = null;
			} catch(Exception ex) {
				System.err.printf("Exception loading file \"%s\"\n",fname);
				retfreq = 1.0;
				retdata = null;
			}
			
			freq = retfreq;
			data = retdata;
		}
	}
	
	// constructors
	public Sound()
	{
		thd = new Thread(this);
		thd.start();
	}
	
	// methods
	public void kill()
	{
		stillAlive = false;
		thd.interrupt();
	}
	
	public void getStuff(int len)
	{
		int lag = dsp.getBufferSize() - dsp.available();
		len = BUF_LAG + len - lag;
		if(len <= 0)
			return;
		len *= 2;
		
		// precalc SOME stuff
		double[] voll = new double[TONE_MAX];
		double[] volr = new double[TONE_MAX];
		double[] offsl = new double[TONE_MAX];
		double[] offsr = new double[TONE_MAX];
		for(int i = 0; i < TONE_MAX; i++)
		{
			CSTone tn = tones[i];
			
			if(tn == null)
				continue;
			
			int cx = tn.getX() - posX;
			int cy = tn.getY() - posY;
			int cxl = cx+1;
			int cxr = cx-1;
			if(cx > 0)
				cxl *= 2;
			if(cx < 0)
				cxr *= 2;
			if(cy > 0)
				cy *= 2;
			double distl = Math.sqrt(cxl*cxl+cy*cy);
			double distr = Math.sqrt(cxr*cxr+cy*cy);
			
			// warning, these formulae are probably wrong -GM
			voll[i] = 900.0/Math.pow(30.0+distl, 2.0);
			volr[i] = 900.0/Math.pow(30.0+distr, 2.0);
			offsl[i] = ISPEED_OF_SOUND*distl;
			offsr[i] = ISPEED_OF_SOUND*distr;
			//System.out.printf("%2d: %.4f %.4f / %.4f %.4f\n", i, voll[i], volr[i], offsl[i], offsr[i]);
		}
		
		for(int i = 0; i < len;)
		{
			double lv = -clipOffs;
			double rv = -clipOffs;
			int cx, cy;
			
			for(int j = 0; j < TONE_MAX; j++)
			{
				CSTone tn = tones[j];
				
				if(tn == null)
					continue;
				
				lv += tn.updateNoBump(FREQ_IBASE, -offsl[j])*voll[j];
				rv += tn.update(FREQ_IBASE, -offsr[j]+0.0002)*volr[j];
			}
			
			if(lv >= 1.0)
			{
				clipOffs += lv - 1.0;
				lv = 1.0;
			} else if(lv <= -1.0) {
				clipOffs += lv + 1.0;
				lv = -1.0;
			}
			
			buf[i++] = (byte)(120*lv);
			//buf[i++] = (byte)(120*256*lv);
			
			if(rv >= 1.0)
			{
				clipOffs += rv - 1.0;
				rv = 1.0;
			} else if(rv <= -1.0) {
				clipOffs += rv + 1.0;
				rv = -1.0;
			}
			
			buf[i++] = (byte)(120*rv);
			//buf[i++] = (byte)(120*256*rv);
			
			clipOffs = clipOffs * 0.997;
		}
		
		for(int i = 0; i < TONE_MAX; i++)
		{
			CSTone tn = tones[i];
			
			if(tn == null)
				continue;
			
			if(!tn.isAlive(tn.getPos()-0.04))
				tones[i] = null;
		}
		dsp.write(buf, 0, len);
	}
	
	public void run()
	{
		AudioFormat afmt = new AudioFormat(
			(float)FREQ_BASE, // float sampleRate
			8, // int sampleSizeInBits - not until OpenJDK gets 16-bit right (the proprietary one sucks tbqh)
			2, // int channels
			true, // boolean signed - you DO want this, asiekierka
			true // boolean bigEndian - not a dumb idea
		);
		
		try
		{
			dsp = AudioSystem.getSourceDataLine(afmt);
			
			// if we do this part wrong, we can end up with code which
			// doesn't work properly on OpenJDK,
			// but will work on the proprietary sound code.
			// Head Trauma (my LD18 entry) is one such example.
			//
			// having said that, it's not hard to do it right.
			//     -GM
			dsp.open();
			dsp.start();
		} catch(LineUnavailableException ex) {
			System.err.println("LineUnavailableException came up - you can't have sound, dear");
			System.err.printf("msg: %s\n", ex.getMessage());
			ex.printStackTrace();
			return;
		}
		
		// prepare buffer
		buf = new byte[BUF_SIZE*4];
		
		while(stillAlive) // when the science gets done and you make a neat gun
		{
			getStuff((int)(FREQ_BASE*0.01));
			try
			{
				Thread.sleep(10);
			} catch(InterruptedException ex) {
				Thread.interrupted(); // apparently this is the correct way to deal with this crap
			}
		}
	}
	
	public void allocateTone(CSTone tn)
	{
		double bestPos = 0.0;
		int bestIdx = 0;
		
		// do two checks at once:
		// 1. if there's an empty slot, fill it in.
		// 2. if there's not, override the tone which has played for the longest.
		for(int i = 0; i < TONE_MAX; i++)
		{
			if(tones[i] == null)
			{
				tones[i] = tn;
				return;
			} else {
				double cPos = tones[i].getPos();
				if(cPos > bestPos)
				{
					bestPos = cPos;
					bestIdx = i;
				}
			}
		}
		
		tones[bestIdx] = tn;
	}
	
	public void playNote(int x, int y, int note, double vol)
	{
		if(note < 240)
			allocateTone(new CSMelodium(x, y, noteTab[note], vol));
		else switch(note-240) // done this way for compiled code size reasons -GM
		{
			case 0: // kick
				allocateTone(new CSKickDrum(x, y, vol));
				break;
			case 1: // snare
				allocateTone(new CSNoise(x, y, 3520, 0.5, vol));
				break;
			case 2: // closed hihat
				allocateTone(new CSNoise(x, y, 7040, 0.05, vol));
				break;
			case 3: // open hihat
				allocateTone(new CSNoise(x, y, 7040, 0.25, vol));
				break;
			case 4: // hi tom
				allocateTone(new CSTomDrum(x, y, 300, vol));
				break;
			case 5: // mid tom
				allocateTone(new CSTomDrum(x, y, 200, vol));
				break;
			case 6: // low tom
				allocateTone(new CSTomDrum(x, y, 120, vol));
				break;
			case 7: // crash
				allocateTone(new CSNoise(x, y, 7040, 2.0, vol));
				break;
			default:
				System.out.printf("TODO drum %d\n", note);
				break;
		}
	}
	
	public void playSample(int x, int y, CSSampleData sd, double vol)
	{
		allocateTone(new CSSample(x, y, sd, vol));
	}
	
	public void playSampleByNumber(int x, int y, int smp, double vol)
	{
		switch(smp)
		{
			case 0:
				playSample(x,y,smpPlateOff,vol);
				break;
			case 1:
				playSample(x,y,smpPlateOn,vol);
				break;
			case 2:
				playSample(x,y,smpDoorClose,vol);
				break;
			case 3:
				playSample(x,y,smpDoorOpen,vol);
				break;
			default:
				System.out.printf("TODO sample %d\n", smp);
		}
	}
	
	public void setPos(int posX, int posY)
	{
		this.posX = posX;
		this.posY = posY;
	}
	
	// test function thing
	public static void main(String[] args)
	{
		Sound cs = new Sound();
		
		try{Thread.sleep(250);}catch(InterruptedException ex){}
		try
		{
			while(true)
			{
				int xnote = 80-24;
				cs.playNote(0,0,247,0.7);
				for(int j = 0; j < 8; j++)
					for(int i = 0; i < 8; i++)
					{
						if(j == 7 && i >= 5)
						{
							cs.playNote(0,0,244+i-5,0.6);
						} else {
							cs.playNote(0,0,i == 7 ? 243 : 242,0.3);
							if(i == 0 || i == 5)
								cs.playNote(0,0,240,0.5);
							if(i == 2 || i == 6)
								cs.playNote(0,0,241,0.5);
						}
						if(i == 1 || i == 3 || i == 4 || i == 7)
							cs.playNote(0,0,xnote,0.7);
						switch(i&3)
						{
							case 0:
								cs.playNote(0,0,104,0.6);
								break;
							case 1:
								cs.playNote(0,0,110,0.6);
								break;
							case 2:
								cs.playNote(0,0,118,0.6);
								break;
							case 3:
								cs.playNote(0,0,128,0.6);
								break;
						}
						try{Thread.sleep(75);}catch(InterruptedException ex){}
						if(i == 3 || i == 4)
							cs.playNote(0,0,242,0.2);
						if(i == 5)
							cs.playNote(0,0,xnote,0.7);
						try{Thread.sleep(75);}catch(InterruptedException ex){}
						if(i == 7)
							xnote = (86-24+80-24)-xnote;
					}
			}
		} catch(Exception ex) {
			System.out.println("interrupt");
		}
		cs.kill();
	}
}
