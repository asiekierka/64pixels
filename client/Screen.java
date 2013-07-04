package client;
import common.*;

import java.awt.*;
import java.awt.image.*;

public abstract class Screen
{
	public Canvas c;
	public Screen(Canvas cc)
	{
		c = cc;
	}
	public Screen()
	{
	
	}
	public abstract void paint(int mmx, int mmy);
	public void setCanvas(Canvas canvas)
	{
		c=canvas;
	}
}
