package common;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Chunk {

	public byte[] type;
	public byte[] param;
	public byte[] chr;
	public byte[] col;
	public byte[] chrPushable;
	public byte[] colPushable;
	public byte[] chrDisplay;
	public byte[] colDisplay;
	public byte[] bulletParam;
	public ArrayList<ExtendedBlock> extendedBlocks;
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
	public static final int LATEST_CHUNK_VERSION = 7;

	public Chunk(int xp, int yp, boolean used)
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
		extendedBlocks = new ArrayList<ExtendedBlock>();
		w = 64;
		h = 64;
		xpos = xp;
		ypos = yp;
		isUsed = used;
		isReUsed = false;
		isSet = true;
	}

	public ExtendedBlock getExtendedBlock(int x, int y) {
		for(ExtendedBlock eb: extendedBlocks) {
			if(eb.getX()==x && eb.getY()==y) return eb;
		}
		return null;
	}

	public void addExtendedBlock(ExtendedBlock block) {
		int replaceIndex = -1;
		for(ExtendedBlock eb: extendedBlocks) {
			if(eb.getX()==block.getX() && eb.getY()==block.getY()) {
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
	public void readChunk(InputStream in, WorldMap map) {
		GZIPInputStream gin = null;
		DataInputStream din = null;
		try
		{
			gin = new GZIPInputStream(in);
			din = new DataInputStream(gin);
			int version = din.readUnsignedByte();
			spawnX = din.readUnsignedByte();
			spawnY = din.readUnsignedByte();

			if(version < 3 || version > LATEST_CHUNK_VERSION)
			{
				System.out.println("[CHUNK] ReadChunk: unknown version: " + version);
				din.close();
				gin.close();
				return;
			}

			if(version >= 3 && version <= 5)
			{
				din.readUnsignedByte(); // skip map information byte
				din.readUnsignedShort(); // skip a short
			}

			if(version >= 3)
			{
				type = readByteArray(gin, 4096);
				param = readByteArray(gin, 4096 * 2); // Second half stores bullet.
				chr = readByteArray(gin, 4096 * 2); // Second half stores display char & colour
				col = readByteArray(gin, 4096 * 2);
			}
			if(version >= 4)
			{
				chrPushable = readByteArray(gin, 4096);
				colPushable = readByteArray(gin, 4096);
				bulletParam = readByteArray(gin, 4096);
				for(int i=0;i<4096;i++)
				{
					// Plate refresh
					if(type[i]==5 && (0x80&(int)param[i])>0)
					{
						param[i]=(byte)1;
						map.physics.addBlockToCheck(new Point(xpos*64+(i&63),ypos*64+(i>>6)));
					}
					else if(Block.isLoaded(type[i])) // Physics refresh
						map.physics.addBlockToCheck(new Point(xpos*64+(i&63),ypos*64+(i>>6)));
					else if(param[i+4096] != 0) // Bullet refresh
						map.physics.addBlockToCheck(new Point(xpos*64+(i&63),ypos*64+(i>>6)));
				}
				fixDisplay();
				int extendedBlockCount = din.readUnsignedShort();
				for(int eBi = 0; eBi < extendedBlockCount; eBi++) {
					int x = din.readUnsignedByte();
					int y = din.readUnsignedByte();
					int flags = (version > 6) ? din.readUnsignedByte() : 0;
					int ebLength = din.readUnsignedShort();
					byte[] data = readByteArray(gin, ebLength);
					addExtendedBlock(new ExtendedBlock(x,y,data,flags));
				}
				din.close();
				gin.close();
				return;
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
	private byte[] saveByteInternal(boolean isNetwork)
	{
		ByteArrayOutputStream baos;
		DataOutputStream out;
		try {
			baos = new ByteArrayOutputStream();
			out = new DataOutputStream(baos);
			out.writeByte(LATEST_CHUNK_VERSION);
			out.writeByte(spawnX);
			out.writeByte(spawnY);
			out.write(type,0,4096);
			out.write(param,0,8192);
			out.write(chr,0,8192);
			out.write(col,0,8192);
			out.write(chrPushable,0,4096);
			out.write(colPushable,0,4096);
			out.write(bulletParam,0,4096);
			// Extended Blocks
			if(isNetwork) {
				int netBlocksSize = 0;
				for(ExtendedBlock e: extendedBlocks)
					if(e.isNetwork()) netBlocksSize++;
				out.writeShort(netBlocksSize);
			} else out.writeShort(extendedBlocks.size());

			for(int eBi = 0; eBi < extendedBlocks.size(); eBi++) {
				ExtendedBlock eb = extendedBlocks.get(eBi);
				if(isNetwork && !eb.isNetwork()) continue;
				out.writeByte(eb.getX());
				out.writeByte(eb.getY());
				out.writeByte(eb.getFlags());
				byte[] data = eb.getData();
				out.writeShort(data.length);
				out.write(data,0,data.length);
			}
			out.flush();
			return baos.toByteArray();
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public byte[] saveByte()
	{
		return saveByteInternal(false);
	}
	public byte[] saveByteNet()
	{
		return saveByteInternal(true);
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
