package server;
import common.*;

import java.io.*;

public class CraftrCopier
{
	CraftrBlock[] paste;
	int xsize;
	int ysize;
	int used;

	public CraftrCopier()
	{
	}

	public String load(String filename)
	{
		DataInputStream in=null;
		FileInputStream fin=null;
		try
		{
			fin = new FileInputStream(filename + ".6cf");
			in = new DataInputStream(fin);
			int version = in.readUnsignedByte(); // to be sure
			if(version==1)
			{
				xsize = in.readInt();
				ysize = in.readInt();
				int blockDataSize = in.readUnsignedByte();
				paste = new CraftrBlock[xsize*ysize];
				for(int iy=0; iy<ysize; iy++)
				{
					for(int ix=0;ix<xsize; ix++)
					{
						byte[] blockd = new byte[blockDataSize];
						in.read(blockd,0,blockDataSize);
						paste[(iy*xsize)+ix]=new CraftrBlock(ix,iy,blockd);
					}
				}
				used=1;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			used=0;
		}
		finally
		{
			try
			{
				if(in!=null) in.close();
				if(fin!=null) fin.close();
			} catch(Exception e) {}
		}
		if(used == 0) return "Failed importing!";
		return "Imported!";
	}
	public void save(String filename)
	{
		if(used!=1) return;
		FileOutputStream fout = null;
		DataOutputStream out = null;
		try
		{
			fout = new FileOutputStream(filename + ".6cf");
			out = new DataOutputStream(fout);
			out.writeByte(1);
			out.writeInt(xsize);
			out.writeInt(ysize);
			out.writeByte(CraftrBlock.getBDSize());
			for(int iy=0; iy<ysize; iy++)
			{
				for(int ix=0;ix<xsize; ix++)
				{
					byte[] blockd = paste[(iy*xsize)+ix].getBlockData();
					out.write(blockd,0,blockd.length);
				}
			}
		}
		catch(Exception e)
		{
			used=0;
		}
		finally
		{
			try
			{
				if(out!=null) out.close();
				if(fout!=null) fout.close();
			} catch(Exception e) {}
		}
	}
	public void copy(CraftrMap map, int startx, int starty, int xs, int ys)
	{
		if(xs>160 || ys>160) return;
		paste = new CraftrBlock[xs*ys];
		xsize=xs;
		ysize=ys;
		for(int yp=0;yp<ysize;yp++)
		{
			for(int xp=0;xp<xsize;xp++)
			{
				paste[(yp*xsize)+xp] = map.getBlock(startx+xp,starty+yp);
			}
		}
		used=1;
	}
	public void paste(CraftrMap map, int xpos, int ypos)
	{
		if(used==0) return;
		for(int yp=0;yp<ysize;yp++)
		{
			for(int xp=0;xp<xsize;xp++)
			{
				byte[] t = paste[(yp*xsize)+xp].getBlockData();
				map.setBlock(xpos+xp,ypos+yp,t);
				map.setBlockNet(xpos+xp,ypos+yp,t[0],t[2],t[3]);
				if(t[5]!=0)
				{
					map.setPushable(xpos+xp,ypos+yp,t[4],t[5]);
					map.setBlockNet(xpos+xp,ypos+yp,(byte)-1,t[4],t[5]);
				}
			}
		}
	}
}
