public class CraftrCopier
{
	CraftrBlock[] paste;
	int xsize;
	int ysize;
	int used;

	public CraftrCopier()
	{
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
