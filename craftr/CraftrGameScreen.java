import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class CraftrGameScreen extends CraftrScreen
{
	public static final int GRID_W = 32;
	public static final int GRID_H = 25;
	public static final int FULLGRID_W = GRID_W+1;
	public static final int FULLGRID_H = GRID_H+1;
	public static final int WIDTH = ((FULLGRID_W-1)*16);
	public static final int HEIGHT = (FULLGRID_H*16);
	public static final int CHATBOTTOM_X = 11;
	public static final int CHATBOTTOM_Y = (GRID_H*16)-17;
	public static final int BARPOS_Y = GRID_H*16;
	public int drawCol, drawChr, drawType;
	public int barselMode;
	public int barType = 0;
	public int chrBarOff;
	public String chatMsg;
	public boolean viewFloorsMode;
	
	public int camX = 0;
	public int camY = 0;
	public int frames = 0;
	public int mx = 0;
	public int my = 0;
	
	public byte[] scr_col;
	public byte[] scr_chr;
	public byte[] f_col;
	public byte[] f_chr;
	public byte[] scr_typ;
	public CraftrChatMsg[] chatarr;
	public int chatlen;
	public boolean cwOpen;
	public CraftrWindow cw;
	public CraftrCanvas c;
	public int hov_type = 0;
	public int hov_par = 0;
	
	public CraftrGameScreen(CraftrCanvas cc)
	{
		c = cc;
		scr_col = new byte[FULLGRID_W*FULLGRID_H];
		scr_chr = new byte[FULLGRID_W*FULLGRID_H];
		f_col = new byte[FULLGRID_W*FULLGRID_H];
		f_chr = new byte[FULLGRID_W*FULLGRID_H];
		scr_typ = new byte[FULLGRID_W*FULLGRID_H];
		chatarr = new CraftrChatMsg[20];
		chatlen = 0;
		drawCol = 15;
		drawChr = 1;
		drawType = 0;
		barselMode = 1; // char
		cw = new CraftrWindow(1);
		cwOpen = false;
		cw.x++;
		chatMsg = "";
		barType = 0;
	}
	
	public void paint(Graphics g, int mmx, int mmy)
	{
		mx = mmx;
		my = mmy;
		g.setColor(new Color(0x000000));
		g.fillRect(0,0,c.sizeX,c.sizeY);
		for(int iy=0;iy<FULLGRID_H-1;iy++)
		{
			//System.out.println(ix);
			for(int ix=0;ix<FULLGRID_W;ix++)
			{
				int t1 = ix+(iy*FULLGRID_W);
				if(scr_typ[t1] == 2)
				{
					c.DrawCharD(ix<<4,iy<<4,f_chr[t1],f_col[t1], g);
					c.DrawChar(ix<<4,iy<<4,scr_chr[t1],(byte)(scr_col[t1]&15), g);
				}
				else c.DrawChar(ix<<4,iy<<4,scr_chr[t1],scr_col[t1], g);
				
			}
		}
		switch(barType)
		{
			case 0: DrawBar(g); break;
			case 1: DrawChatBar(g); break;
		}
		DrawMouse(g);
		DrawChatMsg(g);
		cw.charChosen = drawChr;
		cw.colorChosen = drawCol;
		if(cwOpen) cw.render(c,g);
		for(int i=0;i<256;i++)
		{
			if(players[i] != null && mx>>4 == players[i].px && my>>4 == players[i].py && (!cwOpen || (mx>>3 < cw.x && my>>3 < cw.y && mx>>3 >= cw.w+cw.x && mx>>4 >= cw.h+cw.y)))
			{
				writeChat((players[i].px*16+8)-((players[i].name.length()*8)>>1),players[i].py*16-10,new CraftrChatMsg(players[i].name),g);
			}
		}
		frames++;
	}
	
	
	public boolean writeString(int x, int y, String str, int col, boolean useBar)
	{
		char[] ca = str.toCharArray();
		int pos = 0;
		if(useBar) return true;
		pos = x+(y*FULLGRID_W);
		if((pos+ca.length) > (FULLGRID_W*FULLGRID_H)) return false;
		int i = 0;
		int j = 0;
		while(i<ca.length)
		{
			if((j%FULLGRID_W) < (FULLGRID_W-1))
			{
				scr_col[pos+j] = (byte)col;
				scr_chr[pos+j] = (byte)ca[i];
				i++;
			}
			j++;
		}
		return true;
	}
	
	public boolean writeString(int x, int y, String str, int col)
	{
		return writeString(x,y,str,col,false);
	}
	
	// chat processing
	
	public void addChatMsg(String msg, int ncol) // ncol was here for debug purposes... meh
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
			g.setColor(new Color(0x000000)); // black.
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
	
	public String getName(int t)
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
		c.DrawString(0,BARPOS_Y,getName(drawType),15,g);
		c.DrawChar1x(7*16+8,BARPOS_Y,(byte)30,(byte)14,g);
		c.DrawChar1x(7*16+8,BARPOS_Y+8,(byte)31,(byte)14,g);
		c.DrawChar1x(8*16,BARPOS_Y,(byte)179,(byte)15,g);
		c.DrawChar1x(8*16,BARPOS_Y+8,(byte)179,(byte)15,g);
		c.DrawChar1x(10*16,BARPOS_Y,(byte)179,(byte)15,g);
		c.DrawChar1x(10*16,BARPOS_Y+8,(byte)179,(byte)15,g);
		if(drawType==2) c.DrawChar(10*16+12,BARPOS_Y,(byte)197,(byte)((drawCol&7)+8),g);
		else c.DrawChar(10*16+12,BARPOS_Y,(byte)drawChr,(byte)drawCol,g);
		c.DrawChar1x(12*16,BARPOS_Y,(byte)179,(byte)15,g);
		c.DrawChar1x(12*16,BARPOS_Y+8,(byte)179,(byte)15,g);
		if(drawType == 4) barselMode=2;
		int bsmt= barselMode;
		if(drawType == 2) bsmt=3;
		else if(drawType == 3 && barselMode == 1) bsmt=4;
		if(drawType == 3 && (drawChr<24 || drawChr>=28)) drawChr=25;
		else if(drawType==4) drawChr=206;
		switch(bsmt)
		{
			case 1: // char
				c.DrawString1x(8*16+8,BARPOS_Y,"Chr",240,g);
				c.DrawString1x(8*16+8,BARPOS_Y+8,"Col",15,g);
				for(int j=0;j<16;j++)
				{
					c.DrawChar(12*16+8+(j<<4),BARPOS_Y,(byte)(chrBarOff+j),(byte)drawCol,g);
				}
				c.DrawChar1x(28*16+8,BARPOS_Y,(byte)30,(byte)14,g);
				c.DrawChar1x(28*16+8,BARPOS_Y+8,(byte)31,(byte)14,g);
				c.DrawChar1x(29*16,BARPOS_Y,(byte)179,(byte)15,g);
				c.DrawChar1x(29*16,BARPOS_Y+8,(byte)179,(byte)15,g);
				if(drawChr >= chrBarOff && drawChr < chrBarOff+16)
				{
					g.setColor(new Color(0xAAAAAA));
					g.drawRect(12*16+8+((drawChr-chrBarOff)*16),BARPOS_Y,15,15);
				}
				break;
			case 2: // color
				if(drawType==3) c.DrawString1x(8*16+8,BARPOS_Y,"Dir",15,g);
				else if (drawType != 4) c.DrawString1x(8*16+8,BARPOS_Y,"Chr",15,g);
				c.DrawString1x(8*16+8,BARPOS_Y+8,"Col",240,g);
				for(int j=0;j<16;j++)
				{
					c.DrawChar1x(12*16+8+(j<<3),BARPOS_Y,(byte)254,(byte)((j<<4)|(drawCol&15)),g);
					c.DrawChar1x(12*16+8+(j<<3),BARPOS_Y+8,(byte)254,(byte)(j|(drawCol&240)),g);
				}
				c.DrawChar1x(20*16+10,BARPOS_Y,(byte)'B',(byte)15,g);
				c.DrawChar1x(20*16+10,BARPOS_Y+8,(byte)'F',(byte)15,g);
				c.DrawChar1x(21*16+2,BARPOS_Y,(byte)'G',(byte)15,g);
				c.DrawChar1x(21*16+2,BARPOS_Y+8,(byte)'G',(byte)15,g);
				c.DrawChar1x(21*16+10,BARPOS_Y,(byte)179,(byte)15,g);
				c.DrawChar1x(21*16+10,BARPOS_Y+8,(byte)179,(byte)15,g);
				g.setColor(new Color(0xAAAAAA));
				g.drawRect(12*16+8+((drawCol>>4)<<3),BARPOS_Y,7,7);
				g.drawRect(12*16+8+((drawCol&15)<<3),BARPOS_Y+8,7,7);
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
				g.drawRect(12*16+8+((drawCol&7)<<4),BARPOS_Y,15,15);
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
				g.drawRect(12*16+8+((drawChr-24)<<4),BARPOS_Y,15,15);
			default:
				break;
		}
		c.DrawChar(30*16,BARPOS_Y,(byte)1,(byte)12,g);
		c.DrawChar(31*16,BARPOS_Y,(byte)'C',(byte)9,g);
	}
	public CraftrPlayer players[] = new CraftrPlayer[256];
	
	public int addPlayer(int id, int scrx, int scry, String name)
	{
		players[id] = new CraftrPlayer(scrx,scry,(byte)0,(byte)0,name);
		return id;
	}
	
	public void removePlayer(int id)
	{
		players[id] = null;
	}
	
	public void drawBlock(int pos, byte aChar, byte aCol)
	{
		if(pos>=0 && pos<scr_chr.length)
		{
			scr_chr[pos] = aChar;
			scr_col[pos] = aCol;
		}
	}
	
	public void drawBlock(int x, int y, byte aChar, byte aCol)
	{
		drawBlock(x+(y*FULLGRID_W),aChar,aCol);
	}

}
