package client;
import common.*;

public abstract class CGObject
{
	public int priority;
	public int x;
	public int y;
	public int width;
	public int height;
	public CGCanvas canvas;

	public CGObject(CGCanvas c, int xp, int yp, int w, int h, int p)
	{
		priority=p;
		canvas=c;
		x=xp;
		y=yp;
		width=w;
		height=h;
	}

	public CGObject(CGCanvas c, int xp, int yp, int w, int h)
	{
		this(c,xp,yp,w,h,2);
	}

	public abstract void draw();
}