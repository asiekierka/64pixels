package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class CraftrGameScreen extends CraftrScreen
{
	public int CHATBOTTOM_X = 11;
	public int CHATBOTTOM_Y = (GRID_H*16)-17;
	public int BARPOS_Y = (GRID_H*16);
	public int drawType;
	public int[] drawChrA = new int[256];
	public int[] drawColA = new int[256];
	public int barselMode;
	public int barType = 0;
	public int chrBarOff;
	public String chatMsg;
	public boolean viewFloorsMode;
	public boolean hideousPrompts=false;
	public int camX = 0;
	public int camY = 0;
	public int frames = 0;
	public int mx = 0;
	public int my = 0;
	public ArrayList<CraftrWindow> windows;
	
	public byte[] blockChr;
	public byte[] blockCol;
	public CraftrBlock[] blocks;

	public CraftrChatMsg[] chatarr;
	public int chatlen;
	public CraftrCanvas c;
	public int hov_type = 0;
	public int hov_par = 0;
	
	public CraftrPlayer players[] = new CraftrPlayer[256];

	public CraftrGameScreen(CraftrCanvas cc)
	{
		c = cc;
		windows = new ArrayList<CraftrWindow>();
		blocks = new CraftrBlock[FULLGRID_W*FULLGRID_H];
		blockChr = new byte[FULLGRID_W*FULLGRID_H];
		blockCol = new byte[FULLGRID_W*FULLGRID_H];
		chatarr = new CraftrChatMsg[20];
		chatlen = 0;
		for(int i=0;i<256;i++) drawColA[i] = 15;
		for(int i=0;i<256;i++) drawChrA[i] = 1;
		drawType = 0;
		barselMode = 1; // char
		chatMsg = "";
		barType = 0;
	}

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

	public boolean obstructedWindow(CraftrWindow w, int mx, int my)
	{
		synchronized(windows)
		{
			for(int wi = windows.size()-1;wi>windows.indexOf(w);wi--)
			{
				CraftrWindow cw = windows.get(wi);
				if(insideRect(mx,my,cw.x<<3,cw.y<<3,cw.w<<3,cw.h<<3)) return true;
			}
		}
		return false;
	}

	public boolean inWindow(int x, int y)
	{
		synchronized(windows)
		{
			for(CraftrWindow cw : windows)
				if(insideRect(mx,my,cw.x<<3,cw.y<<3,cw.w<<3,cw.h<<3)) return true;
		}
		return false;
	}

	public CraftrWindow getWindow(int type)
	{
		synchronized(windows)
		{
			for(CraftrWindow cw: windows)
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
			for(CraftrWindow cw : windows)
			{
				if(cw.type == type) app = windows.indexOf(cw);
			}
			if(app>=0) windows.remove(app);
			else windows.add(new CraftrWindow(type,4)); // UID chosen by fair dice roll. Guaranteed to be unique.
		}
	}
	public int gdrawChr()
	{
		return drawChrA[drawType<0?(0xFF&(int)((byte)drawType)):drawType];
	}
	public int gdrawCol()
	{
		return drawColA[drawType<0?(0xFF&(int)((byte)drawType)):drawType];
	}
	public void sdrawChr(int c)
	{
		drawChrA[drawType<0?(0xFF&(int)((byte)drawType)):drawType] = c;
	}
	public void sdrawCol(int c)
	{
		drawColA[drawType<0?(0xFF&(int)((byte)drawType)):drawType] = c;
	}
	public void paint(Graphics g, int mmx, int mmy)
	{
		mx = mmx;
		my = mmy;
		g.setColor(new Color(0x000000));
		g.fillRect(0,0,c.sizeX,c.sizeY);
		for(int iy=0;iy<FULLGRID_H-1;iy++)
		{
			for(int ix=0;ix<FULLGRID_W;ix++)
			{
			    if(blocks[ix+(iy*FULLGRID_W)] != null)
			    {
				    c.DrawChar(ix<<4,iy<<4,blockChr[ix+(iy*FULLGRID_W)],blockCol[ix+(iy*FULLGRID_W)], g);
				    if(blocks[ix+(iy*FULLGRID_W)].isBullet()) c.DrawChar(ix<<4,iy<<4,(byte)248,(byte)15, g);
				}else{
				    c.DrawChar(ix<<4,iy<<4,(byte)177,(byte)0x08, g);
				}
			}
		}
		for (int i=0;i<256;i++)
		{
			if(players[i]!=null)
			{
				g.setColor(new Color(0x000000));
				g.fillRect(players[i].px<<4,players[i].py<<4,16,16);
				c.DrawChar(players[i].px<<4,players[i].py<<4,players[i].pchr,players[i].pcol,g);
			}
		}
		switch(barType)
		{
			case 0: DrawBar(g); break;
			case 1: DrawChatBar(g); break;
		}
		DrawMouse(g);
		DrawChatMsg(g);
		for(int i=0;i<256;i++)
		{
			if(players[i] != null)
			{
				if(mx>>4 == players[i].px && my>>4 == players[i].py && !inWindow(mx,my))
				{
					writeChat((players[i].px*16+8)-((players[i].name.length()*8)>>1),players[i].py*16-10,new CraftrChatMsg(players[i].name),g);
				}
			}
		}
		synchronized(windows)
		{
			for(CraftrWindow cw : windows)
			{
				cw.typeChosen = drawType;
				cw.charChosen = gdrawChr();
				cw.colorChosen = gdrawCol();
				if(drawType==7) cw.isMelodium=true;
				else cw.isMelodium=false;
				cw.render(c,g);
			}
		}
		frames++;
	}

	
	// chat processing
	
	public void addChatMsg(String msg)
	{
		// This fixes the case that there is TOO MUCH CHAT GOING ON BRO
		System.arraycopy(chatarr,0,chatarr,1,19);
		chatarr[0] = new CraftrChatMsg(msg);
		if(chatlen<20) chatlen++;
		
	}
	public void writeChat(int x, int y, CraftrChatMsg msg, Graphics g)
	{
		int tx = x+1;
		int ty = y+1;
		int offs = 0;
		byte col = (byte)15;
		g.setColor(new Color(0x000000)); // black.
		g.fillRect(x-1,y-1,1,10);
		for(int i=0; i<msg.msglen; i++)
		{
			g.setColor(new Color(0x000000));
			g.fillRect(x+((i-offs)<<3),y-1,9,10);
			String z;
			char[] t = new char[1];
			if((i+2)<msg.msglen)
			{
				z = msg.message[i+1]+"";
				t=z.toUpperCase().toCharArray();
			}
			if(msg.message[i]=='&' && ((i+2)<msg.msglen) && ((t[0]>'0' && t[0]<='9') || (t[0]>='A' && t[0]<='F')))
			{
				col=(byte)(t[0]-'0');
				if(col>9) col=(byte)((t[0]-'A')+10);
				i+=2;
				offs+=2;
			} 
			c.DrawChar1x(tx+((i-offs)<<3),ty,(byte)msg.message[i],col,g);
		}
	}
	
	public void DrawChatMsg(Graphics g)
	{
		if(chatlen>0)
		{
			Date now = new Date();
			int ix=CHATBOTTOM_X;
			for(int i=0;i<chatlen;i++)
			{
				if ((now.compareTo(chatarr[i].expirytime) < 0) || barType==1)
				{
					writeChat(ix,CHATBOTTOM_Y-(i*10),chatarr[i],g);
				}
			}
		}
	}
	
	public static String getName(int t)
	{
		switch(t)
		{
			case 0:
				return "Floor";
			case 1:
				return "Wall";
			case 2:
				return "Wirium";
			case 3:
				return "P-NAND";
			case 4:
				return "Crossuh";
			case 5:
				return "Plate";
			case 6:
				return "Door";
			case 7:
				return "Meloder";
			case 8:
				return "Roofy";
			case 9:
				return "Pensor";
			case 10:
				return "Pumulty";
			case 11:
				return "Bodder";
			case 12:
				return "Cannona";
			case 13:
				return "Bullsor";
			case 14:
				return "Break";
			case 15:
				return "Slider";
			case -1:
				return "Pushium";
		}
		return "???????";
	}
	// painting handlers
	
	public void DrawMouse(Graphics g)
	{
		g.setColor(new Color(0xAAAAAA));
		if(mx >= 0 && mx < WIDTH && my >= 0 && my < (GRID_H<<4))
		{
			String tstr=getName(hov_type);
			g.drawRect((mx&(~15)),(my&(~15)),15,15);
			if(viewFloorsMode) writeChat(mx-(tstr.length()<<2),my-10,new CraftrChatMsg(tstr),g);
		}
	}
	
	public void DrawChatBar(Graphics g)
	{
		g.setColor(new Color(0x000000));
		g.fillRect(0,BARPOS_Y,WIDTH,16);
		c.DrawString1x(0,BARPOS_Y,">" + chatMsg, 15, g);
	}
	
	public void DrawBar(Graphics g)
	{
		g.setColor(new Color(0x000000));
		g.fillRect(0,BARPOS_Y,WIDTH,16);
		if(hideousPrompts) c.DrawString1x(0,BARPOS_Y+16,"      Type       ",9,g);
		c.DrawString(0,BARPOS_Y,getName(drawType),15,g);
		c.DrawChar1x(7*16,BARPOS_Y+8,(byte)'T',(byte)10,g);
		c.DrawChar1x(7*16+8,BARPOS_Y,(byte)30,(byte)14,g);
		c.DrawChar1x(7*16+8,BARPOS_Y+8,(byte)31,(byte)14,g);
		c.DrawChar1x(8*16,BARPOS_Y,(byte)179,(byte)15,g);
		c.DrawChar1x(8*16,BARPOS_Y+8,(byte)179,(byte)15,g);
		c.DrawChar1x(10*16,BARPOS_Y,(byte)179,(byte)15,g);
		c.DrawChar1x(10*16,BARPOS_Y+8,(byte)179,(byte)15,g);
		if(drawType==2) c.DrawChar(10*16+12,BARPOS_Y,(byte)197,(byte)((gdrawCol()&7)+8),g);
		else c.DrawChar(10*16+12,BARPOS_Y,(byte)gdrawChr(),(byte)gdrawCol(),g);
		c.DrawChar1x(12*16,BARPOS_Y,(byte)179,(byte)15,g);
		c.DrawChar1x(12*16,BARPOS_Y+8,(byte)179,(byte)15,g);
		if(drawType == 4) barselMode=2;
		int bsmt= barselMode;
		if(drawType == 2) bsmt=3;
		else if(drawType == 3 && barselMode == 1) bsmt=4;
		if(drawType == 3 && (gdrawChr()<24 || gdrawChr()>=28)) sdrawChr(25);
		else if(drawType==4) sdrawChr(206);
		switch(bsmt)
		{
			case 1: // char
				if(hideousPrompts) c.DrawString1x(12*16+8,BARPOS_Y+16,"               Char               ",9,g);
				c.DrawString1x(8*16+8,BARPOS_Y,"Chr",240,g);
				c.DrawString1x(8*16+8,BARPOS_Y+8,"Col",15,g);
				c.DrawChar1x(12*16+8,BARPOS_Y+4,(byte)17,(byte)14,g);
				for(int j=0;j<16;j++)
				{
					c.DrawChar(13*16+(j<<4),BARPOS_Y,(byte)(chrBarOff+j),(byte)gdrawCol(),g);
				}
				c.DrawChar1x(29*16,BARPOS_Y+4,(byte)16,(byte)14,g);
				c.DrawChar1x(29*16+8,BARPOS_Y,(byte)179,(byte)15,g);
				c.DrawChar1x(29*16+8,BARPOS_Y+8,(byte)179,(byte)15,g);
				if(gdrawChr() >= chrBarOff && gdrawChr() < chrBarOff+16)
				{
					g.setColor(new Color(0xAAAAAA));
					g.drawRect(13*16+((gdrawChr()-chrBarOff)*16),BARPOS_Y,15,15);
				}
				break;
			case 2: // color
				if(hideousPrompts) c.DrawString1x(12*16+8,BARPOS_Y+16,"      Color       ",9,g);
				if(drawType==3) c.DrawString1x(8*16+8,BARPOS_Y,"Dir",15,g);
				else if (drawType != 4) c.DrawString1x(8*16+8,BARPOS_Y,"Chr",15,g);
				c.DrawString1x(8*16+8,BARPOS_Y+8,"Col",240,g);
				for(int j=0;j<16;j++)
				{
					c.DrawChar1x(12*16+8+(j<<3),BARPOS_Y,(byte)254,(byte)((j<<4)|(gdrawCol()&15)),g);
					c.DrawChar1x(12*16+8+(j<<3),BARPOS_Y+8,(byte)254,(byte)(j|(gdrawCol()&240)),g);
				}
				c.DrawChar1x(20*16+10,BARPOS_Y,(byte)'B',(byte)15,g);
				c.DrawChar1x(20*16+10,BARPOS_Y+8,(byte)'F',(byte)15,g);
				c.DrawChar1x(21*16+2,BARPOS_Y,(byte)'G',(byte)15,g);
				c.DrawChar1x(21*16+2,BARPOS_Y+8,(byte)'G',(byte)15,g);
				c.DrawChar1x(21*16+10,BARPOS_Y,(byte)179,(byte)15,g);
				c.DrawChar1x(21*16+10,BARPOS_Y+8,(byte)179,(byte)15,g);
				g.setColor(new Color(0xAAAAAA));
				g.drawRect(12*16+8+((gdrawCol()>>4)<<3),BARPOS_Y,7,7);
				g.drawRect(12*16+8+((gdrawCol()&15)<<3),BARPOS_Y+8,7,7);
				break;
			case 3: // wirium color
				for(int j=0;j<8;j++)
				{
					c.DrawChar(12*16+8+(j<<4),BARPOS_Y,(byte)254,(byte)j,g);
				}
				c.DrawString1x(20*16+12,BARPOS_Y,"Wirium",15,g);
				c.DrawString1x(20*16+12,BARPOS_Y+8,"Colors",15,g);
				c.DrawChar1x(24*16,BARPOS_Y,(byte)179,(byte)15,g);
				c.DrawChar1x(24*16,BARPOS_Y+8,(byte)179,(byte)15,g);
				g.setColor(new Color(0xAAAAAA));
				g.drawRect(12*16+8+((gdrawCol()&7)<<4),BARPOS_Y,15,15);
				break;
			case 4: // p-nand direction
				c.DrawString1x(8*16+8,BARPOS_Y,"Dir",240,g);
				c.DrawString1x(8*16+8,BARPOS_Y+8,"Col",15,g);
				for(int j=24;j<28;j++)
				{
					c.DrawChar(12*16+8+((j-24)<<4),BARPOS_Y,(byte)j,(byte)15,g);
				}
				c.DrawString1x(16*16+12,BARPOS_Y+4,"Direction",15,g);
				c.DrawChar1x(21*16+8,BARPOS_Y,(byte)179,(byte)15,g);
				c.DrawChar1x(21*16+8,BARPOS_Y+8,(byte)179,(byte)15,g);
				g.setColor(new Color(0xAAAAAA));
				g.drawRect(12*16+8+((gdrawChr()-24)<<4),BARPOS_Y,15,15);
			default:
				break;
		}
		c.DrawChar(30*16,BARPOS_Y,(byte)1,(byte)12,g);
		c.DrawChar(31*16,BARPOS_Y,(byte)'C',(byte)9,g);
	}
	
	public int addPlayer(int id, int scrx, int scry, String name, byte ch, byte co)
	{
		players[id] = new CraftrPlayer(scrx,scry,ch,co,name);
		return id;
	}

	public void removePlayer(int id)
	{
		players[id] = null;
	}
}
