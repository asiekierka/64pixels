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
	public String mapName = "map";
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
		this(_isServer,_cachesize,multiMode,"map");
	}

	public CraftrMap(boolean _isServer, int _cachesize, String name)
	{
		this(_isServer,_cachesize,false,name);
	}

	public CraftrMap(boolean _isServer, int _cachesize, boolean multiMode, String name)
	{
		mapName=name;
		isServer = _isServer;
		System.out.println("[MAP] Initializing '" + name + "'...");
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
	
	public void kill(int id)
	{
		if(isServer)
			synchronized(se)
			{
				se.kill(id);
			}
		else if (id==255)
			game.kill();
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
		for (int i=0;i<cachesize;i++) if(!chunks[i].isSet) { chunks[i] = new CraftrChunk(x,y,true); chunks[i].isUsed = true; readChunkFile(x,y,chunks[i]); return chunks[i]; }
		
		// No, is there any chunk that isn't in the closest area of players'?
		for (int i=0;i<cachesize;i++) if(!chunks[i].isUsed) { chunks[i] = new CraftrChunk(x,y,true); chunks[i].isUsed = true; readChunkFile(x,y,chunks[i]); return chunks[i]; }
		
		if(isServer)
		{
			// Use up the first chunk you see 
			int i = randc.nextInt(cachesize);
			saveChunkFile(i);
			chunks[i] = new CraftrChunk(x,y,true);
			chunks[i].isUsed = true;
			readChunkFile(x,y,chunks[i]);
			return chunks[i];
		} else {
			// Are you kidding me!? 9 chunks are (should be) used and you can't find a free space in SIXTY-FOUR!?
			// I throw an exception right now.
			throw new NoChunkMemException(x,y);
		}
	}

	public void wipeChunks()
	{
		for (int i=0;i<cachesize;i++)
		{
			chunks[i].isSet=false;
			chunks[i].isUsed=false;
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
		for (int i=0;i<cachesize;i++) if(chunks[i].xpos == x && chunks[i].ypos == y && chunks[i].isSet == true) return i;
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
		CraftrChunk out = new CraftrChunk(cx,cy,used);
		out.generate(genMode);
		return out;
	}
	
	// LOADING/SAVING - UTILITIES
	public void checkDirs(int x, int y)
	{
		File tmf = new File(saveDir + mapName);
		if(!tmf.exists()) tmf.mkdir();
		tmf = new File(saveDir + mapName + "/" + y);
		if(!tmf.exists()) tmf.mkdir();
	}
	
	// LOADING
	public void readChunkFile(int x, int y, CraftrChunk target)
	{
		if(multiplayer)
		{
			System.out.println("[MAP] Chunk request: " + x + ", " + y);
			if(!isServer)
				net.chunkRequest(x,y);
			CraftrChunk nout = new CraftrChunk(x,y,true);
			target = nout;
		}
		else
		{
			try {
				FileInputStream in = new FileInputStream(saveDir + mapName + "/" + CraftrChunk.getFilename(x,y));
				target.readChunk(in, this);
			}
			catch (FileNotFoundException e)
			{
				System.out.println("GEN");
				target.generate(genMode);
				target.isSet = true;
				saveChunkFile(x,y,target);
			}
			catch (Exception e)
			{
				// Something else happened!
				System.out.println("[MAP] ReadChunkFile: exception: " + e.getMessage());
			}
		}
	}
	
	// SAVING
	public void saveChunkFile(int cid)
	{
		CraftrChunk cin = chunks[cid];
		if (cin != null)
		{
			saveChunkFile(cin.xpos,cin.ypos);
		}
	}
	public void saveChunkFile(int x, int y)
	{
		CraftrChunk cin = findCachedChunk(x,y);
		if (cin != null)
		{
			saveChunkFile(x,y,cin);
		}
	}
	public void saveChunkFile(int x, int y, CraftrChunk chunk)
	{
		if(multiplayer) return;
		byte[] data = chunk.saveByte();
		FileOutputStream fout = null;
		GZIPOutputStream gout = null;
		try
		{
			checkDirs(x,y);
			fout = new FileOutputStream(saveDir + mapName + "/" + CraftrChunk.getFilename(x,y));
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
		try
		{ 
			return new CraftrBlock(x,y,grabChunk((x>>6),(y>>6)).getBlock(x&63,y&63));
		}
		catch(NoChunkMemException e)
		{
			System.out.println("getBlock: exception: no chunk memory found. Odd...");
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
 			grabChunk((x>>6),(y>>6)).placePushable(px,py,aChr,aCol);
 		}
		catch(Exception e)
		{
 			System.out.println("setPushable: exception!");
			e.printStackTrace();
 			if(!multiplayer) System.exit(1);
		}
	}
 	public void setBullet(int x, int y, byte aType)
	{
		setBullet(x,y,aType,(byte)0);
	}
 	public void setBullet(int x, int y, byte aType, byte aPar)
 	{
 		try
 		{ 
 			int px = x&63;
 			int py = y&63;
 			grabChunk((x>>6),(y>>6)).placeBullet(px,py,aType, aPar);
 		}
		catch(Exception e)
		{
 			System.out.println("setBullet: exception!");
			e.printStackTrace();
 			if(!multiplayer) System.exit(1);
		}
	}
	public void setBulletNet(int x, int y, byte aType, byte aPar)
	{
		setBulletNet(x,y,aType);
	}
	public void setBulletNet(int x, int y, byte aType)
	{
		try
		{
			se.out.writeByte(0x70);
			se.out.writeInt(x);
			se.out.writeInt(y);
			se.out.writeByte(aType);
			byte[] t = se.getPacket();
			se.sendAllOnMap(t,t.length,mapName);
		}
		catch(Exception e) { System.out.println("setBulletNet sending error!"); }
	}
	
	public void setPlayerNet(int x, int y, int on)
	{
		try
		{
			se.out.writeByte(0x2A|on);
			se.out.writeInt(x);
			se.out.writeInt(y);
			byte[] t = se.getPacket();
			se.sendAllOnMap(t,t.length,mapName);
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
	
	public void pushMultiple(int x, int y, int xs, int ys, int dx, int dy)
	{
		pushMultiple(x,y,xs,ys,dx,dy,false);
	}

	// this was tricky to merge, so it might still be a bit buggy --GM
	public void pushMultiple(int x, int y, int xsize, int ysize, int dx, int dy, boolean pull)
	{
		synchronized(this)
		{
			if(isServer)
			{
				try
				{
					if(pull) se.out.writeByte(0xE2);
					else se.out.writeByte(0xE1);
					se.out.writeInt(x);
					se.out.writeInt(y);
					se.out.writeShort(xsize);
					se.out.writeShort(ysize);
					se.out.writeByte(dx);
					se.out.writeByte(dy);
					byte[] t = se.getPacket();
					se.sendAllOnMap(t,t.length,mapName);
				}
				catch(Exception e){ System.out.println("Failed to send pushMultiple packet!"); }
			}
			CraftrBlock[] blocks = new CraftrBlock[xsize*ysize];
			for(int ty=0;ty<ysize;ty++)
			{
				int iy = ty*dy;
				for(int tx=0;tx<xsize;tx++)
				{
					int ix = tx*dx;
					blocks[(ty*xsize)+tx] = getBlock(x+ix,y+iy);
					setBlock(x+ix,y+iy,(byte)0,(byte)0,(byte)0,(byte)0);
					setPushable(x+ix,y+iy,(byte)0,(byte)0);
					setPlayer(x+ix,y+iy,0);
				}
			}
			for(int ty=0;ty<ysize;ty++)
			{
				int iy = ty*dy;
				for(int tx=0;tx<xsize;tx++)
				{
					int ix = tx*dx;
					int arrayPos = (ty*xsize)+tx;
					int ox = x+ix;
					int oy = y+iy;
					if(pull)
					{
						ox-=dx;
						oy-=dy;
					} else {
						ox+=dx;
						oy+=dy;
					}
					if(blocks[arrayPos].isPushable()) setPushable(ox,oy,
		                                                          (byte)blocks[arrayPos].getChar(),
		                                                          (byte)blocks[arrayPos].getColor());
					if(blocks[arrayPos].isPlaceable())
					{
						setBlock(ox,oy,blocks[arrayPos].getBlockData());
						setPlayer(ox,oy,1);
					}
					for(int moveDir=0;moveDir<4;moveDir++)
					{
						physics.addBlockToCheck(new CraftrBlockPos(ox+xMovement[moveDir],oy+yMovement[moveDir]));
					}
				}
			}
		}
	}

	public void tryPushM(int x, int y, int dx, int dy, byte chr, byte col)
	{
		if((dx!=0 && dy!=0) || (dx==0 && dy==0)) return; // don't do diagonals.
		if(dx>1 || dx<-1 || dy>1 || dy<-1) return; // no.
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
		if(!getBlock(posx,posy).isEmpty()) return;
		int tx = posx-(x+dx);
		int ty = posy-(y+dy);
		if(tx<0) tx=-tx;
		if(ty<0) ty=-ty;
		if(tx==0) tx=1;
		if(ty==0) ty=1;
		pushMultiple(x+dx,y+dy,tx,ty,dx,dy);
		setPushable(x+dx,y+dy,chr,col);
		if(isServer)
			setPushableNet(x+dx,y+dy,chr,col);
	}

	public boolean piston(int x, int y, int dx, int dy, boolean pull)
	{
		if((dx!=0 && dy!=0) || (dx==0 && dy==0)) return false; // don't do diagonals.
		if(dx>1 || dx<-1 || dy>1 || dy<-1) return false; // no.
		if(!getBlock(x+dx,y+dy).isPistonable()) return false;
		int posx = x+dx;
		int posy = y+dy;
		// we'll have to push unless we see a wall and until we have pushiums
		while(getBlock(posx,posy).isPistonable())
		{
			posx+=dx;
			posy+=dy;
		}
		if(!getBlock(posx,posy).isPistonEmpty()) return false;
		int tx = posx-(x+dx);
		int ty = posy-(y+dy);
		if(tx<0) tx=-tx;
		if(ty<0) ty=-ty;
		if(tx==0) tx=1;
		if(ty==0) ty=1;
		pushMultiple(x+dx,y+dy,tx,ty,dx,dy,pull);
		return true;
	}

	public void playSample(int x, int y, int id)
	{
		playSound(x,y,id+256);
	}
	public void playSound(int x, int y, int id)
	{
		if(isServer)
		{
			se.playSound(x,y,id,this);
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
		if(block.getType()==3 && (block.getBlockChar()<24 || block.getBlockChar()>=28)) return 25; // default char for P-NANDs
		if(block.getType()!=2) return block.getBlockChar();
		CraftrBlock surroundingBlock;
		int wiriumNeighbourInfo = 0;
		for(int moveDir=0;moveDir<4;moveDir++)
		{
			surroundingBlock=getBlock(block.x+xMovement[moveDir],block.y+yMovement[moveDir]);
			if(surroundingBlock.isWiriumNeighbour(block)) wiriumNeighbourInfo|=(1<<(3-moveDir));
		}
		return wiriumChar[wiriumNeighbourInfo];
	}

	public void clearBlock(int x, int y)
	{
		try
		{ 
			int px = x&63;
			int py = y&63;
			grabChunk((x>>6),(y>>6)).clear(px, py);
		}
		catch(NoChunkMemException e)
		{
			System.out.println("Map cache too small!");
			if(!multiplayer) System.exit(1);
		}
		catch(NullPointerException e)
		{
			if(!multiplayer) System.exit(1);
		}
	}

	public void setBlock(int x, int y, byte[] data)
	{
		try
		{ 
			if(data.length>5 && data[5]!=0) setPushable(x,y,data[4],data[5]);
			int px = x&63;
			int py = y&63;
			//System.out.println("setBlock at chunk " + (x>>6) + "," + (y>>6) + ", pos " + px + "," + py);
			grabChunk((x>>6),(y>>6)).place(px,py,data[0],data[2],data[3],data[1]);
		}
		catch(NoChunkMemException e)
		{
			System.out.println("Map cache too small!");
			if(!multiplayer) System.exit(1);
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
			grabChunk((x>>6),(y>>6)).place(px,py,(byte)t2,(byte)ch2,(byte)co2,(byte)p2);
		}
		catch(NoChunkMemException e)
		{
			System.out.println("Map cache too small!");
			if(!multiplayer) System.exit(1);
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
			grabChunk((x>>6),(y>>6)).place(px,py,t1,ch1,co1,p1);
		}
		catch(NoChunkMemException e)
		{
			System.out.println("Map cache too small!");
			if(!multiplayer) System.exit(1);
		}
		catch(NullPointerException e)
		{
			if(!multiplayer) System.exit(1);
		}
	}
	public void clearBlockNet(int x, int y)
	{
		try
		{ 
			int px = x&63;
			int py = y&63;
			se.out.writeByte(0x34);
			se.out.writeInt(x);
			se.out.writeInt(y);
			byte[] t = se.getPacket();
			se.sendAllOnMap(t,t.length,mapName);
		}
		catch(Exception e)
		{
			System.out.println("[MAP] setBlockNet exception!");
			e.printStackTrace();
		}
	}
	public void setBlockNet(int x, int y, byte t1, byte ch1, byte co1)
	{
		try
		{ 
			int px = x&63;
			int py = y&63;
			se.out.writeByte(0x33);
			se.out.writeByte((byte)255);
			se.out.writeInt(x);
			se.out.writeInt(y);
			se.out.writeByte(t1);
			se.out.writeByte(ch1);
			se.out.writeByte(co1);
			byte[] t = se.getPacket();
			se.sendAllOnMap(t,t.length,mapName);
		}
		catch(Exception e)
		{
			System.out.println("[MAP] setBlockNet exception!");
			e.printStackTrace();
		}
	}
	public void setPushableNet(int x, int y, byte ch1, byte co1)
	{
		setBlockNet(x,y,(byte)-1,ch1,co1);
	}
}
