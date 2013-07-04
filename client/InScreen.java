package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;

public class InScreen extends Screen
{
	public int inputMode = 0;
	public int inSel = 0;
	public Canvas c;
	public String name;
	public int mx,my;
	public String inString;
	public String[] inputStrings;
	public boolean isRunning = true;
	public int maxLen=48;
	public int minLen=0;
	public ArrayList<Window> windows;

	public InScreen(Canvas cc, int inpMode, String nam)
	{
		c = cc;
		windows = new ArrayList<Window>();
		name = nam;
		inputMode = inpMode;
		isRunning=true;
		inString="";
	}
	
	public void addString(String str)
	{
		inString=str;
	}
	
	public void addStrings(String[] ins)
	{
		inputStrings=ins;
	}
	
	public int step = 0;

	public boolean insideRect(int mx, int my, int x, int y, int w, int h)
	{
		if(mx >= x && my >= y && mx < x+w && my < y+h)
		{
			return true;
		} else
		{
			return false;
		}
	}

	public boolean obstructedWindow(Window w, int mx, int my)
	{
		synchronized(windows)
		{
			for(int wi = windows.size()-1;wi>windows.indexOf(w);wi--)
			{
				Window cw = windows.get(wi);
				if(insideRect(mx,my,cw.x<<3,cw.y<<3,cw.w<<3,cw.h<<3)) return true;
			}
		}
		return false;
	}

	public boolean inWindow(int x, int y)
	{
		synchronized(windows)
		{
			for(Window cw : windows)
				if(insideRect(mx,my,cw.x<<3,cw.y<<3,cw.w<<3,cw.h<<3)) return true;
		}
		return false;
	}

	public Window getWindow(int type)
	{
		synchronized(windows)
		{
			for(Window cw: windows)
			{
				if(cw.type==type) return cw;
			}
		}
		return null;
	}

	public void toggleWindow(int type)
	{
		synchronized(windows)
		{
			int app = -1;
			for(Window cw : windows)
			{
				if(cw.type == type) app = windows.indexOf(cw);
			}
			if(app>=0) windows.remove(app);
			else windows.add(new Window(type,4)); // UID chosen by fair dice roll. Guaranteed to be unique.
		}
	}

	public void paint(int mmx, int mmy)
	{
		mx = mmx;
		my = mmy;
		c.FillRect(0x000000,0,0,c.WIDTH,c.HEIGHT);
		int xPos=(c.WIDTH-(name.length()<<4))/2;
		switch(inputMode)
		{
			case 1:
				c.DrawString(xPos,12*16,name,15);
				xPos=(c.WIDTH-(inString.length()<<3))/2;
				String tString = inString;
				if(step == 1) tString+="_";
				step=1-step;
				c.DrawString1x(xPos,14*16,tString,7);
				break;
			case 2:
				int offset = (c.HEIGHT-30-(inputStrings.length*10))/2;
				c.DrawString(xPos,offset,name,15);
				for(int i=0;i<inputStrings.length;i++)
				{
					int j = 15;
					if(inSel==i) j=(j<<4);
					xPos=(c.WIDTH-(inputStrings[i].length()<<3))/2;
					c.DrawString1x(xPos,offset+30+(i*10),inputStrings[i],j);
				}
				break;
		}
		synchronized(windows)
		{
			for(Window cw : windows)
				cw.render(c);
		}
	}
	
	public void parseKey(KeyEvent ev)
	{
		int kc = ev.getKeyCode();
		switch(inputMode)
		{
			case 2:
				switch(kc)
				{
					case KeyEvent.VK_W:
					case KeyEvent.VK_UP:
						inSel--;
						if(inSel<0) inSel=inputStrings.length-1;
						break;
					case KeyEvent.VK_S:
					case KeyEvent.VK_DOWN:
						inSel++;
						if(inSel>=inputStrings.length) inSel=0;
						break;
					case KeyEvent.VK_ENTER:
						isRunning = false;
						break;
				}
				break;
			case 1:
				char kch = ev.getKeyChar();
				switch(kc)
				{
					case KeyEvent.VK_ENTER:
						if(inString.length() >= minLen && inString.length() <= maxLen) isRunning=false;
						break;
					case KeyEvent.VK_BACK_SPACE:
						if(inString.length() > 0) inString = inString.substring(0,inString.length()-1);
						break;
					case KeyEvent.VK_V:
						if(ev.isControlDown())
						{
							try
							{
								// Ctrl+V pressed, let's-a paste!
								Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
								if(clip.isDataFlavorAvailable(DataFlavor.stringFlavor))
								{
									inString += (String)clip.getData(DataFlavor.stringFlavor);
								}
							}
							catch(Exception e)
							{
								System.out.println("Clipboard pasting failed!");
								e.printStackTrace();
							}
						}
						break;
					case KeyEvent.VK_C:
						if(ev.isControlDown())
						{
							try
							{
								Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
								if(clip.isDataFlavorAvailable(DataFlavor.stringFlavor))
								{
									clip.setContents(new StringSelection(inString),null);
								}
							}
							catch(Exception e)
							{
								System.out.println("Clipboard copying failed!");
								e.printStackTrace();
							}
						}
						break;
				}
				if(kch>=32 && kch<=127 && !ev.isControlDown() && inString.length() <= maxLen)
				{
					inString+=kch;
				}
				break;
		}
	}
}
