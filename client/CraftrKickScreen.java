package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;

public class CraftrKickScreen extends CraftrScreen
{
	public CraftrCanvas c;
	public String name;
	public String mName;
	public int bgcolor = 0xAA0000;
	public CraftrKickScreen(CraftrCanvas cc, String nam)
	{
		c = cc;
		name = nam;
		mName = "KICKED!";
	}
	
	public void paint(int mmx, int mmy)
	{
		c.FillRect(bgcolor,0,0,c.WIDTH,c.HEIGHT);
		c.DrawString((c.WIDTH/2)-(mName.length()<<3),(c.HEIGHT/2)-16-2,mName,15);
		c.DrawString1x((c.WIDTH-(name.length()<<3))/2,(c.HEIGHT/2)+2,name,15);
	}
}
