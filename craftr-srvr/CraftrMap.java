import java.lang.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class CraftrMap
{
	public CraftrChunk[] chunks;
	public int cachesize;
	public int genMode = 0;
	public static Random rand = new Random();
	public String saveDir;
	public static final int maxType = 8;
	public boolean multiplayer;
	public boolean maplock;
	public boolean modlock;
	public CraftrServer se;
    public Set<CraftrBlockPos> blockcheck = new HashSet<CraftrBlockPos>();
    private Set<CraftrBlockPos> blockcheckold = new HashSet<CraftrBlockPos>();
    private Set<CraftrBlock> blockset = new HashSet<CraftrBlock>();
    private Set<CraftrBlock> blocksetold = new HashSet<CraftrBlock>();
    private Set<CraftrBlock> blocksset = new HashSet<CraftrBlock>();
    private Set<CraftrBlock> blockssetold = new HashSet<CraftrBlock>();
	public boolean bslock = false;
	// CONSTRUCTORS

	public CraftrMap(int _cachesize)
	{
		this(_cachesize,false);
	}

	public CraftrMap(int _cachesize, boolean multiMode)
	{
		multiplayer = false;
		chunks = new CraftrChunk[_cachesize];
		cachesize = _cachesize;
		saveDir = "";
		for(int i=0;i<cachesize;i++)
		{
			chunks[i] = new CraftrChunk(0,0,false);
			chunks[i].isUsed = false;
			chunks[i].isSet = false;
		}
	}
	
	public void resizeChunks(int _cachesize)
	{
		chunks = new CraftrChunk[_cachesize];
		cachesize = _cachesize;
		for(int i=0;i<cachesize;i++)
		{
			chunks[i] = new CraftrChunk(0,0,false);
			chunks[i].isUsed = false;
			chunks[i].isSet = false;
		}
	}
	
	Random randc = new Random();
	
	// CHUNK LOCATING	
	public CraftrChunk grabChunk(int x, int y) throws NoChunkMemException
	{
		// Is the chunk cached?
		for (int i=0;i<cachesize;i++) if(chunks[i].xpos == x && chunks[i].ypos == y && chunks[i].isSet == true) { chunks[i].isUsed = true; return chunks[i]; }
		
		// No, is there any empty chunk?
		for (int i=0;i<cachesize;i++) if(!chunks[i].isSet) { chunks[i] = new CraftrChunk(x,y,true); chunks[i].isUsed = true; chunks[i].loadByte(readChunkFile(x,y,true)); return chunks[i]; }
		
		// No, is there any chunk that isn't in the closest area of players'?
		for (int i=0;i<cachesize;i++) if(!chunks[i].isUsed) { chunks[i] = new CraftrChunk(x,y,true); chunks[i].isUsed = true; chunks[i].loadByte(readChunkFile(x,y,true)); return chunks[i]; }

		// Use up the first chunk you see 
		int i = randc.nextInt(cachesize);
		chunks[i] = new CraftrChunk(x,y,true);
		chunks[i].isUsed = true;
		chunks[i].loadByte(readChunkFile(x,y,true));
		return chunks[i];
				
		// Are you kidding me!? 9 chunks are (should be) used and you can't find a free space in SIXTY-FOUR!?
		// I throw an exception right now.
		//throw new NoChunkMemException(x,y);
	}
	
	public CraftrChunk findCachedChunk(int x, int y)
	{
		// Is the chunk cached?
		for (int i=0;i<cachesize;i++) if(chunks[i].xpos == x && chunks[i].ypos == y && chunks[i].isSet == true) return chunks[i];
	
		// No, who cares.
		return null;
	}
	
	public int findChunkID(int x, int y)
	{
		// Is the chunk cached?
		for (int i=0;i<cachesize;i++) if(chunks[i].xpos == x && chunks[i].ypos == y && chunks[i].isSet == true) return i;

		// No, who cares.
		return -1;
	}

	public int findNewChunkID(int x, int y)
	{
		// Is the chunk cached?
		for (int i=0;i<cachesize;i++) if(chunks[i].xpos == x && chunks[i].ypos == y && chunks[i].isSet == true) return i;
	
		// No, is there any empty chunk?
		for (int i=0;i<cachesize;i++) if(!chunks[i].isSet) { chunks[i] = new CraftrChunk(x,y,true); return i; }
		
		// No, is there any chunk that isn't in the closest area of players'?
		for (int i=0;i<cachesize;i++) if(!chunks[i].isUsed) { chunks[i] = new CraftrChunk(x,y,true); return i; }

		// No, who cares.
		return -1;
	}
	
	// HANDLING isUsed
	public void setUsed(int x, int y)
	{
		for (int i=0;i<cachesize;i++) if(chunks[i].xpos == x && chunks[i].ypos == y) { chunks[i].isReUsed = true;}
	}
	
	public void clearAllUsed()
	{
		for (int i=0;i<cachesize;i++)
		{
			if(chunks[i].isSet && chunks[i].isUsed == true && chunks[i].isReUsed == false)
			{
				saveChunkFile(i);
				chunks[i].isUsed = false;
			} else if (chunks[i].isReUsed == true && chunks[i].isSet)
			{
				chunks[i].isUsed = true;
				chunks[i].isReUsed = false;
			}
		}
	}
	
	// NEW CHUNK GENERATION
	public CraftrChunk generateChunk(int cx, int cy, boolean used)
	{
		CraftrChunk out = new CraftrChunk(0,0,used);
		out.generate(genMode);
		return out;
	}
	
	// LOADING/SAVING - UTILITIES
	public void checkDirs(int x, int y)
	{
		File tmf = new File(saveDir + "map");
		if(!tmf.exists()) tmf.mkdir();
		tmf = new File(saveDir + "map/" + y);
		if(!tmf.exists()) tmf.mkdir();
	}
	
	// LOADING
	public CraftrChunk readChunkFromFile(int x, int y)
	{
		byte[] tmp = readChunkFile(x,y,false);
		if(tmp != null) {
			CraftrChunk n = new CraftrChunk(x,y,false);
			n.loadByte(tmp);
			return n;
		}
		else return null;
	}
	public void readChunkFile(int x, int y, int cid)
	{
		if (cid >= 0)
		{
			chunks[cid] = new CraftrChunk(x,y,true);
			chunks[cid].loadByte(readChunkFile(x,y,true));
		}
	}
	public byte[] readChunkFile(int x, int y, boolean genNew)
	{
		// Init variables
		FileInputStream in = null;
		GZIPInputStream gin = null;
		byte[] out = new byte[16384];
		try	// The code proper
		{
			// Load file
			in = new FileInputStream("map/" + CraftrChunk.getFilename(x,y));
			gin = new GZIPInputStream(in);
			// Create buffer, check version
			byte[] buf = new byte[256];
			gin.read(buf,0,1);
			int i = 0;
			int hdrsize = CraftrChunk.hdrsize;
			switch(buf[0])
			{
				case 1: // ver. 1
					while(i<16384) i += gin.read(out,i,16384-i);
					gin.close();
					byte[] out2 = new byte[(4096*7)+hdrsize];
					System.arraycopy(out,0,out2,hdrsize,4096);
					for(i=0;i<4096;i++)
					{
						if(out2[i]==0)
						{
							out2[(4096*2)+i+hdrsize] = out[4096+i];
							out2[(4096*4)+i+hdrsize] = out[8192+i];
							out2[(4096*6)+i+hdrsize] = out[12288+i];
						}
						else
						{
							out2[4096+i+hdrsize] = out[4096+i];
							out2[(4096*3)+i+hdrsize] = out[8192+i];
							out2[(4096*5)+i+hdrsize] = out[12288+i];
						}
					}
					for(int ri=0;ri<4096;ri++)
					{
						if(out2[ri+hdrsize]==5 && (0x80&(int)out2[ri+hdrsize+4096])>0)
						{
							out2[ri+hdrsize+4096]=(byte)1;
							addbc(new CraftrBlockPos(x*64+(ri&63),y*64+(ri>>6)));
						}
					}
					return out2;
				case 2:
					out = new byte[(4096*7)+hdrsize+4096];
					while(i<(4096*7)) i += gin.read(out,i+hdrsize,(4096*7)-i);
					gin.close();
					for(int ri=0;ri<4096;ri++)
					{
						if(out[ri+hdrsize]==5 && (0x80&(int)out[ri+hdrsize+4096])>0)
						{
							out[ri+hdrsize+4096]=(byte)1;
							addbc(new CraftrBlockPos(x*64+(ri&63),y*64+(ri>>6)));
						}
					}
					return out;
				case 3:
					out = new byte[(4096*7)+hdrsize+4096];
					while(i<out.length && i>-1) i += gin.read(out,i,out.length-i);
					gin.close();
					for(int ri=0;ri<4096;ri++)
					{
						if(out[ri+hdrsize]==5 && (0x80&(int)out[ri+hdrsize+4096])>0)
						{
							out[ri+hdrsize+4096]=(byte)1;
							addbc(new CraftrBlockPos(x*64+(ri&63),y*64+(ri>>6)));
						}
					}
					return out;
				default:
					System.out.println("ReadChunkFile: unknown version: " + buf[0]);
					break;
			}
			System.out.println("Read chunk " + x + ", " + y);
		}
		catch (FileNotFoundException e)
		{
			// FileInputStream - file was not found.
			CraftrChunk nout = null;
			if(genNew) nout = generateChunk(x, y,false);
			else return null;
			saveChunkFile(x,y,nout.saveByte());
			return nout.saveByte();
		}
		catch (Exception e)
		{
			// Something else happened!
			System.out.println("ReadChunkFile: exception: " + e.getMessage());
			return out;
		}
		finally
		{
			try
			{
				if(gin != null && in != null) {gin.close(); in.close();}
			}
			catch (Exception e) { System.out.println("ReadChunkFile: warning - file in streams didn't close"); }
		}
		return out;
	}
	
	// SAVING
	public void saveChunkFile(int cid)
	{
		CraftrChunk cin = chunks[cid];
		if (cin != null)
		{
			saveChunkFile(cin.xpos,cin.ypos,cin.saveByte());
		}
	}
	public void saveChunkFile(int x, int y)
	{
		CraftrChunk cin = findCachedChunk(x,y);
		if (cin != null)
		{
			saveChunkFile(x,y,cin.saveByte());
		}
	}
	public void saveChunkFile(int x, int y, byte[] data)
	{
		if(multiplayer) return;
		FileOutputStream fout = null;
		GZIPOutputStream gout = null;
		try
		{
			checkDirs(x,y);
			fout = new FileOutputStream(saveDir + "map/" + CraftrChunk.getFilename(x,y));
			gout = new GZIPOutputStream(fout);
			gout.write(data,0,data.length);
		}
		catch (Exception e)
		{
			System.out.println("SaveChunkFile: exception: " + e.toString());
		}
		finally
		{
			try
			{
				if(gout != null && fout != null) {gout.finish(); gout.close(); fout.close();}
			}
			catch (Exception e) { System.out.println("SaveChunkFile: warning - file out streams didn't close"); }
		}
	}
	public void loop()
	{
		if(multiplayer)return;
		while(modlock) { try{ Thread.sleep(1); } catch(Exception e) {} }
		maplock=true;
		Set<CraftrBlockPos> tempb;
		synchronized(blockcheck)
		{
			tempb = blockcheck;
			blockcheck = blockcheckold;
			blockcheckold = tempb;
			blockcheck.clear();
		}
		
		for(CraftrBlockPos cbp:blockcheckold)
		{
			phyCheck(cbp.x,cbp.y);
		}
		Set<CraftrBlock> temps;
		synchronized(blockset)
		{
			temps = blockset;
			blockset = blocksetold;
			blocksetold = temps;
			blockset.clear();
		}
		for(CraftrBlock cba:blocksetold)
		{
			CraftrBlock cb=cba.copy();
			if(!cb.setOnlyType)
			{
				byte[] z = getBlock(cb.x,cb.y);
				setBlockSuper(cb.x,cb.y,cb.type,cb.param,cb.chr,cb.col);
				cb.chr=(byte)updateLooks(cb.x,cb.y,cba.chr);
				setBlockSuper(cb.x,cb.y,cb.type,cb.param,cb.chr,cb.col);
				for(int i=0;i<4;i++)
				{
					byte[] t = getBlock(cb.x+xMovement[i],cb.y+yMovement[i]);
					byte tc = (byte)updateLooks(cb.x+xMovement[i],cb.y+yMovement[i],t[2]);
					if(t[2]!=tc)
					{
						setBlockSuper(cb.x+xMovement[i],cb.y+yMovement[i],t[0],t[1],tc,t[3]);
						setBlockNet(cb.x+xMovement[i],cb.y+yMovement[i],t[0],tc,t[3]);
					}
				}
				if(z[0]!=cb.type || z[2]!=cb.chr || z[3]!=cb.col)	 setBlockNet(cb.x,cb.y,cb.type,cb.chr,cb.col);
			}
			else
			{
				//setType(cb.x,cb.y,cb.type);
			}
		}
		synchronized(blocksset)
		{
			temps = blocksset;
			blocksset = blockssetold;
			blockssetold = temps;
			blocksset.clear();
		}
		for(CraftrBlock cba:blockssetold)
		{
			CraftrBlock cb=cba.copy();
			if(!cb.setOnlyType)
			{
				byte[] z = getBlock(cb.x,cb.y);
				setBlockSuper(cb.x,cb.y,cb.type,cb.param,cb.chr,cb.col);
				cb.chr=(byte)updateLooks(cb.x,cb.y,cba.chr);
				setBlockSuper(cb.x,cb.y,cb.type,cb.param,cb.chr,cb.col);
				for(int i=0;i<4;i++)
				{
					byte[] t = getBlock(cb.x+xMovement[i],cb.y+yMovement[i]);
					byte tc = (byte)updateLooks(cb.x+xMovement[i],cb.y+yMovement[i],t[2]);
					if(t[2]!=tc)
					{
						setBlockSuper(cb.x+xMovement[i],cb.y+yMovement[i],t[0],t[1],tc,t[3]);
						setBlockNet(cb.x+xMovement[i],cb.y+yMovement[i],t[0],tc,t[3]);
					}
				}
				if(z[0]!=cb.type || z[2]!=cb.chr || z[3]!=cb.col)	 setBlockNet(cb.x,cb.y,cb.type,cb.chr,cb.col);
			}
			else
			{
				//setType(cb.x,cb.y,cb.type);
			}
		}
		maplock=false;
	}
	
	public void addbc(CraftrBlockPos cbp)
	{
		synchronized(blockcheck)
		{
			// should blockcheckold be checked, too? --GM
			//if(!blockcheck.contains(cbp)) blockcheck.add(cbp);
			blockcheck.add(cbp);
		}
	}
	
	public void addbs(CraftrBlock cb)
	{
		synchronized(blockset)
		{
			blockset.add(cb);
		}
		//if(!blockset.contains(cb)) blockset.add(cb);
	}
	public void addbss(CraftrBlock cb)
	{
		synchronized(blockset)
		{
			blocksset.add(cb);
		}
		//if(!blockset.contains(cb)) blockset.add(cb);
	}	
	public void setPlayerNet(int x, int y, int on)
	{
		try
		{
			se.out.writeByte(0x2C|on);
			se.out.writeInt(x);
			se.out.writeInt(y);
			byte[] t = se.getPacket();
			se.sendAll(t,t.length);
		}
		catch(Exception e) { System.out.println("setPlayerNet sending error!"); }
	}
	public void setPlayer(int x, int y, int on)
	{
		byte[] d = getBlock(x,y);
		switch(d[0])
		{
			case 5:
				int d15 = (d[1]&0x7F)|(on<<7);
				if(((int)d[1]&0x80)==0 && on>0) d15=(d15&0x80)|4;
				while(maplock) { try{ Thread.sleep(1); } catch(Exception e) {} }
				modlock=true;
				setBlock(x,y,d[0],(byte)d15,d[2],d[3]);
				addbc(new CraftrBlockPos(x,y));
				for(int i=0;i<4;i++)
				{
					addbc(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
				}
				setPlayerNet(x,y,on);
				modlock=false;
				break;
			default:
				break;
		}
	}
	public boolean isEmpty(int x, int y)
	{
		byte[] d = getBlock(x,y);
		if(d[0]==0 || d[0]==2 || d[0]==5 || (d[0]==6 && ((int)d[1]&0x80)>0) || d[0]==8) return true;
		return false;
	}
	public static final int[] xMovement = { -1, 1, 0, 0 };
	public static final int[] yMovement = { 0, 0, -1, 1 };
	public static final int[] wiriumChr = { 197,179,179,179,196,218,192,195,196,191,217,180,196,194,193,197};
	public static final int[] pnandDir = {26,27,25,24};
	public static final int[] pnandDir2 = {27,26,24,25};
	public int updateLooks(int x, int y, int currchr)
	{
		byte[] d = getBlock(x,y);
		if(d[0]==4) return 206;
		if(d[0]==3 && (d[2]<24 || d[2]>=28)) return 25;
		if(d[0]!=2) return currchr;
		byte[] d2 = new byte[4];
		int wcp = 0;
		for(int i=0;i<4;i++)
		{
			d2=getBlock(x+xMovement[i],y+yMovement[i]);
			if((d2[0]>=2 && d2[0]<=7)) wcp|=(1<<(3-i));
		}
		return wiriumChr[wcp];
	}
	public void phyCheck(int x, int y)
	{
		byte[] d = getBlock(x,y);
		byte[][] dt = new byte[4][];
		int[][] d2 = new int[4][4];
		for(int i=0;i<4;i++)
		{
			dt[i]=getBlock(x+xMovement[i],y+yMovement[i]);
			for(int j=0;j<4;j++)
			{
				d2[i][j]=0xFF&(int)dt[i][j];
			}
		}
		int[] strength = new int[4];
		for(int i=0;i<4;i++)
		{
			switch(d2[i][0])
			{
				case 2:
					strength[i]=(d2[i][1]>>4)!=(i^1)?d2[i][1]&15:0;
					break;
				case 3:
					if(pnandDir[i]==d2[i][2]) strength[i]=d2[i][1]&15;
					break;
				case 4:
					if( ((d2[i][1]>>(i^1))&1)!=0 ) strength[i]=d2[i][1]>>4;
					break;
				case 5:
					if(d2[i][1]>0) strength[i]=15;
					break;
				default:
					strength[i]=0;
					break;
			}
		}
		byte oldd3 = d[3];
		byte oldd1 = d[1];
		int maxSignal = 0;
		int ss = d[1]&15;
		switch(d[0])
		{
			case 2:
				int mSi=4;
				int oldmSi=((0xFF&(int)d[1])>>4)&7;
				for(int i=0;i<4;i++)
				{
					int ty = d2[i][0];
					int str = strength[i];
					if(oldmSi<4 && (oldmSi^1)==i) continue;
					if(str>maxSignal && str>ss) { maxSignal=str; mSi=i;}
				}
				if(maxSignal<=1)
				{
					if(ss>0) d[3]=(byte)(d[3]&7);
					if(oldd1!=((byte)(mSi<<4)))
					{
						addbs(new CraftrBlock(x,y,d[0],(byte)(mSi<<4),d[2],d[3]));
						addbc(new CraftrBlockPos(x,y));
						for(int i=0;i<4;i++)
						{
							int ty = d2[i][0];
							if(ty==2 || (ty==3) || ty==4 || ty==6 || ty==7) { addbc(new CraftrBlockPos(x+xMovement[i],y+yMovement[i])); }
						}
					}
				}
				else
				{
					if(ss==0) d[3]=(byte)((d[3]&7)|8);
					if(oldd1!=((byte)((maxSignal-1) | (mSi<<4))))
					{
						addbs(new CraftrBlock(x,y,d[0],(byte)((maxSignal-1) | (mSi<<4)),d[2],d[3]));
						addbc(new CraftrBlockPos(x,y));
						for(int i=0;i<4;i++)
						{
							int ty = d2[i][0];
							if(ty==2 || (ty==3) || ty==4 || ty==6 || ty==7) { addbc(new CraftrBlockPos(x+xMovement[i],y+yMovement[i])); }
						}
					}
				}
				if(oldd1!=d[1])
				{
				}
				break;
			case 3:
				int pnps = 3;
				if (d[2]>=24 && d[2]<28)
				{
					for(int i=0;i<4;i++)
					{
						if(pnandDir2[i]==d[2]) { pnps=i; break; }
					}
				} 
				int signals=0;
				for(int i=0;i<4;i++)
				{
					if(i==pnps) continue;
					int ty = d2[i][0];
					int str = strength[i];
					if((ty==1) || str>0) { signals++; }
				}
				if(signals==1 || signals==2)
				{
					if(ss==0) d[3]=(byte)(((d[3]>>4)&15)|((d[3]<<4)&240));
					addbs(new CraftrBlock(x,y,d[0],(byte)15,d[2],d[3]));
					int ty = d2[pnps][0];
					d[1]=15;
					int str = strength[pnps];
					if(oldd1!=15 && (ty==2|| ty==3 || ty==4 || ty==6 || ty==7)) { addbc(new CraftrBlockPos(x+xMovement[pnps],y+yMovement[pnps])); }
				}
				else
				{
					if(ss>0) d[3]=(byte)(((d[3]>>4)&15)|((d[3]<<4)&240));
					addbs(new CraftrBlock(x,y,d[0],(byte)0,d[2],d[3]));
					int ty = d2[pnps][0];
					d[1]=0;
					int str = strength[pnps];
					if(oldd1!=0 && (ty==2 || ty==3 || ty==4 || ty==6 || ty==7)) { addbc(new CraftrBlockPos(x+xMovement[pnps],y+yMovement[pnps])); }
				}
				if(oldd1!=d[1])
				{
					for(int i=0;i<4;i++)
					{
						int t = d2[i][0];
						int str = strength[i];
						if(t==2 ||t==3||t==4 ||  t==6 || t==7) addbc(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
				break;
			case 4:
				int newParam=0;
				int oldparam = 0xFF&(int)d[1];
				//int maxSignal=0;
				for(int i=0;i<4;i++)
				{
					// SETUP
					int t = d2[i][0];
					int str = strength[i];
					int rstr = strength[i^1];
					// PARAM CONFIGURATION
					boolean t1 = ((oldparam>>i)&1)>0; // was it sending in that direction?
					boolean t3 = ((oldparam>>(i^1))&1)>0; // was it sending opposite?
					boolean t2 = false; // should it be sending in that direction?
					if(!t3 && rstr>1)
					{
						t2=true;
						int t4 = 0xFF&(int)d2[i^1][1];
						boolean t5 = ((t4>>i)&1)!=0;
						//meaning i 
						if((d2[i^1][0]==2 && (d2[i^1][1]>>4)==i)) t2=false;
						//if(t==2 && (d2[i][1]>>4)==i) t2=false;
					}
					//if((!t3 && !t1 && rstr>1 && str<=1) || (!t3 && t1 && rstr>1)) t2=true;
					if(t2) { newParam|=1<<i; }
					else if (str>maxSignal && str>1) { maxSignal=str; }
				}
				if(maxSignal>1)
				{
					   newParam |= ((maxSignal-1)<<4);
				}
				if(oldd1!=newParam)
				{
					addbs(new CraftrBlock(x,y,d[0],(byte)newParam,d[2],d[3]));
					for(int i=0;i<4;i++)
					{
						int t = d2[i][0];
						int str = strength[i];
						if(t==2 ||t==3||t==4 || t==6 || t==7) addbc(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
				break;
			case 5:
				int co5 = d[1]&0x7F;
				int on = (int)d[1]&0x80;
				if(co5>0)
				{
					if(co5>1) addbc(new CraftrBlockPos(x,y));
					for(int i=0;i<4;i++)
					{
						int t = d2[i][0];
						int str = strength[i];
						if(t==2 ||t==3||t==4 || t==6 || t==7) addbc(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
					addbs(new CraftrBlock(x,y,d[0],on|(co5-1),d[2],d[3]));
				}
				break;
			case 6:
				int signalz=0;
				int counter = d[1]&0x7F;
				int ct = 0x80&(int)d[1];
				if(counter>0) counter-=1;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { signalz++; }
				}	
				if(signalz>0)
				{
					addbs(new CraftrBlock(x,y,d[0],(byte)(counter|0x80),d[2],d[3]));
					if(ct==0) setPlayerNet(x,y,1);
				}
				else
				{
					if(ct>0) setPlayerNet(x,y,0);
					addbs(new CraftrBlock(x,y,d[0],(byte)counter,d[2],d[3]));
				}
				//addbc(new CraftrBlockPos(x,y));
				for(int i=0;i<4;i++)
				{
					int t = d2[i][0];
					int str = strength[i];
					if(t==2 ||t==3||t==4 || t==6 || t==7) addbc(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
				}
				break;
			case 7:
				int sig7=0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { sig7++; }
				}
				if(sig7>0 && (d[1]&1)==0)
				{
					for(int i=0;i<255;i++)
					{
						if(se.clients[i] != null && se.clients[i].dc == 0) se.clients[i].playSound(x,y,(0xFF&(int)d[2])%248);
					}
				}
				int np=sig7>0?1:0;
				if((d[1]&1)!=np) addbs(new CraftrBlock(x,y,d[0],(byte)np,d[2],d[3]));
				break;
			default:
				break;
		}
	}
	public byte[] getBlock(int x, int y)
	{
		try
		{ 
			byte[] data = new byte[4];
			int px = x&63;
			int py = y&63;
			synchronized(chunks)
			{
				CraftrChunk cnk = grabChunk((x>>6),(y>>6));
				data[0] = cnk.getBlockType(px,py);
				data[1] = cnk.getBlockParam(px,py);
				data[2] = cnk.getBlockChar(px,py);
				data[3] = cnk.getBlockColor(px,py);
			}
			return data;
		}
		catch(NoChunkMemException e)
		{
			System.out.println("getBlock: exception: no chunk memory found. Odd...");
			//System.exit(1); // someone might still use this
			return null;
		}
	}
	public void setBlock(int x, int y, byte[] data)
	{
		try
		{ 
			int px = x&63;
			int py = y&63;
			synchronized(chunks) { grabChunk((x>>6),(y>>6)).place(px,py,data[0],data[2],data[3],data[1]); }
		}
		catch(NullPointerException e)
		{
			System.out.println("setBlock: no cached chunk near player found. ODD.");
			//if(!multiplayer) System.exit(1);
		}
	}
	
	public void setBlockNet(int x, int y, byte t1, byte ch1, byte co1)
	{
		try
		{ 
			int px = x&63;
			int py = y&63;
			//findCachedChunk((x>>6),(y>>6)).place(px,py,t1,ch1,co1,(byte)0);
			se.out.writeByte(0x31);
			se.out.writeByte((byte)255);
			se.out.writeInt(x);
			se.out.writeInt(y);
			se.out.writeByte(t1);
			se.out.writeByte(ch1);
			se.out.writeByte(co1);
			byte[] t = se.getPacket();
			se.sendAll(t,t.length);
		}
		catch(Exception e)
		{
			System.out.println("setBlockNet exception!");
			e.printStackTrace();
			//if(!multiplayer) System.exit(1);
		}
	}
	
	public void setBlock(int x, int y, byte t1, byte ch1, byte co1)
	{
		setBlock(x,y,t1,(byte)0,ch1,co1);
	}
	public void setBlock(int x, int y, byte t1, byte p1, byte ch1, byte co1)
	{
		try
		{ 
			int px = x&63;
			int py = y&63;
			//System.out.println("setBlock at chunk " + (x>>6) + "," + (y>>6) + ", pos " + px + "," + py);
			synchronized(chunks) { grabChunk((x>>6),(y>>6)).place(px,py,t1,ch1,co1,p1); }
		}
		catch(NullPointerException e)
		{
			System.out.println("setBlock: no cached chunk near player found. ODD.");
			//if(!multiplayer) System.exit(1);
		}
	}
	
	public void setBlockSuper(int x, int y, byte t1, byte ch1, byte co1)
	{
		setBlockSuper(x,y,t1,(byte)0,ch1,co1);
	}
	public void setBlockSuper(int x, int y, byte t1, byte p1, byte ch1, byte co1)
	{
		try
		{ 
			int px = x&63;
			int py = y&63;
			//System.out.println("setBlock at chunk " + (x>>6) + "," + (y>>6) + ", pos " + px + "," + py);
			synchronized(chunks)
			{
				grabChunk((x>>6),(y>>6)).place(px,py,t1,ch1,co1,p1);
			}
		}
		catch(NullPointerException e)
		{
			System.out.println("setBlock: no cached chunk near player found. ODD.");
			//if(!multiplayer) System.exit(1);
		}
		catch(NoChunkMemException e2)
		{
			System.out.println("setBlock: no chunk memory found! Shutting down...");
			System.exit(1);
			//if(!multiplayer) System.exit(1);
		}
	}
}
