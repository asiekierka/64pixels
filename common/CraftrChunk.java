package common;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class CraftrChunk {

	public byte[] type;
	public byte[] param;
	public byte[] chr;
	public byte[] col;
	public byte[] chrPushable;
	public byte[] colPushable;
	public byte[] chrDisplay;
	public byte[] colDisplay;
	public byte[] bulletParam;
	public ArrayList<CraftrExtendedBlock> extendedBlocks;
	public int w;
	public int h;
	public int xpos;
	public int ypos;
	public int spawnX = 0;
	public int spawnY = 0;
	public boolean isUsed;
	public boolean isSet;
	public boolean isReUsed;
	public static final int hdrsize = 2;
	public static final int LATEST_CHUNK_VERSION = 6;

	public CraftrChunk(int xp, int yp, boolean used)
	{
		type = new byte[64*64];
		param = new byte[64*64*2];
		chr = new byte[64*64*2];
		col = new byte[64*64*2];
		chrDisplay = new byte[64*64];
		colDisplay = new byte[64*64];
		chrPushable = new byte[64*64];
		colPushable = new byte[64*64];
		bulletParam = new byte[64*64];
		extendedBlocks = new ArrayList<CraftrExtendedBlock>();
		w = 64;
		h = 64;
		xpos = xp;
		ypos = yp;
		isUsed = used;
		isReUsed = false;
		isSet = true;
	}

	public CraftrExtendedBlock getExtendedBlock(int x, int y) {
		for(CraftrExtendedBlock eb: extendedBlocks) {
			if(eb.getX()==x && eb.getY()==y) return eb;
		}
		return null;
	}

	public void setExtendedBlock(int x, int y, CraftrExtendedBlock block) {
		int replaceIndex = -1;
		for(CraftrExtendedBlock eb: extendedBlocks) {
			if(eb.getX()==x && eb.getY()==y) {
				replaceIndex = extendedBlocks.indexOf(eb);
			}
		}
		if(replaceIndex >= 0) {
			extendedBlocks.set(replaceIndex, block);
		} else extendedBlocks.add(block);
	}
	public byte[] readByteArray(InputStream is, int length) {
		byte[] array = new byte[length];
		int i = 0;
		try {
			while(i<length && i>-1) i += is.read(array, i, array.length-i);
		}
		catch(Exception e) {
			System.out.println("[CHUNK] ReadByteArray: " + e.getMessage() + " ("+i+"/"+length+" bytes read)");
		}
		return array;
	}
	public void readChunk(InputStream in, CraftrMap map) {
		GZIPInputStream gin = null;
		DataInputStream din = null;
		try
		{
			gin = new GZIPInputStream(in);
			din = new DataInputStream(gin);
			int version = din.readUnsignedByte();
			spawnX = din.readUnsignedByte();
			spawnY = din.readUnsignedByte();
			switch(version)
			{
				case LATEST_CHUNK_VERSION:
					type = readByteArray(gin, 4096);
					param = readByteArray(gin, 4096 * 2); // Second half stores bullet.
					chr = readByteArray(gin, 4096 * 2); // Second half stores display char & colour
					col = readByteArray(gin, 4096 * 2);
					chrPushable = readByteArray(gin, 4096);
					colPushable = readByteArray(gin, 4096);
					bulletParam = readByteArray(gin, 4096);
					for(int i=0;i<4096;i++)
					{
						// Plate refresh
						if(type[i]==5 && (0x80&(int)param[i])>0)
						{
							param[i]=(byte)1;
							map.physics.addBlockToCheck(new CraftrBlockPos(xpos*64+(i&63),ypos*64+(i>>6)));
						}
						else if(map.physics.isReloaded(type[i])) // Physics refresh
							map.physics.addBlockToCheck(new CraftrBlockPos(xpos*64+(i&63),ypos*64+(i>>6)));
						else if(param[i+4096] != 0) // Bullet refresh
							map.physics.addBlockToCheck(new CraftrBlockPos(xpos*64+(i&63),ypos*64+(i>>6)));
					}
					fixDisplay();
					/*
					int extendedBlocks = din.readUnsignedShort();
					for(int eBi = 0; eBi < extendedBlocks; eBi++) {
						int x = din.readUnsignedByte();
						int y = din.readUnsignedByte();
					}
					*/
					din.close();
					gin.close();
					return;
				default:
					System.out.println("[CHUNK] ReadChunk: unknown version: " + version);
					break;
			}
		}
		catch (Exception e)
		{
			// Something else happened!
			System.out.println("[CHUNK] ReadChunk: exception: " + e.getMessage());
			return;
		}
		finally
		{
			try
			{
				if(din != null && gin != null && in != null) {din.close(); gin.close(); in.close();}
			}
			catch (Exception e) { System.out.println("[CHUNK] ReadChunk: warning - streams did not close"); }
		}
	}
	public byte[] saveByte()
	{
		byte[] out = new byte[(4096*10)+1+hdrsize];
		System.arraycopy(type,0,out,1+hdrsize,4096);
		System.arraycopy(param,0,out,4096+1+hdrsize,8192);
		System.arraycopy(chr,0,out,(4096*3)+1+hdrsize,8192);
		System.arraycopy(col,0,out,(4096*5)+1+hdrsize,8192);
 		System.arraycopy(chrPushable,0,out,(4096*7)+1+hdrsize,4096);
 		System.arraycopy(colPushable,0,out,(4096*8)+1+hdrsize,4096);
 		System.arraycopy(bulletParam,0,out,(4096*9)+1+hdrsize,4096);
		out[0] = LATEST_CHUNK_VERSION; // version
		out[1] = (byte)spawnX;
		out[2] = (byte)spawnY;
		return out;
	}
	public byte[] saveByteNet()
	{
		return saveByte();
	}
	public void fixDisplay()
	{
		for(int i=0;i<4096;i++)
		{
 			if(colPushable[i] != 0)
 			{
 				chrDisplay[i] = chrPushable[i];
 				colDisplay[i] = colPushable[i];
 			}
 			else if(type[i] == 0)
			{
				chrDisplay[i] = chr[4096+i];
				colDisplay[i] = col[4096+i];
			}
			else
			{
				chrDisplay[i] = chr[i];
				colDisplay[i] = col[i];
			}
		}
	}

	public void generate(int mode)
	{
		Random rand = new Random();
		switch(mode)
		{
			default:
				for(int i=0;i<64;i++)
				{
					int ipx = (byte)rand.nextInt(64);
					int ipy = (byte)rand.nextInt(64);
					int ipp = ipx+(ipy<<6);
					chr[ipp] = (byte)rand.nextInt(256);
					col[ipp] = (byte)rand.nextInt(256);
					type[ipp] = (byte)1;
				}
				break;
		}
		fixDisplay();
	}
	public static int index(int x, int y)
	{
		return x+(y<<6);
	}
	public byte[] getBlock(int x, int y)
	{
		byte[] data = new byte[8];
		int p = x+(y<<6);
		data[0] = type[p];
		data[1] = param[p];
		data[2] = getBlockChar(x,y);
		data[3] = getBlockColor(x,y);
		data[4] = chrPushable[p];
		data[5] = colPushable[p];
		data[6] = getBullet(x,y);
		data[7] = bulletParam[p];
		return data;
	}
	public byte getBlockType(int x, int y)
	{
		return type[x+(y<<6)];
	}
	public byte getBlockChar(int x, int y)
	{
		if(type[x+(y<<6)] == 0) return chr[4096+x+(y<<6)];
		else return chr[x+(y<<6)];
	}
	public byte getBlockColor(int x, int y)
	{
		if(type[x+(y<<6)] == 0) return col[4096+x+(y<<6)];
		else return col[x+(y<<6)];
	}
	public byte getBlockParam(int x, int y)
	{
		return param[x+(y<<6)];
	}
	public void setBlockParam(int x, int y, byte val)
	{
		param[x+(y<<6)]=val;
	}
	public byte getPushableChar(int x, int y)
 	{
 		return chrPushable[x+(y<<6)];
 	}
 	public byte getPushableColor(int x, int y)
 	{
 		return colPushable[x+(y<<6)];
 	}
	public byte getBullet(int x, int y)
	{
		return param[4096+x+(y<<6)];
	}
	public byte getBulletParam(int x, int y)
	{
		return bulletParam[x+(y<<6)];
	}		
	public void placeBullet(int x, int y, byte aType)
	{
		placeBullet(x,y,aType,(byte)0);
	}	
	public void placeBullet(int x, int y, byte aType, byte aPar)
	{
		param[4096+x+(y<<6)]=aType;
		bulletParam[x+(y<<6)]=aPar;
	}
	public void place(int x, int y, byte aType, byte aChr, byte aCol, byte aPar)
	{
		int tmp = x+(y<<6);
		type[tmp]=aType;
		param[tmp] = aPar;
		if(aType!=0)
		{
			chr[tmp]=aChr;
			col[tmp]=aCol;
		} else {
			chr[4096+tmp]=aChr;
			col[4096+tmp]=aCol;
		}
 		if(colPushable[tmp] == 0)
 		{
 			chrDisplay[tmp] = aChr;
 			colDisplay[tmp] = aCol;
		}
	}
 	public void placePushable(int x, int y, byte aChr, byte aCol)
 	{
 		int tmp = x+(y<<6);
 		chrPushable[tmp] = aChr;
 		colPushable[tmp] = aCol;
 		if(aCol == 0)
 		{
 			if(type[tmp] != 0)
 			{
 				chrDisplay[tmp] = chr[tmp];
 				colDisplay[tmp] = col[tmp];
 			} else {
 				chrDisplay[tmp] = chr[4096+tmp];
 				colDisplay[tmp] = col[4096+tmp];
 			}
 		} else {
 			chrDisplay[tmp] = aChr;
 			colDisplay[tmp] = aCol;
 		}
	}

	public void clear(int x, int y)
	{
 		int tmp = x+(y<<6);
		type[tmp]=0;
	}
	
	public void place(int x, int y, byte aType, byte aChr, byte aCol)
	{
		place(x,y,aType,aChr,aCol,(byte)0);
	}
	
	public static String getFilename(int x, int y)
	{
		return y + "/chunk" + x + ".cnk";
	}
}
