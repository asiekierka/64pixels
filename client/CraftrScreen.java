package client;
import common.*;

import java.awt.*;
import java.awt.image.*;

public abstract class CraftrScreen
{
	public CraftrCanvas c;
	public CraftrScreen(CraftrCanvas cc)
	{
		c = cc;
	}
	public CraftrScreen()
	{
	
	}
	public abstract void paint(int mmx, int mmy);
	public void setCanvas(CraftrCanvas canvas)
	{
		c=canvas;
	}
}
