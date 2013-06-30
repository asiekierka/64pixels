package common;

import java.util.*;

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
	public byte[] mapInfo;
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
	public int mapInfoLength;
	public int mapInfoType;
	public static final int hdrsize = 5;

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
		mapInfo = new byte[4096];
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

	public void loadByte(byte[] rawdata)
	{
		// i love arraycopy, and all its mysteries!
		// boom de yada, boom de yada...
		System.arraycopy(rawdata,1+hdrsize,type,0,4096);
		System.arraycopy(rawdata,1+hdrsize+4096,param,0,8192);
		System.arraycopy(rawdata,1+hdrsize+(4096*3),chr,0,8192);
		System.arraycopy(rawdata,1+hdrsize+(4096*5),col,0,8192);
 		System.arraycopy(rawdata,1+hdrsize+(4096*7),chrPushable,0,4096);
 		System.arraycopy(rawdata,1+hdrsize+(4096*8),colPushable,0,4096);
 		System.arraycopy(rawdata,1+hdrsize+(4096*9),bulletParam,0,4096);
		byte[] tmp = new byte[2];
		System.arraycopy(rawdata,4,tmp,0,2);
		mapInfoLength = CraftrConvert.arrShort(tmp);
		mapInfoType = rawdata[3];
		if(mapInfoLength > 0 && mapInfoType > 0) System.arraycopy(rawdata,1+hdrsize+(4096*10),mapInfo,0,mapInfoLength);
		spawnX = rawdata[1];
		spawnY = rawdata[2];
		fixDisplay();
	}

	public void loadByteNet(byte[] rawdata)
	{
		loadByte(rawdata);
	}

	public byte[] saveByte()
	{
		byte[] out = new byte[(4096*11)+1+hdrsize];
		System.arraycopy(type,0,out,1+hdrsize,4096);
		System.arraycopy(param,0,out,4096+1+hdrsize,8192);
		System.arraycopy(chr,0,out,(4096*3)+1+hdrsize,8192);
		System.arraycopy(col,0,out,(4096*5)+1+hdrsize,8192);
 		System.arraycopy(chrPushable,0,out,(4096*7)+1+hdrsize,4096);
 		System.arraycopy(colPushable,0,out,(4096*8)+1+hdrsize,4096);
 		System.arraycopy(bulletParam,0,out,(4096*9)+1+hdrsize,4096);
		System.arraycopy(mapInfo,0,out,(4096*10)+1+hdrsize,4096);
		out[0] = 5; // version
		out[1] = (byte)spawnX;
		out[2] = (byte)spawnY;
		out[3] = (byte)mapInfoType;
		byte[] tmp = CraftrConvert.shortArray((short)mapInfoLength);
		System.arraycopy(tmp,0,out,4,2);
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
