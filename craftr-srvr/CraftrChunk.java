import java.util.*;

public class CraftrChunk {

	public byte[] type;
	public byte[] param;
	public byte[] chr;
	public byte[] col;
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
		System.arraycopy(rawdata,hdrsize,type,0,4096);
		System.arraycopy(rawdata,hdrsize+4096,param,0,8192);
		System.arraycopy(rawdata,hdrsize+(4096*3),chr,0,8192);
		System.arraycopy(rawdata,hdrsize+(4096*5),col,0,8192);
		byte[] tmp = new byte[2];
		System.arraycopy(rawdata,3,tmp,0,2);
		mapinfo_len = CraftrConvert.arrShort(tmp);
		mapinfo_type = rawdata[2];
		if(mapinfo_len > 0 && mapinfo_type > 0) System.arraycopy(rawdata,hdrsize+(4096*7),mapinfo,0,mapinfo_len);
		spawnX = rawdata[0];
		spawnY = rawdata[1];
		fixDisplay();
	}

	public void loadByteNet(byte[] rawdata)
	{
		// i love arraycopy, and all its mysteries!
		// boom de yada, boom de yada...
		System.arraycopy(rawdata,1,type,0,4096);
		System.arraycopy(rawdata,1+4096,chr,0,8192);
		System.arraycopy(rawdata,1+(4096*3),col,0,8192);
		fixDisplay();
		byte[] tmp = new byte[2];
		System.arraycopy(rawdata,(4096*5)+1,tmp,0,2);
		short amount = CraftrConvert.arrShort(tmp);
		for(int i=0;i<amount;i++)
		{
			int ip = (4096*5)+3+(i*3);
			setBlockParam(rawdata[ip],rawdata[ip+1],rawdata[ip+2]);
		}
	}

	public byte[] saveByte()
	{
		byte[] out = new byte[(4096*8)+1+hdrsize];
		System.arraycopy(type,0,out,1+hdrsize,4096);
		System.arraycopy(param,0,out,4096+1+hdrsize,8192);
		System.arraycopy(chr,0,out,(4096*3)+1+hdrsize,8192);
		System.arraycopy(col,0,out,(4096*5)+1+hdrsize,8192);
		System.arraycopy(mapinfo,0,out,(4096*7)+1+hdrsize,4096);
		out[0] = 3; // version
		out[1] = (byte)spawnX;
		out[2] = (byte)spawnY;
		out[3] = (byte)mapinfo_type;
		byte[] tmp = CraftrConvert.shortArray((short)mapinfo_len);
		System.arraycopy(tmp,0,out,4,2);
		return out;
	}
	public byte[] saveByteNet()
	{
		byte[] out = new byte[(4096*8)+3];
		System.arraycopy(type,0,out,1,4096);
		System.arraycopy(chr,0,out,4096+1,8192);
		System.arraycopy(col,0,out,(4096*3)+1,8192);
		int ts=4096*5+3;
		int parame = 0;
		for(int i=0;i<4096;i++)
		{
			if(type[i]==6 && (0x80&(int)param[i])>0)
			{
				out[ts+parame]=(byte)(i&63);
				out[ts+parame+1]=(byte)(i>>6);
				out[ts+parame+2]=(byte)0x80;
				parame+=3;
			}
		}
		if(parame>0)
		{
			byte[] tmp = CraftrConvert.shortArray((short)(parame/3));
			System.arraycopy(tmp,0,out,4096*5+1,2);
		}
		byte[] out2 = new byte[(4096*5)+3+parame];
		System.arraycopy(out,0,out2,0,(4096*5)+3+parame);
		out2[0] = 2; // version
		return out2;
	}
	public void fixDisplay()
	{
		for(int i=0;i<4096;i++)
		{
			if(type[i] == 0)
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
		if(type[x+(y<<6)] == 0) return param[4096+x+(y<<6)];
		else return param[x+(y<<6)];
	}
	public void setBlockParam(int x, int y, byte val)
	{
		if(type[x+(y<<6)] == 0) param[4096+x+(y<<6)]=val;
		else param[x+(y<<6)]=val;
	}
	public int getBlockFullType(int x, int y)
	{
		if(type[x+(y<<6)] == 0) return (param[4096+x+(y<<6)]<<8)|type[4096+x+(y<<6)];
		else return (param[x+(y<<6)]<<8)|type[x+(y<<6)];
	}
	public void place(int x, int y, byte aType, byte aChr, byte aCol, byte aPar)
	{
		int tmp = x+(y<<6);
		type[tmp]=aType;
		if(aType>0)
		{
			chr[tmp]=aChr;
			col[tmp]=aCol;
			param[tmp] = aPar;
		} else {
			chr[4096+tmp]=aChr;
			col[4096+tmp]=aCol;
			param[4096+tmp] = aPar;
		}
		chr2[tmp] = aChr;
		col2[tmp] = aCol;
	}
	
	public void placeTypeOnly(int x, int y, byte val)
	{
		type[x+(y<<6)]=val;
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