package common;

import java.util.*;

public class DungeonGenerator extends MapGenerator
{
	private byte[] types;
	private int threshold;
	public int maxarea = 100;
	private int cover;
	private int w;
	private int h;

	public DungeonGenerator()
	{
	}

	public static String getError(int val)
	{
		switch(val)
		{
			case -1:
				return "Map size too small, must be at least 32x32!";
			default:
				return "Unknown error!";
		}

	}
	public boolean generate(WorldMap map, int width, int height)
	{
		if(width<32 || height<32) return false;
		w=width;
		h=height;
		types = new byte[width*height];
		threshold = (width*height*2)/5;
		cover = 0;
		drawrect((width/2)-3,(height/2)-3,(width/2)+3,(height/2)+3);
		scrawl();
		trim();
		putOnMap(map);
		return true;
	}

	public void putOnMap(WorldMap map)
	{
		int cx = w/2;
		int cy = h/2;
		for(int y=0;y<h;y++)
			for(int x=0;x<w;x++)
			{
				if(types[ind(x,y)]!=(byte)0) map.setBlock(x-cx,y-cy,(byte)0,(byte)0,(byte)0,(byte)0);
				else map.setBlock(x-cx,y-cy,(byte)1,(byte)0,(byte)177,(byte)0x87);
			}
	}

	public boolean checkZero(int x, int y)
	{
		if(x<0 || y<0 || x>=w || y>=h) return false;	
		return types[(y*w)+x]!=(byte)0;
	}
	public int ind(int x,int y)
	{
		int ty = y;
		int tx = x;
		if(tx<0) tx=0;
		if(ty<0) ty=0;
		if(tx>=w) tx=w-1;
		if(ty>=h) ty=h-1;	
		return (ty*w)+tx;
	}
	public void trim()
	{
		for(int y=0;y<h;y++)
			for(int x=0;x<w;x++)
				if(types[ind(x,y)]==(byte)0 && ((checkZero(x,y-1) && checkZero(x,y+1)) || (checkZero(x-1,y) && checkZero(x+1,y)))) 
					types[ind(x,y)]=(byte)1;
	}
	public Random rand = new Random();
	public void scrawl()
	{
		while(cover<threshold)
		{
			int x1 = rand.nextInt(w-2)+1;
			int x2 = rand.nextInt(w-2)+1;
			int y1 = rand.nextInt(h-2)+1;
			int y2 = rand.nextInt(h-2)+1;
			if(x1>x2)
			{
				int t=x1;
				x1=x2;
				x2=t;
			}
			if(y1>y2)
			{
				int t=y1;
				y1=y2;
				y2=t;
			}
			int ww = x2-x1+1;
			int hh = y2-y1+1;
			if(ww<=1 || hh<=1 || (ww*hh)>maxarea) continue;	
			drawrect(x1,y1,x2,y2);
		}
	}

	public void drawrect(int x1, int y1, int x2, int y2)
	{
		boolean notok;
		if(x1<=0 || x2>=w-1 || y1<=0 || y2>=h-1) return;
		if(cover!=0)
		{
			notok=true;
			for(int y=y1;y<=y2;y++)
				for(int x=x1;x<=x2;x++)
					if(types[(y*w)+x]!=(byte)0) return;
			for(int y=y1;y<=y2;y++)
				if((types[(y*w)+x1-1]!=(byte)0) || (types[(y*w)+x2+1]!=(byte)0))
				{
					notok=false;
					break;
				}
			if(notok)
			{
				for(int x=x1;x<=x2;x++)
					if((types[((y1-1)*w)+x]!=(byte)0) || (types[((y2+1)*w)+x]!=(byte)0))
					{
						notok=false;
						break;
					}
			}
			if(notok) return;
		}
		for(int y=y1;y<=y2;y++)
			for(int x=x1;x<=x2;x++)
				if(types[(y*w)+x]==(byte)0)
				{
					types[(y*w)+x]=(byte)1;
					cover++;
				}
	}
}
