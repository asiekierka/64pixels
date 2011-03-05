package common;

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
	public static final int maxType = 11;
	public boolean multiplayer;
	public boolean maplock;
	public boolean modlock;
	
	public boolean isServer;
	
	// just inferfaces for the client / server specifics -GM
	public CraftrGameShim game;
	public CraftrNetShim net;
	public CraftrServerShim se;
	
	public CraftrPhysics physics;
	Random randc = new Random();
	// CONSTRUCTORS

	public CraftrMap(boolean _isServer, int _cachesize)
	{
		this(_isServer,_cachesize,false);
	}

	public CraftrMap(boolean _isServer, int _cachesize, boolean multiMode)
	{
		isServer = _isServer;
		physics = new CraftrPhysics(isServer);
		multiplayer = multiMode;
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
	
	// CHUNK LOCATING	
	public CraftrChunk grabChunk(int x, int y) throws NoChunkMemException
	{
		// Is the chunk cached?
		for (int i=0;i<cachesize;i++) if(chunks[i].xpos == x && chunks[i].ypos == y && chunks[i].isSet == true) { chunks[i].isUsed = true; return chunks[i]; }
		
		// No, is there any empty chunk?
		for (int i=0;i<cachesize;i++) if(!chunks[i].isSet) { chunks[i] = new CraftrChunk(x,y,true); chunks[i].isUsed = true; chunks[i].loadByte(readChunkFile(x,y,true)); return chunks[i]; }
		
		// No, is there any chunk that isn't in the closest area of players'?
		for (int i=0;i<cachesize;i++) if(!chunks[i].isUsed) { chunks[i] = new CraftrChunk(x,y,true); chunks[i].isUsed = true; chunks[i].loadByte(readChunkFile(x,y,true)); return chunks[i]; }
		
		if(isServer)
		{
			// Use up the first chunk you see 
			int i = randc.nextInt(cachesize);
			chunks[i] = new CraftrChunk(x,y,true);
			chunks[i].isUsed = true;
			chunks[i].loadByte(readChunkFile(x,y,true));
			return chunks[i];
		} else {
			// Are you kidding me!? 9 chunks are (should be) used and you can't find a free space in SIXTY-FOUR!?
			// I throw an exception right now.
			throw new NoChunkMemException(x,y);
		}
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
		if(multiplayer)
		{
			System.out.println("request: " + x + ", " + y);
			if(!isServer)
				net.chunkRequest(x,y);
			CraftrChunk nout = new CraftrChunk(x,y,true);
			return nout.saveByte();
		}
		else
		{
		// Init variables
		FileInputStream in = null;
		GZIPInputStream gin = null;
		byte[] out = new byte[16384];
		try	// The code proper
		{
			// Load file
			in = new FileInputStream(saveDir + "map/" + CraftrChunk.getFilename(x,y));
			gin = new GZIPInputStream(in);
			// Create buffer, check version
			byte[] buf = new byte[256];
			gin.read(buf,0,1);
			int i = 1;
			int hdrsize = CraftrChunk.hdrsize;
			switch(buf[0])
			{
				case 3:
					out = new byte[1+(4096*10)+hdrsize];
					while(i<(1+(4096*8)+hdrsize) && i>-1) i += gin.read(out,i,(1+(4096*8)+hdrsize)-i);
					gin.close();
					for(int ri=0;ri<4096;ri++)
					{
						if(out[1+ri+hdrsize]==5 && (0x80&(int)out[1+ri+hdrsize+4096])>0)
						{
							out[1+ri+hdrsize+4096]=(byte)1;
							physics.addBlockToCheck(new CraftrBlockPos(x*64+(ri&63),y*64+(ri>>6)));
						}
					}
					return out;
				case 4:
					out = new byte[1+(4096*10)+hdrsize];
					while(i<(1+(4096*10)+hdrsize) && i>-1) i += gin.read(out,i,out.length-i);
					gin.close();
					for(int ri=0;ri<4096;ri++)
					{
						if(out[1+ri+hdrsize]==5 && (0x80&(int)out[1+ri+hdrsize+4096])>0)
						{
							out[1+ri+hdrsize+4096]=(byte)1;
							physics.addBlockToCheck(new CraftrBlockPos(x*64+(ri&63),y*64+(ri>>6)));
						}
					}
					return out;
				default:
					System.out.println("ReadChunkFile: unknown version: " + buf[0]);
					break;
			}
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
	
	public CraftrBlock getBlock(int x, int y)
	{
		return new CraftrBlock(x,y,getBlockBytes(x,y));
	}

	private byte[] getBlockBytes(int x, int y)
	{
		try
		{ 
			byte[] data = new byte[6];
			int px = x&63;
			int py = y&63;
			CraftrChunk cnk = grabChunk((x>>6),(y>>6));
			data[0] = cnk.getBlockType(px,py);
			data[1] = cnk.getBlockParam(px,py);
			data[2] = cnk.getBlockChar(px,py);
			data[3] = cnk.getBlockColor(px,py);
			data[4] = cnk.getPushableChar(px,py);
			data[5] = cnk.getPushableColor(px,py);
			return data;
		}
		catch(NoChunkMemException e)
		{
			System.out.println("getBlock: exception: no chunk memory found. Odd...");
			//System.exit(1); // someone might still use this
			return null;
 		}
 	}
 	
 	// returns true if it needs to pull the player along with it
 	public boolean pushAttempt(int lolx, int loly, int lolvx, int lolvy)
 	{
 		CraftrBlock dc = getBlock(lolx,loly);
 		CraftrBlock dt = getBlock(lolx+lolvx,loly+lolvy);
		
 		if(dc.isPushable() && dt.isEmpty())
 		{
 			if(!multiplayer)
 			{
 				synchronized(this)
 				{
 					setPushable(lolx,loly,(byte)0,(byte)0);
 					setPushable(lolx+lolvx,loly+lolvy,dc.getChar(),dc.getColor());
 				}
				for(int i=0;i<4;i++)
				{
					physics.addBlockToCheck(new CraftrBlockPos(lolx+xMovement[i],loly+yMovement[i]));
					physics.addBlockToCheck(new CraftrBlockPos(lolx+lolvx+xMovement[i],loly+lolvy+yMovement[i]));
				}
 			}
 			return true;
 		} else {
 			return false;
 		}
 	}
 	
	public void setPushable(int x, int y, int aChr, int aCol)
	{
		setPushable(x,y,(byte)aChr,(byte)aCol);
	}

 	public void setPushable(int x, int y, byte aChr, byte aCol)
 	{
 		try
 		{ 
 			int px = x&63;
 			int py = y&63;
 			//System.out.println("setBlock at chunk " + (x>>6) + "," + (y>>6) + ", pos " + px + "," + py);
 			grabChunk((x>>6),(y>>6)).placePushable(px,py,aChr,aCol);
 		}
		catch(Exception e)
		{
 			System.out.println("setPushable: exception!");
			e.printStackTrace();
 			if(!multiplayer) System.exit(1);
		}
	}
	
	public void setPlayerNet(int x, int y, int on)
	{
		try
		{
			se.out.writeByte(0x2A|on);
			se.out.writeInt(x);
			se.out.writeInt(y);
			byte[] t = se.getPacket();
			se.sendAll(t,t.length);
		}
		catch(Exception e) { System.out.println("setPlayerNet sending error!"); }
	}
	public void setPlayer(int x, int y, int on)
	{
		CraftrBlock block = getBlock(x,y);
		switch(block.getType())
		{
			case 5:
				int d15 = (block.getParam()&0x7F)|(on<<7);
				int t15 = block.getParam()&0x80;
				if(t15==0 && on>0)
				{
					d15=d15&0x80;
					playSample(x,y,1);
				}
				else if(t15>0 && on==0) playSample(x,y,0);
				while(maplock) { try{ Thread.sleep(1); } catch(Exception e) {} }
				modlock=true;
				setBlock(x,y,block.getType(),(byte)d15,block.getBlockChar(),block.getBlockColor());
				physics.addBlockToCheck(new CraftrBlockPos(x,y));
				for(int i=0;i<4;i++)
				{
					physics.addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
				}
				
				if(isServer)
					setPlayerNet(x,y,on);
				
				modlock=false;
				break;
			default:
				break;
		}
	}
	
	// this was tricky to merge, so it might still be a bit buggy --GM
	public void pushMultiple(int x, int y, int xsize, int ysize, int dx, int dy)
	{
		synchronized(this)
		{
			if(isServer)
			{
				try
				{
					se.out.writeByte(0xE1);
					se.out.writeInt(x);
					se.out.writeInt(y);
					se.out.writeShort(xsize);
					se.out.writeShort(ysize);
					se.out.writeByte(dx);
					se.out.writeByte(dy);
					byte[] t = se.getPacket();
					se.sendAll(t,t.length);
				}
				catch(Exception e){ System.out.println("Failed to send pushMultiple packet!"); }
			}
			CraftrBlock[] blocks = new CraftrBlock[xsize*ysize];
			for(int iy=0;iy<ysize;iy++)
			{
				for(int ix=0;ix<xsize;ix++)
				{
					blocks[(iy*xsize)+ix] = getBlock(x+ix,y+iy);
					setPushable(x+ix,y+iy,(byte)0,(byte)0);
					setPlayer(x+ix,y+iy,0);
				}
			}
			for(int iy=0;iy<ysize;iy++)
			{
				for(int ix=0;ix<xsize;ix++)
				{
					int arrayPos = (iy*xsize)+ix;
					if(blocks[arrayPos].isPushable()) setPushable(x+ix+dx,y+iy+dy,
		                                                          (byte)blocks[arrayPos].getChar(),
		                                                          (byte)blocks[arrayPos].getColor());
					setPlayer(x+ix+dx,y+iy+dy,1);
					for(int moveDir=0;moveDir<4;moveDir++)
					{
						physics.addBlockToCheck(new CraftrBlockPos(x+ix+dx+xMovement[moveDir],y+iy+dy+yMovement[moveDir]));
					}
				}
			}
		}
	}

	public void tryPushM(int x, int y, int dx, int dy, byte chr, byte col)
	{
		if((dx!=0 && dy!=0) || (dx==0 && dy==0)) return; // don't do diagonals.
		if(col==0) return; // we do not want non-colored pushables
		if(getBlock(x+dx,y+dy).isEmpty()) // can we not push?
		{
			setPushable(x+dx,y+dy,chr,col);
			if(isServer)
				setPushableNet(x+dx,y+dy,chr,col);
			return;
		}
		int posx = x+dx;
		int posy = y+dy;
		// we'll have to push unless we see a wall and until we have pushiums
		while(getBlock(posx,posy).isPushable())
		{
			posx+=dx;
			posy+=dy;
		}
		if( !( getBlock(posx,posy).isEmpty() ) ) return;
		int tx = posx-(x+dx);
		int ty = posy-(y+dy);
		if(tx<0) tx=-tx;
		if(ty<0) ty=-ty;
		if(dx<=0) tx++;
		if(dy<=0) ty++;
		int txs = x+dx;
		int tys = y+dy;
		if((posx-dx)<txs) txs=posx-dx;
		if((posy-dy)<tys) tys=posy-dy;
		pushMultiple(txs,tys,tx,ty,dx,dy);
		setPushable(x+dx,y+dy,chr,col);
		if(isServer)
			setPushableNet(x+dx,y+dy,chr,col);
	}

	public void playSample(int x, int y, int id)
	{
		playSound(x,y,id+256);
	}
	public void playSound(int x, int y, int id)
	{
		if(isServer)
		{
			se.playSound(x,y,id);
		} else {
			game.playSound(x,y,id);
		}
	}
	public static final int[] xMovement = { -1, 1, 0, 0 };
	public static final int[] yMovement = { 0, 0, -1, 1 };
	private static final int[] wiriumChar = { 197,179,179,179,196,218,192,195,196,191,217,180,196,194,193,197};

	public int updateLook(CraftrBlock block)
	{
		if(block.getType()==4) return 206; // default char for Crossuh blocks
		// NOTE: server used getChar() here.
		// That bug has gone unnoticed because nobody bothered to put a pushium on a P-NAND. --GM
		if(block.getType()==3 && (block.getBlockChar()<24 || block.getBlockChar()>=28)) return 25; // default char for P-NANDs
		if(block.getType()!=2) return block.getBlockChar();
		CraftrBlock surroundingBlock;
		int wiriumNeighbourInfo = 0;
		for(int moveDir=0;moveDir<4;moveDir++)
		{
			surroundingBlock=getBlock(block.x+xMovement[moveDir],block.y+yMovement[moveDir]);
			if(surroundingBlock.isWiriumNeighbour()) wiriumNeighbourInfo|=(1<<(3-moveDir));
		}
		return wiriumChar[wiriumNeighbourInfo];
	}

	public void setBlock(int x, int y, byte[] data)
	{
		try
		{ 
			if(data.length>5 && data[5]!=0) setPushable(x,y,data[4],data[5]);
			int px = x&63;
			int py = y&63;
			//System.out.println("setBlock at chunk " + (x>>6) + "," + (y>>6) + ", pos " + px + "," + py);
			findCachedChunk((x>>6),(y>>6)).place(px,py,data[0],data[2],data[3],data[1]);
		}
		catch(NullPointerException e)
		{
			if(!multiplayer) System.exit(1);
		}
	}

	public void setBlock(int x, int y, int t2, int p2, int ch2, int co2)
	{
		try
		{
			t2 &= 0xFF;
			if(t2==0xFF) setPushable(x,y,ch2,co2);
			int px = x&63;
			int py = y&63;
			//System.out.println("setBlock at chunk " + (x>>6) + "," + (y>>6) + ", pos " + px + "," + py);
			findCachedChunk((x>>6),(y>>6)).place(px,py,(byte)t2,(byte)ch2,(byte)co2,(byte)p2);
		}
		catch(NullPointerException e)
		{
			//System.out.println("setBlock: no cached chunk near player found. ODD.");
			if(!multiplayer) System.exit(1);
		}
	}
	public void setBlock(int x, int y, byte t1, byte p1, byte ch1, byte co1)
	{
		try
		{ 
			if(t1==-1) setPushable(x,y,ch1,co1);
			int px = x&63;
			int py = y&63;
			findCachedChunk((x>>6),(y>>6)).place(px,py,t1,ch1,co1,p1);
		}
		catch(NullPointerException e)
		{
			if(!multiplayer) System.exit(1);
		}
	}
	
	public void setBlockNet(int x, int y, byte t1, byte ch1, byte co1)
	{
		try
		{ 
			int px = x&63;
			int py = y&63;
			//findCachedChunk((x>>6),(y>>6)).place(px,py,t1,ch1,co1,(byte)0);
			se.out.writeByte(0x33);
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
	public void setPushableNet(int x, int y, byte ch1, byte co1)
	{
		setBlockNet(x,y,(byte)-1,ch1,co1);
	}
}
