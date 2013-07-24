package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.awt.event.*;

public class GameScreen extends Screen
{
	public int CHATBOTTOM_X = 11;
	public int CHATBOTTOM_Y;
	public int BARPOS_Y;
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
	public int health = 3;
	public boolean showHealthBar = true;
	public int mx = 0;
	public int my = 0;
	public ArrayList<Window> windows;
	public boolean isSticky;

	public Block[] blocks;

	public ChatMsg[] chatarr;
	public int chatlen;
	public Canvas c;
	public int hov_type = 0;
	public int hov_par = 0;
	
	public Player players[] = new Player[256];

	public GameScreen(Canvas cc)
	{
		c = cc;
		windows = new ArrayList<Window>();
		if(c!=null)
		{
			blocks = new Block[c.FULLGRID_W*c.FULLGRID_H];
			CHATBOTTOM_Y = (c.GRID_H*16)-17;
			BARPOS_Y = (c.GRID_H*16);
		}
		chatarr = new ChatMsg[20];
		chatlen = 0;
		for(int i=0;i<256;i++) drawColA[i] = 15;
		for(int i=0;i<256;i++) drawChrA[i] = 1;
		drawType = 0;
		barselMode = 1; // char
		chatMsg = "";
		barType = 0;
	}

	public void invokeHelp(String t)
	{
		toggleWindow(5);
		getWindow(5).text=t;
		getWindow(5).resize();
	}

	public boolean mousePressed(MouseEvent ev)
	{
		int mb = ev.getButton();
		if (insideRect(mx,my,7*16+8,BARPOS_Y,8,8)) // type, up
		{
			if(mb==ev.BUTTON3) { invokeHelp("Choose the type."); return false; }
			drawType=(drawType-1)&0xFF;
			while(!Block.isPlaceable(drawType)) drawType = (drawType-1)&0xFF;
		} else if (insideRect(mx,my,7*16+8,BARPOS_Y+8,8,8)) // type, down
		{
			if(mb==ev.BUTTON3) { invokeHelp("Choose the type."); return false; }
			drawType=(drawType+1)&0xFF;
			while(!Block.isPlaceable(drawType)) drawType=(drawType+1)&0xFF;
		}
		else if (insideRect(mx,my,7*16,BARPOS_Y+8,8,8)) // T
		{
			if(mb==ev.BUTTON3) { invokeHelp("Open type selection window."); return false; }
			toggleWindow(4);
		}
		 else if (insideRect(mx,my,8*16+8,BARPOS_Y,24,8)) // mode, chr
		{
			if(mb==ev.BUTTON3) { invokeHelp("Set the bar mode to choosing characters."); return false; }
			barselMode = 1;
		} else if (insideRect(mx,my,8*16+8,BARPOS_Y+8,24,8)) // mode, col
		{
			if(mb==ev.BUTTON3) { invokeHelp("Set the bar mode to choosing colors."); return false; }
			barselMode = 2;
		} else if(insideRect(mx,my,30*16,BARPOS_Y,16,16))
		{
			if(mb==ev.BUTTON3) { invokeHelp("Open the character selection window."); return false; }
			toggleWindow(1);
		}
		else if(insideRect(mx,my,31*16,BARPOS_Y,16,16))
		{
			if(mb==ev.BUTTON3) { invokeHelp("Open the color selection window."); return false; }
			toggleWindow(2);
		}
		else if (drawType == 17)
		{
			if(insideRect(mx,my,12*16+8,BARPOS_Y,48,8))
			{
				if(mb==ev.BUTTON3) { invokeHelp("Unstickify piston."); return false; }
				isSticky=false;
			}
			else if(insideRect(mx,my,12*16+8,BARPOS_Y+8,48,8))
			{
				if(mb==ev.BUTTON3) { invokeHelp("Stickify piston."); return false; }
				isSticky=true;
			}
		}
		else if (drawType == 2)
		{
			if(insideRect(mx,my,12*16+8,BARPOS_Y,128,16))
			{
				if(mb==ev.BUTTON3) { invokeHelp("Select color."); return false; }
				sdrawCol((mx-(12*16+8))>>4);
			}
		}
		else if (barselMode == 1 && (drawType == 3 || drawType == 20)) // p-nand dir
		{
			if(insideRect(mx,my,12*16+8,BARPOS_Y,64,16))
			{
				if(mb==ev.BUTTON3) { invokeHelp("Select block output direction."); return false; }
				sdrawChr(24+((mx-(12*16+8))>>4));
			}
		}
		else if (barselMode == 1 && drawType == 15) // extend dir
		{
			if(insideRect(mx,my,12*16+8,BARPOS_Y,64,16))
			{
				if(mb==ev.BUTTON3) { invokeHelp("Select block output direction."); return false; }
				sdrawChr(Block.extendDir[((mx-(12*16+8))>>4)]);
			}
		}
		else if (barselMode == 1) // checkings, chr
		{
			if(insideRect(mx,my,13*16,BARPOS_Y,256,16))
			{
				if(mb==ev.BUTTON3) { invokeHelp("Select character."); return false; }
				sdrawChr(((mx-(13*16))>>4)+chrBarOff);
			}
			else if(mb==ev.BUTTON3 && insideRect(mx,my,12*16+8,BARPOS_Y+1,8,14))
			{
				chrBarOff -= 16;
				if(chrBarOff<0) chrBarOff += 256;
				return true;
			}
			else if(mb==ev.BUTTON3 && insideRect(mx,my,29*16,BARPOS_Y+1,8,14))
			{
				chrBarOff += 16;
				if(chrBarOff>255) chrBarOff -= 256;
				return true;
			}
		} else if (barselMode == 2) // checkings, col
		{
			if(insideRect(mx,my,12*16+8,BARPOS_Y,128,16))
			{
				if(mb==ev.BUTTON3) { invokeHelp("Select colour."); return false; }
				int colChoose = (mx-(12*16+8))>>3;
				int colMode = my-BARPOS_Y;
				if(colMode>7) // FG
				{
					sdrawCol((gdrawCol()&240)|(colChoose&15));
				} else // BG
				{
					sdrawCol((gdrawCol()&15)|((colChoose&15)<<4));
				}
			}
		}
		return false;
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

	public void setCanvas(Canvas canvas)
	{
		c=canvas;
		if(c!=null)
		{
			blocks = new Block[c.FULLGRID_W*c.FULLGRID_H];
			CHATBOTTOM_Y = (c.GRID_H*16)-17;
			BARPOS_Y = (c.GRID_H*16);
		}
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
	public void paint(int mmx, int mmy)
	{
		mx = mmx;
		my = mmy;
		c.FillRect(0x000000,0,0,c.sizeX,c.sizeY);
		Block t;
		for(int iy=0;iy<c.FULLGRID_H-1;iy++)
		{
			for(int ix=0;ix<c.FULLGRID_W;ix++)
			{
				t = blocks[ix+(iy*c.FULLGRID_W)];
				if(t != null)
				{
					c.DrawChar(ix<<4,iy<<4,(byte)t.getDrawnChar(),(byte)t.getDrawnColor());
					if(t.isBullet())
						switch(t.getBullet())
						{
							case 1:
							case 2:
							case 3:
							case 4:
								c.DrawChar(ix<<4,iy<<4,(byte)248,(byte)15);
								break;
						}
				}else{
					c.DrawChar(ix<<4,iy<<4,(byte)177,(byte)0x08);
				}
			}
		}
		for (int i=0;i<256;i++)
		{
			if(players[i]!=null)
			{
				c.FillRect(0x000000,players[i].x<<4,players[i].y<<4,16,16);
				c.DrawChar(players[i].x<<4,players[i].y<<4,players[i].chr,players[i].col);
			}
		}
		switch(barType)
		{
			case 0: DrawBar(); break;
			case 1: DrawChatBar(); break;
		}
		DrawMouse();
		DrawChatMsg();
		DrawHealthBar();
		for(int i=0;i<256;i++)
		{
			if(players[i] != null)
			{
				if(mx>>4 == players[i].x && my>>4 == players[i].y && !inWindow(mx,my))
				{
					writeChat((players[i].x*16+8)-((players[i].name.length()*8)>>1),players[i].y*16-10,new ChatMsg(players[i].name));
				}
			}
		}
		synchronized(windows)
		{
			for(Window cw : windows)
			{
				cw.typeChosen = drawType;
				cw.charChosen = gdrawChr();
				cw.colorChosen = gdrawCol();
				cw.isMelodium=(drawType==7);
				cw.render(c);
			}
		}
		frames++;
	}

	
	// chat processing
	
	public void addChatMsg(String msg)
	{
		// This fixes the case that there is TOO MUCH CHAT GOING ON BRO
		System.arraycopy(chatarr,0,chatarr,1,19);
		chatarr[0] = new ChatMsg(msg);
		if(chatlen<20) chatlen++;
		
	}
	public void writeChat(int x, int y, ChatMsg msg)
	{
		int tx = x+1;
		int ty = y+1;
		int offs = 0;
		byte col = (byte)15;
		c.FillRect(0x000000,x-1,y-1,1,10);
		for(int i=0; i<msg.msglen; i++)
		{
			c.FillRect(0x000000,x+((i-offs)<<3),y-1,9,10);
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
			c.DrawChar1x(tx+((i-offs)<<3),ty,(byte)msg.message[i],col);
		}
	}
	public void DrawHealthBar()
	{
		if(!showHealthBar) return;
		int startX = c.WIDTH-42;
		int startY = BARPOS_Y-9;
		for(int i=0;i<5;i++)
			if(health>i)
				c.DrawChar1x(startX+(i<<3),startY,(byte)3,(byte)0x0C);
			else
				c.DrawChar1x(startX+(i<<3),startY,(byte)3,(byte)0x08);
	}
	public void DrawChatMsg()
	{
		if(chatlen>0)
		{
			Date now = new Date();
			int ix=CHATBOTTOM_X;
			for(int i=0;i<chatlen;i++)
			{
				if ((now.compareTo(chatarr[i].expirytime) < 0) || barType==1)
				{
					writeChat(ix,CHATBOTTOM_Y-(i*10),chatarr[i]);
				}
			}
		}
	}

	// painting handlers
	
	public void DrawMouse()
	{
		if(mx >= 0 && mx < c.WIDTH && my >= 0 && my < (c.GRID_H<<4))
		{
			String tstr=Block.getName(hov_type);
			c.DrawRect(0xAAAAAA,(mx&(~15)),(my&(~15)),15,15);
			if(viewFloorsMode) writeChat(mx-(tstr.length()<<2),my-10,new ChatMsg(tstr));
		}
	}
	
	public void DrawChatBar()
	{
		c.FillRect(0x000000,0,BARPOS_Y,c.WIDTH,16);
		c.DrawString1x(0,BARPOS_Y,">" + chatMsg, 15);
	}
	
	public boolean isArrow()
	{
		return (drawType==3 || drawType==15 || drawType==20);
	}

	public void DrawBar()
	{
		c.FillRect(0x000000,0,BARPOS_Y,c.WIDTH,16);
		if(hideousPrompts) c.DrawString1x(0,BARPOS_Y+16,"      Type       ",9);
		c.DrawString(0,BARPOS_Y,Block.getName(drawType),15);
		c.DrawChar1x(7*16,BARPOS_Y+8,(byte)'T',(byte)10);
		c.DrawChar1x(7*16+8,BARPOS_Y,(byte)30,(byte)14);
		c.DrawChar1x(7*16+8,BARPOS_Y+8,(byte)31,(byte)14);
		c.DrawChar1x(8*16,BARPOS_Y,(byte)179,(byte)15);
		c.DrawChar1x(8*16,BARPOS_Y+8,(byte)179,(byte)15);
		c.DrawChar1x(10*16,BARPOS_Y,(byte)179,(byte)15);
		c.DrawChar1x(10*16,BARPOS_Y+8,(byte)179,(byte)15);
		if(drawType==2) c.DrawChar(10*16+12,BARPOS_Y,(byte)197,(byte)((gdrawCol()&7)+8));
		else c.DrawChar(10*16+12,BARPOS_Y,(byte)gdrawChr(),(byte)gdrawCol());
		c.DrawChar1x(12*16,BARPOS_Y,(byte)179,(byte)15);
		c.DrawChar1x(12*16,BARPOS_Y+8,(byte)179,(byte)15);
		if(drawType == 4) barselMode=2;
		int bsmt = barselMode;
		if(drawType == 2) bsmt=3;
		else if(isArrow() && barselMode == 1) bsmt=4;
		else if(drawType == 17) bsmt=5;
		else if(drawType >= 21 && drawType <= 22) bsmt=0;
		if((drawType == 3 || drawType ==20)&& (gdrawChr()<24 || gdrawChr()>=28)) sdrawChr(25);
		else if(drawType == 15 && !(gdrawChr()==30 || gdrawChr()==31 || gdrawChr()==16 || gdrawChr()==17)) sdrawChr(31);
		else if(drawType==4) sdrawChr(206);
		else if (drawType == 17)
		{
			if(isSticky) sdrawCol(0x2E);
			else sdrawCol(0x7F);
			sdrawChr(177);
		}
		else if (drawType == 21)
		{
			sdrawCol(0x06);
			sdrawChr(153);
		} else if (drawType == 22)
		{
			sdrawCol(0x0B);
			sdrawChr(227);
		}
		switch(bsmt)
		{
			case 1: // char
				if(hideousPrompts) c.DrawString1x(12*16+8,BARPOS_Y+16,"               Char               ",9);
				c.DrawString1x(8*16+8,BARPOS_Y,"Chr",240);
				c.DrawString1x(8*16+8,BARPOS_Y+8,"Col",15);
				c.DrawChar1x(12*16+8,BARPOS_Y+4,(byte)17,(byte)14);
				for(int j=0;j<16;j++)
				{
					c.DrawChar(13*16+(j<<4),BARPOS_Y,(byte)(chrBarOff+j),(byte)gdrawCol());
				}
				c.DrawChar1x(29*16,BARPOS_Y+4,(byte)16,(byte)14);
				c.DrawChar1x(29*16+8,BARPOS_Y,(byte)179,(byte)15);
				c.DrawChar1x(29*16+8,BARPOS_Y+8,(byte)179,(byte)15);
				if(gdrawChr() >= chrBarOff && gdrawChr() < chrBarOff+16)
				{
					c.DrawRect(0xAAAAAA,13*16+((gdrawChr()-chrBarOff)*16),BARPOS_Y,15,15);
				}
				break;
			case 2: // color
				if(hideousPrompts) c.DrawString1x(12*16+8,BARPOS_Y+16,"      Color       ",9);
				if(isArrow()) c.DrawString1x(8*16+8,BARPOS_Y,"Dir",15);
				else if (drawType != 4) c.DrawString1x(8*16+8,BARPOS_Y,"Chr",15);
				c.DrawString1x(8*16+8,BARPOS_Y+8,"Col",240);
				for(int j=0;j<16;j++)
				{
					c.DrawChar1x(12*16+8+(j<<3),BARPOS_Y,(byte)254,(byte)((j<<4)|(gdrawCol()&15)));
					c.DrawChar1x(12*16+8+(j<<3),BARPOS_Y+8,(byte)254,(byte)(j|(gdrawCol()&240)));
				}
				c.DrawChar1x(20*16+10,BARPOS_Y,(byte)'B',(byte)15);
				c.DrawChar1x(20*16+10,BARPOS_Y+8,(byte)'F',(byte)15);
				c.DrawChar1x(21*16+2,BARPOS_Y,(byte)'G',(byte)15);
				c.DrawChar1x(21*16+2,BARPOS_Y+8,(byte)'G',(byte)15);
				c.DrawChar1x(21*16+10,BARPOS_Y,(byte)179,(byte)15);
				c.DrawChar1x(21*16+10,BARPOS_Y+8,(byte)179,(byte)15);
				c.DrawRect(0xAAAAAA,12*16+8+((gdrawCol()>>4)<<3),BARPOS_Y,7,7);
				c.DrawRect(0xAAAAAA,12*16+8+((gdrawCol()&15)<<3),BARPOS_Y+8,7,7);
				break;
			case 3: // wirium color
				for(int j=0;j<8;j++)
				{
					c.DrawChar(12*16+8+(j<<4),BARPOS_Y,(byte)254,(byte)j);
				}
				c.DrawString1x(20*16+12,BARPOS_Y,"Wirium",15);
				c.DrawString1x(20*16+12,BARPOS_Y+8,"Colors",15);
				c.DrawChar1x(24*16,BARPOS_Y,(byte)179,(byte)15);
				c.DrawChar1x(24*16,BARPOS_Y+8,(byte)179,(byte)15);
				c.DrawRect(0xAAAAAA,12*16+8+((gdrawCol()&7)<<4),BARPOS_Y,15,15);
				break;
			case 4: // p-nand direction
				c.DrawString1x(8*16+8,BARPOS_Y,"Dir",240);
				c.DrawString1x(8*16+8,BARPOS_Y+8,"Col",15);
				if(drawType==3 || drawType==20)
				{
					for(int j=24;j<28;j++)
					{
						c.DrawChar(12*16+8+((j-24)<<4),BARPOS_Y,(byte)j,(byte)15);
					}
					c.DrawRect(0xAAAAAA,12*16+8+((gdrawChr()-24)<<4),BARPOS_Y,15,15);
				}
				else if(drawType==15)
				{
					c.DrawChar(12*16+8,BARPOS_Y,(byte)30,(byte)15);
					c.DrawChar(12*16+24,BARPOS_Y,(byte)31,(byte)15);
					c.DrawChar(12*16+40,BARPOS_Y,(byte)16,(byte)15);
					c.DrawChar(12*16+56,BARPOS_Y,(byte)17,(byte)15);
					switch(gdrawChr())
					{
						case 30:
						case 31:
							c.DrawRect(0xAAAAAA,12*16+8+((gdrawChr()-30)<<4),BARPOS_Y,15,15);
							break;
						case 16:
						case 17:
							c.DrawRect(0xAAAAAA,12*16+40+((gdrawChr()-16)<<4),BARPOS_Y,15,15);
							break;
					}
				}
				c.DrawString1x(16*16+12,BARPOS_Y+4,"Direction",15);
				c.DrawChar1x(21*16+8,BARPOS_Y,(byte)179,(byte)15);
				c.DrawChar1x(21*16+8,BARPOS_Y+8,(byte)179,(byte)15);
				break;
			case 5: // pusher info
				if(isSticky)
				{
					c.DrawString1x(12*16+8,BARPOS_Y,"Normal",15);
					c.DrawString1x(12*16+8,BARPOS_Y+8,"Sticky",240);
				} else
				{
					c.DrawString1x(12*16+8,BARPOS_Y,"Normal",240);
					c.DrawString1x(12*16+8,BARPOS_Y+8,"Sticky",15);
				}
				break;
			default:
				break;
		}
		c.DrawChar(30*16,BARPOS_Y,(byte)1,(byte)12);
		c.DrawChar(31*16,BARPOS_Y,(byte)'C',(byte)9);
	}
	
	public int addPlayer(int id, int scrx, int scry, String name, byte ch, byte co)
	{
		players[id] = new Player(scrx,scry,ch,co,name);
		return id;
	}

	public void removePlayer(int id)
	{
		players[id] = null;
	}
}
