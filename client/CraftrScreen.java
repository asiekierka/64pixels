package client;
import common.*;

import java.awt.*;
import java.awt.image.*;

public abstract class CraftrScreen
{
	public static final int GRID_W = 32;
	public static final int GRID_H = 25;
	public static final int FULLGRID_W = GRID_W+1;
	public static final int FULLGRID_H = GRID_H+1;
	public static final int WIDTH = ((FULLGRID_W-1)*16);
	public static final int HEIGHT = (FULLGRID_H*16)+8;
	public CraftrCanvas c;
	public CraftrScreen(CraftrCanvas cc)
	{
		c = cc;
	}
	public CraftrScreen()
	{
	
	}
	public abstract void paint(int mmx, int mmy);
}
