package server;
import common.*;

import java.io.*;

public class Region
{
	Block[] paste;
	int xsize, ysize, used;

	public Region() { }

	public String load(String filename)
	{
		DataInputStream in=null;
		FileInputStream fin=null;
		try
		{
			fin = new FileInputStream(filename + ".6cf");
			in = new DataInputStream(fin);
			int version = in.readUnsignedByte(); // to be sure
			if(version == 1)
			{
				xsize = in.readInt();
				ysize = in.readInt();
				int blockDataSize = in.readUnsignedByte();
				paste = new Block[xsize*ysize];
				for(int iy=0; iy<ysize; iy++)
				{
					for(int ix=0;ix<xsize; ix++)
					{
						byte[] blockData = new byte[blockDataSize];
						in.read(blockData,0,blockDataSize);
						paste[(iy*xsize)+ix] = new Block(ix,iy,blockData);
					}
				}
				used = 1;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			used = 0;
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
		if(used != 1) return;
		FileOutputStream fout = null;
		DataOutputStream out = null;
		try
		{
			fout = new FileOutputStream(filename + ".6cf");
			out = new DataOutputStream(fout);
			out.writeByte(1);
			out.writeInt(xsize);
			out.writeInt(ysize);
			out.writeByte(Block.getBDSize());
			for(int iy=0; iy<ysize; iy++)
			{
				for(int ix=0;ix<xsize; ix++)
				{
					byte[] blockData = paste[(iy*xsize)+ix].getBlockData();
					out.write(blockData, 0, blockData.length);
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
	
	public void copy(WorldMap map, int startx, int starty, int xs, int ys)
	{
		if(xs>160 || ys>160) return;
		xsize=xs;
		ysize=ys;
		paste = new Block[xsize*ysize];
		for(int yp=0;yp<ysize;yp++)
		{
			for(int xp=0;xp<xsize;xp++)
			{
				paste[(yp*xsize)+xp] = map.getBlock(startx+xp,starty+yp);
			}
		}
		used=1;
	}
	public void paste(WorldMap map, int xpos, int ypos)
	{
		if(used==0) return;
		for(int yp=0;yp<ysize;yp++)
		{
			for(int xp=0;xp<xsize;xp++)
			{
				byte[] data = paste[(yp*xsize)+xp].getBlockData();
				map.setBlock(xpos+xp,ypos+yp,data);
				map.setBlockNet(xpos+xp,ypos+yp,data[0],data[2],data[3]);
				if(data[5]!=0)
				{
					map.setPushable(xpos+xp,ypos+yp,data[4],data[5]);
					map.setBlockNet(xpos+xp,ypos+yp,(byte)-1,data[4],data[5]);
				}
			}
		}
	}
}
