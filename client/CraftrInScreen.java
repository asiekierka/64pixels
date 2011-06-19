package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;

public class CraftrInScreen extends CraftrScreen
{
	public int inputMode = 0;
	public int inSel = 0;
	public CraftrCanvas c;
	public String name;
	public int mx,my;
	public String inString;
	public String[] inputStrings;
	public boolean isRunning = true;
	public int maxLen=48;
	public int minLen=0;

	public CraftrInScreen(CraftrCanvas cc, int inpMode, String nam)
	{
		c = cc;
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
	
	public void paint(Graphics g, int mmx, int mmy)
	{
		mx = mmx;
		my = mmy;
		g.setColor(new Color(0,0,0));
		g.fillRect(0,0,WIDTH,HEIGHT);
		int xPos=(WIDTH-(name.length()<<4))/2;
		switch(inputMode)
		{
			case 1:
				c.DrawString(xPos,12*16,name,15,g);
				xPos=(WIDTH-(inString.length()<<3))/2;
				String tString = inString;
				if(step == 1) tString+="_";
				step=1-step;
				c.DrawString1x(xPos,14*16,tString,7,g);
				break;
			case 2:
				int offset = (HEIGHT-30-(inputStrings.length*10))/2;
				c.DrawString(xPos,offset,name,15,g);
				for(int i=0;i<inputStrings.length;i++)
				{
					int j = 15;
					if(inSel==i) j=(j<<4);
					xPos=(WIDTH-(inputStrings[i].length()<<3))/2;
					c.DrawString1x(xPos,offset+30+(i*10),inputStrings[i],j,g);
				}
				break;
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
