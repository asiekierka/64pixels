package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public abstract class Screen
{
	public boolean isRunning = true;
	public Canvas c;
	public Screen(Canvas cc)
	{
		c = cc;
	}
	public Screen()
	{
	
	}
	public abstract void paint(int mmx, int mmy);
	public void parseKey(KeyEvent ev) { }
	public void setCanvas(Canvas canvas)
	{
		c=canvas;
	}
}
