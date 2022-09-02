package client;
import common.*;

import java.lang.*;
import javax.swing.*;
import java.util.*;

public class Window
{
	public int type;
	public int w,h;
	public boolean isMelodium = false;
	public String title;
	public String text;
	public static int[] linechr; 
	public int x,y;
	public int charChosen, colorChosen, typeChosen;
	public int[] recBlockChr;
	public int[] recBlockCol;
	public int[] recBlockType;
	public static final String[] NOTE_NAMES={"C","C#","D","D#","E","F","F#","G","G#","A","A#","B"};
	public static final String[] DRUM_NAMES={"Kick","Snare","(C) HiHat","(O) HiHat","HiTom","MidTom","LoTom","Crash"};
	public int uid;
	private int oldtype;

	public Window(int _type, int _uid)
	{
		_uid=uid;
		type = _type;
		recBlockChr = new int[16];
		recBlockCol = new int[16];
		recBlockType = new int[16];
	}
	
	public static String getNoteName(int v)
	{
		int chr = v%248;
		if(chr>=240) return DRUM_NAMES[chr-240];
		else return NOTE_NAMES[(chr%24)>>1]+"-"+(chr/24);
	}

	public static int getBlockType(int pos)
	{
		int i = -1;
		int j = -1;
		for(;j<=255;j++)
		{
			if(Block.isPlaceable(j)) i++;
			if(i==pos) break;
		}
		return j;
	}
	public static int getBlockTypeCount()
	{
		int i = -1;
		int j = -1;
		for(;j<=255;j++)
		{
			if(Block.isPlaceable(j)) i++;
		}
		return i;
	}
	public void resize()
	{
		switch(type)
		{
			case 1: // char screen
				if(typeChosen==7) { w = 26; h=13; }
				else { w = 34; h=10; }
				title = "Choose char:";
				break;
			case 2: // color screen
				w = 18;
				h = 18;
				title = "Color:";
				break;
			case 3: // recent block screen
				w = 12;
				h = 12;
				title = "History";
				break;
			case 4: // type screen
				w = 16;
				h = getBlockTypeCount()+2+1; // Pushium
				title = "Types";
				break;
			case 5: // help screen
				w = 26;
				h = text.length()/24+5;
				title = "Help";
				break;
			default:
				w = 8;
				h = 3;
				title = "ERROR!";
				break;
		}
		if(oldtype!=type)
		{
			oldtype=type;
			x = 32-(w>>1);
			y = 25-(h>>1);
		}
	}
	
	static
	{
		linechr = new int[] { 218, 191, 192, 217, 196, 179 }; // border NW, NE, SW, SE, horiz, very
	}
	
	public void addRecBlock(byte at, byte ach, byte aco)
	{
		int t = 0xFF&(int)at;
		int ch = 0xFF&(int)ach;
		int co = 0xFF&(int)aco;
		int len=15;
		if(recBlockChr[0]==ch&&recBlockCol[0]==co&&recBlockType[0]==t)return;
		for(int i=1;i<15;i++)
		{
			if(recBlockChr[i]==ch&&recBlockCol[i]==co&&recBlockType[i]==t)
			{
				len=i;
				break;
			}
		}
		System.arraycopy(recBlockChr,0,recBlockChr,1,len);
		System.arraycopy(recBlockCol,0,recBlockCol,1,len);
		System.arraycopy(recBlockType,0,recBlockType,1,len);
		recBlockChr[0]=ch;
		recBlockCol[0]=co;
		recBlockType[0]=t;
	}
	
	public void makeWindow(Canvas cc)
	{
		for(int i=(x+1)<<3;i<=(x+w-2)<<3;i+=8)
		{
			cc.DrawChar1x(i,y<<3,(byte)linechr[4],(byte)143);
			cc.DrawChar1x(i,(y+h-1)<<3,(byte)linechr[4],(byte)143);
		}
		for(int i=(y+1)<<3;i<=(y+h-2)<<3;i+=8)
		{
			cc.DrawChar1x(x<<3,i,(byte)linechr[5],(byte)143);
			cc.DrawChar1x((x+w-1)<<3,i,(byte)linechr[5],(byte)143);
		}
		cc.DrawChar1x(x<<3,y<<3,(byte)linechr[0],(byte)143);
		cc.DrawChar1x((x+w-1)<<3,y<<3,(byte)linechr[1],(byte)143);
		cc.DrawChar1x(x<<3,(y+h-1)<<3,(byte)linechr[2],(byte)143);
		cc.DrawChar1x((x+w-1)<<3,(y+h-1)<<3,(byte)linechr[3],(byte)143);
		cc.DrawChar1x((x+w-1)<<3,y<<3,(byte)'X',(byte)143);
		cc.DrawString1x((x+1)<<3,y<<3,title,(byte)143);
		cc.FillRect(cc.palette[8],(x+1)<<3,(y+1<<3),(w-2)<<3,(h-2)<<3);
	}
	
	public void render(Canvas cc)
	{
		resize();
		makeWindow(cc);
		int fx = (x+1)<<3;
		int fy = (y+1)<<3;
		switch(type)
		{
			case 1: // char screen
				for(int i=0;i<256;i++)
				{
					cc.DrawChar1x(fx+((i%(w-2))<<3),fy+((i/(w-2))<<3),(byte)i,(byte)143);
					if(i==charChosen)
					{
						cc.DrawRect(0xAAAAAA,fx+((i%(w-2))<<3),fy+((i/(w-2))<<3),7,7);
					}
					String tn = getNoteName(charChosen&0xFF);
					String t = "" + (charChosen&0xFF);
					if(isMelodium) cc.DrawString1x((x+w-2-t.length()-tn.length())<<3,(y+h-1)<<3,tn,142);
					cc.DrawString1x((x+w-1-t.length())<<3,(y+h-1)<<3,t,142);
				}
				break;
			case 2: // color screen
				cc.FillRect(0x000000,fx,fy,128,8); // DrawChar workaround
				for(int i=0;i<256;i++)
				{
					cc.DrawChar1x(fx+((i&15)<<3),fy+((i>>4)<<3),(byte)254,(byte)i);
					if(i==colorChosen)
					{
						cc.DrawRect(0xAAAAAA,fx+((i&15)<<3),fy+((i>>4)<<3),7,7);
					}
					String t = "" + (colorChosen&0xFF);
					cc.DrawString1x((x+w-1-t.length())<<3,(y+h-1)<<3,t,142);
				}
				break;
			case 3: // recent blocks screen
				for(int i=0;i<16;i++)
				{
					int tmx = fx+8+((i&3)<<4);
					int tmy = fy+8+((i>>2)<<4);
					cc.DrawChar(tmx,tmy,(byte)recBlockChr[i],(byte)recBlockCol[i]);
					if(Rectangle.insideRect(cc.mx,cc.my,tmx,tmy,16,16))
					{
						cc.DrawRect(0xAAAAAA,tmx,tmy,15,15);
					}
				}
				break;
			case 4: // types screen
				int i = 0;
				for(int j=-1;j<=255;j++)
				{
					if(Block.isPlaceable(j))
					{
						int col=143;
						if(j==typeChosen) col=248;
						String t = Block.getLongName(j);
						int xm = (x+((w-1)>>1));
						cc.DrawString1x(((x<<3)+(w<<2))-(t.length()<<2),(y+1+i)<<3,t,col);
						i++;
					}
				}
				break;
			case 5: // help
				int j = 0;
				for(;j<text.length();j+=24)
				{
					int k = j+24;
					if(text.length()<k) k=text.length();
					cc.DrawString1x(fx,fy+8+((j/24)<<3),text.substring(j,k),143);
				}
				break;
			default:
				break;
		}
	}
	
}
