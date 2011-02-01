public class CraftrCopier
{
	byte[] paste = new byte[128*128*6];
	int xsize;
	int ysize;
	int used;

	public CraftrCopier()
	{
	}

	public void copy(CraftrMap map, int startx, int starty, int xs, int ys)
	{
		if(xs>128 || ys>128) return;
		xsize=xs;
		ysize=ys;
		for(int yp=0;yp<ys;yp++)
		{
			for(int xp=0;xp<xs;xp++)
			{
				byte[] t = map.getBlock(startx+xp,starty+yp);
				System.arraycopy(t,0,paste,(((yp*128)+xp)*6),6);
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
				byte[] t = new byte[6];
				System.arraycopy(paste,(((yp*128)+xp)*6),t,0,6);
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
