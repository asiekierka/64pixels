package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;

public class TextInputScreen extends Screen
{
	public Canvas canvas;
	public String name;
	public int step;
	public String inString;
	public int maxLen=48;
	public int minLen=0;

	public TextInputScreen(Canvas cc, String _name)
	{
		c = cc;
		name = _name;
		inString="";
	}
	
	public void addString(String str)
	{
		inString=str;
	}
	
	public void paint(int mmx, int mmy)
	{
		c.FillRect(0x000000,0,0,c.WIDTH,c.HEIGHT);

		int xPos = (c.WIDTH-(name.length()<<4))/2;
		c.DrawString(xPos,12*16,name,15);

		xPos = (c.WIDTH-(inString.length()<<3))/2;
		String tString = inString;
		// Add blinking
		if(step == 1) tString+="_";
		step = 1-step;
		// Draw input string
		c.DrawString1x(xPos,14*16,tString,7);
	}
	
	public void parseKey(KeyEvent ev)
	{
		int kc = ev.getKeyCode();
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
	}
}
