import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;

public class CraftrKickScreen extends CraftrScreen
{
	public static final int GRID_W = 32;
	public static final int GRID_H = 25;
	public static final int FULLGRID_W = GRID_W+1;
	public static final int FULLGRID_H = GRID_H+1;
	public static final int WIDTH = ((FULLGRID_W-1)*16);
	public static final int HEIGHT = (FULLGRID_H*16);
	public CraftrCanvas c;
	public String name;
	public String mName;

	public CraftrKickScreen(CraftrCanvas cc, String nam)
	{
		c = cc;
		name = nam;
		mName = "KICKED!";
	}
	
	public void paint(Graphics g, int mmx, int mmy)
	{
		g.setColor(new Color(170,0,0));
		g.fillRect(0,0,WIDTH,HEIGHT);
		c.DrawString((WIDTH/2)-(mName.length()<<3),(HEIGHT/2)-16-2,mName,15+(4<<4),g);
		c.DrawString1x((WIDTH-(name.length()<<3))/2,(HEIGHT/2)+2,name,15+(4<<4),g);
	}
}
