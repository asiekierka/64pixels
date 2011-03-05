package common;

import java.util.*;

public class CraftrChunk {

	public byte[] type;
	public byte[] param;
	public byte[] chr;
	public byte[] col;
	public byte[] chrp;
	public byte[] colp;
	public byte[] chr2;
	public byte[] col2;
	public byte[] mapinfo;
	public int w;
	public int h;
	public int xpos;
	public int ypos;
	public int spawnX = 0;
	public int spawnY = 0;
	public boolean isUsed;
	public boolean isSet;
	public boolean isReUsed;
	public int mapinfo_len;
	public int mapinfo_type;
	public static final int hdrsize = 5;

	public CraftrChunk(int xp, int yp, boolean used)
	{
		type = new byte[64*64];
		param = new byte[64*64*2];
		chr = new byte[64*64*2];
		col = new byte[64*64*2];
		chr2 = new byte[64*64];
		col2 = new byte[64*64];
		chrp = new byte[64*64];
		colp = new byte[64*64];
		mapinfo = new byte[4096];
		w = 64;
		h = 64;
		xpos = xp;
		ypos = yp;
		isUsed = used;
		isReUsed = false;
		isSet = true;
	}
	
	public void loadByte(byte[] rawdata)
	{
		// i love arraycopy, and all its mysteries!
		// boom de yada, boom de yada...
		System.arraycopy(rawdata,1+hdrsize,type,0,4096);
		System.arraycopy(rawdata,1+hdrsize+4096,param,0,8192);
		System.arraycopy(rawdata,1+hdrsize+(4096*3),chr,0,8192);
		System.arraycopy(rawdata,1+hdrsize+(4096*5),col,0,8192);
 		System.arraycopy(rawdata,1+hdrsize+(4096*7),chrp,0,4096);
 		System.arraycopy(rawdata,1+hdrsize+(4096*8),colp,0,4096);
		byte[] tmp = new byte[2];
		System.arraycopy(rawdata,4,tmp,0,2);
		mapinfo_len = CraftrConvert.arrShort(tmp);
		mapinfo_type = rawdata[3];
		if(mapinfo_len > 0 && mapinfo_type > 0) System.arraycopy(rawdata,1+hdrsize+(4096*9),mapinfo,0,mapinfo_len);
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
		byte[] out = new byte[(4096*10)+1+hdrsize];
		System.arraycopy(type,0,out,1+hdrsize,4096);
		System.arraycopy(param,0,out,4096+1+hdrsize,8192);
		System.arraycopy(chr,0,out,(4096*3)+1+hdrsize,8192);
		System.arraycopy(col,0,out,(4096*5)+1+hdrsize,8192);
 		System.arraycopy(chrp,0,out,(4096*7)+1+hdrsize,4096);
 		System.arraycopy(colp,0,out,(4096*8)+1+hdrsize,4096);
		System.arraycopy(mapinfo,0,out,(4096*9)+1+hdrsize,4096);
		out[0] = 4; // version
		out[1] = (byte)spawnX;
		out[2] = (byte)spawnY;
		out[3] = (byte)mapinfo_type;
		byte[] tmp = CraftrConvert.shortArray((short)mapinfo_len);
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
 			if(colp[i] != 0)
 			{
 				chr2[i] = chrp[i];
 				col2[i] = colp[i];
 			}
 			else if(type[i] == 0)
			{
				chr2[i] = chr[4096+i];
				col2[i] = col[4096+i];
			}
			else
			{
				chr2[i] = chr[i];
				col2[i] = col[i];
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
 		return chrp[x+(y<<6)];
 	}
 	public byte getPushableColor(int x, int y)
 	{
 		return colp[x+(y<<6)];
 	}
	public byte getPushableFlags(int x, int y)
	{
		return param[4096+x+(y<<6)];
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
 		if(colp[tmp] == 0)
 		{
 			chr2[tmp] = aChr;
 			col2[tmp] = aCol;
		}
	}
 	public void placePushable(int x, int y, byte aChr, byte aCol)
	{
		placePushable(x,y,(byte)0,aChr,aCol);
	}
 	public void placePushable(int x, int y, byte aPar, byte aChr, byte aCol)
 	{
 		int tmp = x+(y<<6);
		param[4096+tmp] = aPar;
 		chrp[tmp] = aChr;
 		colp[tmp] = aCol;
 		if(aCol == 0)
 		{
 			if(type[tmp] != 0)
 			{
 				chr2[tmp] = chr[tmp];
 				col2[tmp] = col[tmp];
 			} else {
 				chr2[tmp] = chr[4096+tmp];
 				col2[tmp] = col[4096+tmp];
 			}
 		} else {
 			chr2[tmp] = aChr;
 			col2[tmp] = aCol;
 		}
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
