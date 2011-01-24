import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import java.lang.*;
import java.text.*;

public class CraftrGame extends JComponent
implements MouseListener, MouseMotionListener, KeyListener, ComponentListener, FocusListener
{
	public CraftrCanvas canvas;
	public JFrame window;
	public JApplet applet;
	public boolean gameOn;
	public static Random rand = new Random();
	
	public CraftrMap map;
	public CraftrPlayer players[] = new CraftrPlayer[256];
	
	public Date told = new Date();
	public Date tnew;
	public int fps = 0;
	public int ix = 0;
	public int iy = 0;
	public int mx = -1;
	public int my = -1;
	public int oldmx = -1;
	public int oldmy = -1;
	public int lpx = 0;
	public int lpy = 0;
	public long frame = 0;
	public long fold = 0;
	public int waitTime = 0;
	public int nagle=0;
	public boolean isApplet;
	public int mb = 0;
	public int oldmb = 0;
	public int ev_no,ev_1,ev_2,ev_3;
	public boolean blockChange = false;
	public boolean playerChange = false;
	public boolean mouseChange = false;
	public boolean netChange = false;
	public boolean multiplayer;
	public boolean canMousePress;
	public boolean isShift = false;
	public CraftrMapThread cmt;
	public int cmtsp=30;
	public CraftrConfig config;
	public CraftrNet net;
	public CraftrGameScreen gs;
	public CraftrInScreen is;
	public CraftrSound audio;
	public boolean[] keyHeld;
	public int netThreadRequest = 0;
	public int key_up = KeyEvent.VK_UP;
	public int key_left = KeyEvent.VK_LEFT;
	public int key_right = KeyEvent.VK_RIGHT;
	public int key_down = KeyEvent.VK_DOWN;
	public int kim = 0;
	public int scm = 0;
	public int scm2 = 0;
	public void playSound(int tx, int ty, int val)
	{
		int x=players[255].px-tx;
		int y=players[255].py-ty;
		audio.playNote(x,y,val,1.0);
	}
	public void playSample(int tx, int ty, int val)
	{
		int x=players[255].px-tx;
		int y=players[255].py-ty;
		audio.playSampleByNumber(x,y,val,1.0);
	}
	public String getVersion()
	{
		return "0.0.9";
	}
	public CraftrGame()
	{
		audio = new CraftrSound();
		isApplet = false;
		File sdchk = new File(System.getProperty("user.home") + "/.64pixels");
		if(!sdchk.exists()) sdchk.mkdir();
		window = new JFrame("64pixels " + getVersion());
		gameOn = true;
		map = new CraftrMap(64);
		map.game = this;
		map.saveDir = System.getProperty("user.home") + "/.64pixels/";
		players[255] = new CraftrPlayer(0,0);
		canMousePress = true;
		config = new CraftrConfig(map.saveDir + "config.txt");
		cmt = new CraftrMapThread(map);
		gs = new CraftrGameScreen(null);	
		loadConfig();
		canvas = new CraftrCanvas(scm);
		canvas.scm2=scm2;
		gs.c=canvas;
		canvas.cs = (CraftrScreen)gs;
		if(cmtsp>0) cmt.speed=(1000/cmtsp);
		else cmt.speed=0;
		keyHeld = new boolean[4];
	}
	public CraftrGame(JApplet ja)
	{
		audio = new CraftrSound();
		isApplet = true;
		applet = ja;
		map = new CraftrMap(64);
		map.game = this;
		try
		{
			File sdchk = new File(System.getProperty("user.home") + "/.64pixels");
			if(!sdchk.exists()) sdchk.mkdir();
			map.saveDir = System.getProperty("user.home") + "/.64pixels/";
		}
		catch(Exception e)
		{
			System.out.println("Cannot use default saveDir due to exception stuff");
			map.saveDir = "";
		}
		gameOn = true;
		players[255] = new CraftrPlayer(0,0);
		canMousePress = true;
		config = new CraftrConfig(map.saveDir + "config.txt");
		cmt = new CraftrMapThread(map);
		gs = new CraftrGameScreen(null);
		loadConfig();
		canvas = new CraftrCanvas(scm);
		canvas.scm2=scm2;
		gs.c=canvas;
		canvas.cs = (CraftrScreen)gs;
		if(cmtsp>0) cmt.speed=(1000/cmtsp);
		else cmt.speed=0;
		keyHeld = new boolean[4];
	}
	
	public void focusLost(FocusEvent ce)
	{
		keyHeld[0]=false;
		keyHeld[1]=false;
		keyHeld[2]=false;
		keyHeld[3]=false;
	}
	public void focusGained(FocusEvent ce) {}
	
	public void componentHidden(ComponentEvent ce) {}
	public void componentShown(ComponentEvent ce) {}
	public void componentMoved(ComponentEvent ce) {}
	public void componentResized(ComponentEvent ce)
	{
		canvas.scale(window.getRootPane().getWidth(),window.getRootPane().getHeight());
	}

	public String getPassword()
	{
		return getData("Input password:",16);
	}

	public String getData(String name,int len)
	{
		is = new CraftrInScreen(canvas,1,name);
		canvas.cs = (CraftrScreen)is;
		is.maxLen=len;
		loopInScreen();
		String t = is.inString;
		canvas.cs = (CraftrScreen)gs;
		is = null;
		return t;
	}
	
	public void saveConfig()
	{
		try
		{
			NumberFormat nf = NumberFormat.getNumberInstance();
			BufferedWriter out = new BufferedWriter(new FileWriter(map.saveDir + "config.txt"));
			String s = "";
			if(players[255].pchr != (byte)2)
			{
				s = "player-char=" + players[255].pchr;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(players[255].pcol != (byte)31)
			{
				s = "player-color=" + players[255].pcol;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(map.cachesize != 64)
			{
				s = "map-cache-size=" + map.cachesize;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(players[255].name != "You")
			{
				s = "player-name=" + players[255].name;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(map.genMode != 0)
			{
				s = "mapgen-mode=" + map.genMode;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(kim>0)
			{
				s = "wsad-mode=" + kim;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(nagle>0)
			{
				s = "use-nagle=" + nagle;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(cmtsp!=30)
			{
				s = "physics-speed=";
				if(cmtsp==0) s += "max";
				else s += cmtsp;
				out.write(s,0,s.length());
				out.newLine();
			}
			s = "drawn-type=" + gs.drawType;
			out.write(s,0,s.length());
			out.newLine();
			s = "drawn-char=" + gs.drawChr;
			out.write(s,0,s.length());
			out.newLine();
			s = "drawn-color=" + gs.drawCol;
			out.write(s,0,s.length());
			out.newLine();
			if(!multiplayer)
			{
				lpx = players[255].px;
				lpy = players[255].py;
			}
			if(scm!=0)
			{
				s = "char-scaler="+ scm;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(scm2!=0)
			{
				s = "bg-scaler="+ scm2;
				out.write(s,0,s.length());
				out.newLine();
			}
			s = "player-x=" + lpx;
			out.write(s,0,s.length());
			out.newLine();
			s = "player-y=" + lpy;
			out.write(s,0,s.length());
			out.close();	
		}
		catch (Exception e)
		{
			System.out.println("Error reading config data! " + e.getMessage());
		}
	}
	
	public void loadConfig()
	{
		try
		{
			NumberFormat nf = NumberFormat.getNumberInstance();
			for(int i=0;i<config.keys;i++)
			{
				String key = config.key[i];
				String val = config.value[i];
				System.out.println("Config key found: " + key);
				if(key.contains("drawn-char"))
				{
					gs.drawChr = nf.parse(val).byteValue();
					gs.chrBarOff = gs.drawChr&(~15);
				}
				else if(key.contains("drawn-color"))
				{
					gs.drawCol = nf.parse(val).byteValue();
				}
				else if(key.contains("drawn-type"))
				{
					gs.drawType = nf.parse(val).byteValue();
					if(gs.drawType>map.maxType) gs.drawType = 0;
				}
				else if(key.contains("player-char"))
				{
					players[255].pchr = nf.parse(val).byteValue();
				}
				else if(key.contains("mapgen-mode"))
				{
					map.genMode = nf.parse(val).byteValue();
				}
				else if(key.contains("player-name"))
				{
					players[255].name = val;
				}
				else if(key.contains("player-color"))
				{
					players[255].pcol = nf.parse(val).byteValue();
				}
				else if(key.contains("map-cache-length"))
				{
					map.resizeChunks(nf.parse(val).intValue());
				}
				if(key.contains("player-x"))
				{
					players[255].px = nf.parse(val).intValue();
					lpx = players[255].px;
				}
				else if(key.contains("player-y"))
				{
					players[255].py = nf.parse(val).intValue();
					lpy = players[255].py;
				}
				else if(key.contains("wsad-mode"))
				{
					kim = nf.parse(val).intValue();
					if(kim==1)
					{
						key_up = KeyEvent.VK_W;
						key_down = KeyEvent.VK_S;
						key_left = KeyEvent.VK_A;
						key_right = KeyEvent.VK_D;
					}
				}
				else if(key.contains("use-nagle"))
				{
					nagle = nf.parse(val).intValue();
				}
				else if(key.contains("physics-speed"))
				{
					if(val.equals("max"))
					{
						cmtsp = 0;
					}
					else
					{
						cmtsp = nf.parse(val).intValue();
						if(cmtsp<0) cmtsp=30;
						else if(cmtsp>1000) cmtsp=0;
					}
				}
				else if(key.contains("char-scaler"))
				{
					if(val.equals("scale2x"))
					{
						scm=1;
					}
					else
					{
						scm = nf.parse(val).intValue();
						if(scm<0 || scm>1) scm=0;
					}
				}
				else if(key.contains("bg-scaler"))
				{
					if(val.equals("nearest-neighbor") || val.equals("nearest-neighbour"))
					{
						scm2=1;
					}
					else if(val.equals("bilinear"))
					{
						scm2=0;
					}
					else
					{
						scm2 = nf.parse(val).intValue();
						if(scm2<0 || scm2>1) scm2=0;
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error reading config data! " + e.getMessage());
		}
	}
	public byte randByte()
	{
		return (byte)rand.nextInt(256);
	}
	
	public void mouseEntered(MouseEvent ev) {}
	public void mouseExited(MouseEvent ev) {}
	public void mouseClicked(MouseEvent ev) {} // this one sucks
	public void mousePressed(MouseEvent ev)
	{
		mb = ev.getButton();
		updateMouse(ev.getX(),ev.getY());
		ev_no = ev.NOBUTTON;
		ev_1 = ev.BUTTON1;
		ev_2 = ev.BUTTON2;
		ev_3 = ev.BUTTON3;
		    if (insideRect(mx,my,7*16+8,gs.BARPOS_Y,8,8)) // type, up
			{
				gs.drawType = (gs.drawType-1);
				if(gs.drawType < 0) gs.drawType = map.maxType;
			} else if (insideRect(mx,my,7*16+8,gs.BARPOS_Y+8,8,8)) // type, down
			{
				gs.drawType = (gs.drawType+1);
				if(gs.drawType > map.maxType) gs.drawType = 0;
			} else if (insideRect(mx,my,8*16+8,gs.BARPOS_Y,24,8)) // mode, chr
			{
				gs.barselMode = 1;
			} else if (insideRect(mx,my,8*16+8,gs.BARPOS_Y+8,24,8)) // mode, col
			{
				gs.barselMode = 2;
			} else if(gs.cwOpen && insideRect(mx,my,(gs.cw.x+gs.cw.w-1)<<3,gs.cw.y<<3,8,8))
			{
				// close button, any window
				gs.cwOpen = false;
				canMousePress = false;
			} else if(gs.cwOpen && insideRect(mx,my,(gs.cw.x+1)<<3,(gs.cw.y+1)<<3,(gs.cw.w-2)<<3,(gs.cw.h-2)<<3))
			{
				switch(gs.cw.type)
				{
					case 1: // char selecting, char window only
						gs.drawChr = (((mx-((gs.cw.x+1)<<3))>>3)&31) | (((my-((gs.cw.y+1)<<3))<<2)&224);
						gs.chrBarOff = gs.drawChr&(~15);					
						break;
					case 2:
						gs.drawCol = (((mx-((gs.cw.x+1)<<3))>>3)&15) | (((my-((gs.cw.y+1)<<3))<<1)&240);
						break;
					case 3:
						if(insideRect(mx,my,(gs.cw.x+2)<<3,(gs.cw.y+2)<<3,(gs.cw.w-4)<<3,(gs.cw.h-4)<<3))
						{
							int ix = (mx-((gs.cw.x+2)<<3))>>4;
							int iy = (my-((gs.cw.y+2)<<3))>>4;
							int ip = ix+iy*4;
							gs.drawType = gs.cw.recBlockType[ip];
							gs.drawChr = gs.cw.recBlockChr[ip];
							gs.chrBarOff = gs.drawChr&(~15);
							gs.drawCol = gs.cw.recBlockCol[ip];
							gs.cwOpen = false;
							canMousePress = false;
							mouseChange = true;
						}
					default:
						break;
				}
			}
			else if(insideRect(mx,my,30*16,gs.BARPOS_Y,16,16))
			{
				if(gs.cw.type == 1) gs.cwOpen = !gs.cwOpen;
				else gs.cwOpen = true;
				gs.cw.type = 1;
			}
			else if(insideRect(mx,my,31*16,gs.BARPOS_Y,16,16))
			{
				if(gs.cw.type == 2) gs.cwOpen = !gs.cwOpen;
				else gs.cwOpen = true;
				gs.cw.type = 2;
			}
			else if (gs.drawType == 2)
			{
				if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,128,16))
				{
					gs.drawCol = ((mx-(12*16+8))>>4);
				}
			}
			else if (gs.barselMode == 1 && gs.drawType == 3)
			{
				if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,64,16))
				{
					gs.drawChr= 24+((mx-(12*16+8))>>4);
				}
			}
			else if (gs.barselMode == 1) // checkings, chr
			{
				if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,256,16))
				{
					gs.drawChr = ((mx-(12*16+8))>>4)+gs.chrBarOff;
					gs.chrBarOff = gs.drawChr&(~15);
				}
				else if(insideRect(mx,my,28*16+8,gs.BARPOS_Y,8,8))
				{
					gs.chrBarOff -= 16;
					if(gs.chrBarOff<0) gs.chrBarOff = 240;
				}
				else if(insideRect(mx,my,28*16+8,gs.BARPOS_Y+8,8,8))
				{
					gs.chrBarOff += 16;
					if(gs.chrBarOff>240) gs.chrBarOff=0;
				}
			} else if (gs.barselMode == 2) // checkings, col
			{
				// insideRect!
				if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,128,16))
				{
					int colChoose = (mx-(12*16+8))>>3;
					int colMode = my-gs.BARPOS_Y;
					if(colMode>7) // FG
					{
						gs.drawCol = (gs.drawCol&240)|(colChoose&15);
					} else // BG
					{
						gs.drawCol = (gs.drawCol&15)|((colChoose&15)<<4);
					}
				}
			}
			processMouse();
	}
	public void mouseReleased(MouseEvent ev) { mb = ev_no; canMousePress = true;}

	public void mouseMoved(MouseEvent ev) {
		updateMouse(ev.getX(),ev.getY());//System.out.println(mx + ", " + my);
	}
	public void mouseDragged(MouseEvent ev) { updateMouse(ev.getX(),ev.getY()); } // this can be quite handy
	
	public void updateMouse(int umx, int umy)
	{
		mx=(int)(umx/canvas.scaleX);
		my=(int)(umy/canvas.scaleY);
		if(mx >= 0 && mx < gs.WIDTH && my >= 0 && my < (gs.GRID_H<<4))
		{
			int tx = (players[255].px+(mx>>4))-15;
			int ty = (players[255].py+(my>>4))-12;
			gs.hov_type=map.getBlock(tx,ty)[0];
			gs.hov_par=map.getBlock(tx,ty)[1];
		}
	}	
	public void processMouse()
	{
		if(mb != ev_no && canMousePress)
		{
			if(insideRect(mx,my,0,0,canvas.WIDTH,(canvas.GRID_H<<4)) && (!gs.cwOpen || !insideRect(mx,my,gs.cw.x<<3,gs.cw.y<<3,gs.cw.w<<3,gs.cw.h<<3)))
			{
				for(int i=0;i<256;i++)
				{
					if(gs.players[i] != null && gs.players[i].px == mx>>4 && gs.players[i].py == my>>4) return;
				}
				byte[] tmparr = new byte[4];
				if(mb == ev_1)
				{
					tmparr[0] = (byte)gs.drawType;
					tmparr[1] = (byte)0;
					tmparr[2] = (byte)gs.drawChr;
					tmparr[3] = (byte)gs.drawCol;
				}
				if(mb == ev_2)
				{
					byte[] tmpg = map.getBlock(players[255].px-15+(mx>>4),players[255].py-12+(my>>4));
					gs.drawChr = tmpg[2];
					gs.chrBarOff = gs.drawChr&(~15);
					gs.drawCol = tmpg[3];
					gs.drawType = tmpg[0];
					gs.cw.addRecBlock(tmpg[0],tmpg[2],tmpg[3]);
				}
				else if(oldmb != mb || (mx>>4 != oldmx>>4 || my>>4 != oldmy>>4))
				{
					mouseChange = true;
					oldmx = mx;
					oldmy = my;
					oldmb = mb;
					int ttx = players[255].px-15+(mx>>4);
					int tty = players[255].py-12+(my>>4);
					if(!multiplayer) synchronized(map.blockcheck)
					{
						map.blockcheck.add(new CraftrBlockPos(ttx,tty));
						for(int i=0;i<4;i++)
						{
							map.blockcheck.add(new CraftrBlockPos(ttx+map.xMovement[i],tty+map.yMovement[i]));
						}
					}
					if(gs.drawType==2 && mb == ev_1)
					{
						tmparr[3]=(byte)(tmparr[3]&7);
						tmparr[0]=(byte)2;
					}
					gs.cw.addRecBlock(tmparr[0],tmparr[2],tmparr[3]);
					map.setBlock(ttx,tty,tmparr);
					blockChange = true;
					if(mb == ev_1 || mb == ev_3)
					{
						byte[] t = map.getBlock(ttx,tty);
						map.setBlock(ttx,tty,t[0],t[1],(byte)map.updateLooks(ttx,tty,t[2]),t[3]);
						for(int i=0;i<4;i++)
						{
							t = map.getBlock(ttx+map.xMovement[i],tty+map.yMovement[i]);
							map.setBlock(ttx+map.xMovement[i],tty+map.yMovement[i],t[0],t[1],(byte)map.updateLooks(ttx+map.xMovement[i],tty+map.yMovement[i],t[2]),t[3]);
						}
					}
					if(multiplayer)
					{
						net.sendBlock(ttx,tty,tmparr[0],tmparr[2],tmparr[3]);
					}
					updateMouse((int)(mx*canvas.scaleX),(int)(my*canvas.scaleY));
				}
			}
		}
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
	/*
	public void shoot(int dir)
	{
		int sx=players[255].px+map.xMovement[dir];
		int sy=players[255].py+map.yMovement[dir];
		map.setBlock(sx,sy,(byte)7,(byte)dir,(byte)248,(byte)15);
		blockChange=true;
		map.addbc(new CraftrBlockPos(sx,sy));
	}
	*/
	public void keyTyped(KeyEvent ev) {} // this one sucks even more
	public void keyPressed(KeyEvent ev)
	{
		if(is != null)
		{
			is.parseKey(ev);
			return;
		}
		int kc = ev.getKeyCode();
		if(is == null && gs.barType == 0)
		{
			if (gs.cwOpen && gs.cw.type==1) {
				char chr = ev.getKeyChar();
				if(chr >= 32 && chr <= 127)
				{
					gs.drawChr=(byte)chr;
					gs.chrBarOff = gs.drawChr&(~15);
					mouseChange=true;
				}
				else if(kc==KeyEvent.VK_ESCAPE)
				{
					if(gs.cwOpen)
					{
						gs.cwOpen = false;
					}
					mouseChange=true;
				}
			}
/*
			else if(ev.isShiftDown()&&kc==key_left)
					shoot(0);
			else if(ev.isShiftDown()&&kc==key_right)
					shoot(1);
			else if(ev.isShiftDown()&&kc==key_up)
					shoot(2);
			else if(ev.isShiftDown()&&kc==key_down)
					shoot(3);
*/
			else if(kc==key_left)
				keyHeld[1] = true;
			else if(kc==key_right)
				keyHeld[2] = true;
			else if(kc==key_up)
				keyHeld[0] = true;
			else if(kc==key_down)
				keyHeld[3] = true;
			else
			{
				switch(kc)
					{
						case KeyEvent.VK_F4:
							canvas.screenshot(map.saveDir + "scr" + System.currentTimeMillis() + ".bmp");
							break;
						case KeyEvent.VK_P:
							System.out.println("player pos: x = " + players[255].px + ", y = " + players[255].py + ".");
							waitTime=2;
							break;
						case KeyEvent.VK_T:
							if(multiplayer)
							{
								gs.barType = 1;
								gs.chatMsg="";
								mouseChange = true;
							}
							break;
						case KeyEvent.VK_R:
							if(multiplayer && (players[255].px != 0 || players[255].py != 0))
							{
								net.respawnRequest();
							}
							break;
						case KeyEvent.VK_ESCAPE:
							if(gs.cwOpen)
							{
								gs.cwOpen = false;
							}
							mouseChange=true;
							break;
						case KeyEvent.VK_F:
							if(!multiplayer||net.isOp)
							{
								mouseChange=true;
								gs.viewFloorsMode=!gs.viewFloorsMode;
							}
							break;
						case KeyEvent.VK_B:
							if(!gs.cwOpen)
							{
								gs.cwOpen=true;
								gs.cw.type=3;
							}
							else if(gs.cw.type==3)
							{
								gs.cwOpen=false;
							}
							else
							{
								gs.cw.type=3;
							}
							mouseChange=true;
							break;
						default:
							break;
					}
			}
		} else if(gs.barType==1) {
			if(kc == KeyEvent.VK_BACK_SPACE && gs.chatMsg.length() > 0)
			{
				gs.chatMsg = gs.chatMsg.substring(0,gs.chatMsg.length()-1);
				mouseChange = true;
			} else if(kc == KeyEvent.VK_ENTER)
			{
				if(!gs.chatMsg.trim().equals(""))
				{
					net.sendChatMsg(gs.chatMsg);
				}
				gs.chatMsg = "";
				mouseChange = true;
				gs.barType=0;
			} else if(kc == KeyEvent.VK_ESCAPE)
			{
				gs.barType = 0;
				mouseChange = true;
			}
			else if (gs.chatMsg.length()<120)
			{
				char chr = ev.getKeyChar();
				if(chr >= 32 && chr <= 127) gs.chatMsg += chr;
				mouseChange = true;
			}
		}
	}
	public void keyReleased(KeyEvent ev)
	{
		int kc=ev.getKeyCode();
		if(kc==key_left)
				keyHeld[1] = false;
		else if(kc==key_right)
				keyHeld[2] = false;
		else if(kc==key_up)
				keyHeld[0] = false;
		else if(kc==key_down)
				keyHeld[3] = false;
	}
	public int movePlayer(int dpx, int dpy)
	{
		int px = players[255].px+dpx;
		int py = players[255].py+dpy;
		if(map.isEmpty(px,py))
		{
			if(multiplayer) net.playerMove(dpx,dpy);
			else
			{
				map.setPlayer(players[255].px,players[255].py,0);
				map.setPlayer(px,py,1);
			}
			oldmx=-1;
			oldmy=-1;
			players[255].move(px,py);
			playerChange = true;
			return 3;
		}
		return waitTime;
	}
	public void spawnPlayer(int cx, int cy, int id)
	{
		CraftrChunk pc;
		try
		{
			pc = map.grabChunk(cx,cy);
		}
		catch(Exception e)
		{
			System.out.println("spawnPlayer: no chunk memory found, most likely");
			return;
		}
		for(int i=0;i<64;i++)
		{
			for(int j=0;j<64;j++)
			{
				if(pc.getBlockType(i,j) == 0) { players[id].px = cx+i; players[id].py = cy+j; return; }
			}
		}
	}
	
	public void render()
	{
		// oh my...
		
		// This code initalizes Screen Chunk X/Y, Pixel X/Y;
		int px = players[255].px;
		int py = players[255].py;
		int sx = px-15;
		int sy = py-12;
		int scx = sx>>6;
		int scy = sy>>6;
		int spx = sx&63;
		int spy = sy&63;
		try
		{
			if(spx<32 && spy<39)
			{
				CraftrChunk mychunk = map.grabChunk(scx,scy);
				// One chunk required
				// Optimizations galore
				for(int iy=0;iy<25;iy++)
				{
					System.arraycopy(mychunk.chr2,spx+((spy+iy)<<6),gs.scr_chr,iy*canvas.FULLGRID_W,32);
					System.arraycopy(mychunk.col2,spx+((spy+iy)<<6),gs.scr_col,iy*canvas.FULLGRID_W,32);			
					System.arraycopy(mychunk.chr,4096+spx+((spy+iy)<<6),gs.f_chr,iy*canvas.FULLGRID_W,32);
					System.arraycopy(mychunk.col,4096+spx+((spy+iy)<<6),gs.f_col,iy*canvas.FULLGRID_W,32);
					System.arraycopy(mychunk.type,spx+((spy+iy)<<6),gs.scr_typ,iy*canvas.FULLGRID_W,32);
				}
			} else {
				// This is a mess.
				// More than one chunk required.
				CraftrChunk[][] surr = new CraftrChunk[2][2];
				if(spx>=32 && spy>=39)
				{
					map.setUsed(scx,scy);
					map.setUsed(scx+1,scy);
					map.setUsed(scx,scy+1);
					map.setUsed(scx+1,scy+1);
					map.clearAllUsed();
					// X and Y take 2 chunks
					surr[0][0] = map.grabChunk(scx,scy);
					surr[1][0] = map.grabChunk(scx+1,scy);
					surr[0][1] = map.grabChunk(scx,scy+1);
					surr[1][1] = map.grabChunk(scx+1,scy+1);
				}
				else if(spx>=32)
				{
					map.setUsed(scx,scy);
					map.setUsed(scx+1,scy);
					map.clearAllUsed();
					surr[0][0] = map.grabChunk(scx,scy);
					surr[1][0] = map.grabChunk(scx+1,scy);
				}
				else if(spy>=39)
				{
					map.setUsed(scx,scy);
					map.setUsed(scx,scy+1);
					map.clearAllUsed();
					surr[0][0] = map.grabChunk(scx,scy);
					surr[0][1] = map.grabChunk(scx,scy+1);
				}
				else
				{
					System.out.println("[SEVERE] render: What are you doing in ELSE if the chunk is one-chunk!? :O");
					System.exit(1);
				}
				int iylen = 64-spy;
				if(spx>=32)
				{
					int tspx = 64-spx;
					int tspx2 = 32-tspx;
					for(int iy=0; iy<25; iy++)
					{
						if(iy>=iylen)
						{
							System.arraycopy(surr[0][1].chr2,((iy-iylen)<<6)+spx,gs.scr_chr,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[0][1].col2,((iy-iylen)<<6)+spx,gs.scr_col,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[1][1].chr2,(iy-iylen)<<6,gs.scr_chr,(iy*canvas.FULLGRID_W)+tspx,tspx2);
							System.arraycopy(surr[1][1].col2,(iy-iylen)<<6,gs.scr_col,(iy*canvas.FULLGRID_W)+tspx,tspx2);
							System.arraycopy(surr[0][1].chr,4096+((iy-iylen)<<6)+spx,gs.f_chr,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[0][1].col,4096+((iy-iylen)<<6)+spx,gs.f_col,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[1][1].chr,4096+((iy-iylen)<<6),gs.f_chr,(iy*canvas.FULLGRID_W)+tspx,tspx2);
							System.arraycopy(surr[1][1].col,4096+((iy-iylen)<<6),gs.f_col,(iy*canvas.FULLGRID_W)+tspx,tspx2);
							System.arraycopy(surr[0][1].type,((iy-iylen)<<6)+spx,gs.scr_typ,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[1][1].type,((iy-iylen)<<6),gs.scr_typ,(iy*canvas.FULLGRID_W)+tspx,tspx2);
							
						}
						else
						{
							System.arraycopy(surr[0][0].chr2,((spy+iy)<<6)+spx,gs.scr_chr,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[0][0].col2,((spy+iy)<<6)+spx,gs.scr_col,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[1][0].chr2,(spy+iy)<<6,gs.scr_chr,(iy*canvas.FULLGRID_W)+tspx,tspx2);
							System.arraycopy(surr[1][0].col2,(spy+iy)<<6,gs.scr_col,(iy*canvas.FULLGRID_W)+tspx,tspx2);	
							System.arraycopy(surr[0][0].chr,4096+((spy+iy)<<6)+spx,gs.f_chr,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[0][0].col,4096+((spy+iy)<<6)+spx,gs.f_col,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[1][0].chr,4096+((spy+iy)<<6),gs.f_chr,(iy*canvas.FULLGRID_W)+tspx,tspx2);
							System.arraycopy(surr[1][0].col,4096+((spy+iy)<<6),gs.f_col,(iy*canvas.FULLGRID_W)+tspx,tspx2);	
							System.arraycopy(surr[0][0].type,((spy+iy)<<6)+spx,gs.scr_typ,iy*canvas.FULLGRID_W,tspx);
							System.arraycopy(surr[1][0].type,((spy+iy)<<6),gs.scr_typ,(iy*canvas.FULLGRID_W)+tspx,tspx2);
						}
					}
				}
				else {
					for(int iy=0; iy<25; iy++)
					{
						if(iy>=iylen)
						{
							System.arraycopy(surr[0][1].chr2,((iy-iylen)<<6)+spx,gs.scr_chr,iy*canvas.FULLGRID_W,32);
							System.arraycopy(surr[0][1].col2,((iy-iylen)<<6)+spx,gs.scr_col,iy*canvas.FULLGRID_W,32);
							System.arraycopy(surr[0][1].chr,4096+((iy-iylen)<<6)+spx,gs.f_chr,iy*canvas.FULLGRID_W,32);
							System.arraycopy(surr[0][1].col,4096+((iy-iylen)<<6)+spx,gs.f_col,iy*canvas.FULLGRID_W,32);
							System.arraycopy(surr[0][1].type,((iy-iylen)<<6)+spx,gs.scr_typ,iy*canvas.FULLGRID_W,32);
						}
						else
						{
							System.arraycopy(surr[0][0].chr2,((spy+iy)<<6)+spx,gs.scr_chr,iy*canvas.FULLGRID_W,32);
							System.arraycopy(surr[0][0].col2,((spy+iy)<<6)+spx,gs.scr_col,iy*canvas.FULLGRID_W,32);
							System.arraycopy(surr[0][0].chr,4096+((spy+iy)<<6)+spx,gs.f_chr,iy*canvas.FULLGRID_W,32);
							System.arraycopy(surr[0][0].col,4096+((spy+iy)<<6)+spx,gs.f_col,iy*canvas.FULLGRID_W,32);
							System.arraycopy(surr[0][0].type,((spy+iy)<<6)+spx,gs.scr_typ,iy*canvas.FULLGRID_W,32);
						}
					}
				}
			}
			for (int i=0;i<256;i++)
			{
				if(!multiplayer && i<255) continue;
				if(players[i] == null)
				{
					gs.removePlayer(i);
					continue;
				}
				int tx = (players[i].px-players[255].px)+15;
				int ty = (players[i].py-players[255].py)+12;
				if(tx>=0 && ty>=0 && tx<32 && ty<25)
				{
					byte[] t = map.getBlock(players[i].px,players[i].py);
					if(t[0]!=8) gs.drawBlock(tx,ty,(byte)players[i].pchr,(byte)players[i].pcol);
					gs.addPlayer(i,tx,ty,players[i].name);
				}
				else gs.removePlayer(i);
			}
		}
		catch (Exception e)
		{
			System.out.println("[SEVERE] render exception: " + e.toString() + " | " + e.getMessage() + " | " + e.getCause());
			e.printStackTrace();
			System.exit(1);
		}
	}
	public void init()
	{
		// i know it's a hack but still
		if(isApplet)
		{
			applet.getContentPane().add(canvas);
			//appletwindow.pack();
		} else
		{
			window.add(canvas);
			window.pack(); // makes everything a nice size
			window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			window.setVisible(true);
		}
		tnew = new Date(told.getTime() + 1000L);
	}
	
	public void loopInScreen()
	{
		try
		{
			while(is.isRunning)
			{
				//keyPressedOld();
				canvas.draw(mx,my);
				if(waitTime>0) waitTime--;
				if(mx != oldmx || my != oldmy || mb != oldmb)
				{
					mouseChange = true;
					oldmx = mx;
					oldmy = my;
					oldmb = mb;
				}
				Thread.sleep(100);
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal loopInScreen error!");
			System.exit(1);
		}
	}
	public String configure()
	{
		is = new CraftrInScreen(canvas,2,"Select mode");
		String[] modes = new String[2];
		modes[0] = "Singleplayer";
		modes[1] = "Multiplayer";
		is.addStrings(modes);
		canvas.cs = (CraftrScreen)is;
		loopInScreen();
		String ostr = "";
		switch(is.inSel)
		{
			case 0:
				multiplayer = false;
				break;
			case 1:
				multiplayer = true;
				is = new CraftrInScreen(canvas,1,"Input address:");
				canvas.cs = (CraftrScreen)is;
				loopInScreen();
				ostr = is.inString;
				is = new CraftrInScreen(canvas,1,"Enter nickname:");
				canvas.cs = (CraftrScreen)is;
				loopInScreen();
				net.nick = is.inString;
				break;
		}
		canvas.cs = (CraftrScreen)gs;
		is = null;
		return ostr;
	}
	
	public void parseNTR()
	{
		switch(netThreadRequest)
		{
			case 1:
				net.sendDecrypt(getPassword());
				break;
			default:
				break;
		}
		netThreadRequest=0;
	}
	public void start(String[] args)
	{
		if(!isApplet)
		{
			window.getRootPane().addComponentListener(this);
			window.addFocusListener(this);
			window.addKeyListener(this);
			addKeyListener(this);
			addComponentListener(this);
		}
		else
		{
			applet.setFocusable(true);
			applet.addKeyListener(this);
		}
		net = new CraftrNet();
		String thost = configure();
		if(!multiplayer)
		{
			gs.addChatMsg("you're running 64pixels " + getVersion(),0);
			gs.addChatMsg("created by asiekierka.",0);
			Thread tmap = new Thread(cmt);
			tmap.start();
		}
		else
		{
			System.out.println("Connecting...");
			net.connect(CraftrConvert.getHost(thost),CraftrConvert.getPort(thost), nagle);
			System.out.println("Connected! Logging in...");
			map.net = net;
			net.gaa = this;
			map.multiplayer = true;
			Thread t1 = new Thread(net);
			t1.start();
			net.chunkRequest(0,0);
		}
		if(!isApplet)
		{
			window.getRootPane().addMouseListener(this);
			window.getRootPane().addMouseMotionListener(this);
			addMouseListener(this);
			addMouseMotionListener(this);
		}
		else
		{
			applet.getRootPane().addMouseListener(this);
			applet.getRootPane().addMouseMotionListener(this);
		}
		long wpso = 0;
		while(gameOn)
		{
			try
			{
			Thread.sleep(33);
			}
			catch (Exception e) { }
			if(waitTime==0)
			{
				if(keyHeld[0]==true)
				{
					waitTime=movePlayer(0,-1);
				}
				if(keyHeld[1]==true)
				{
					waitTime=movePlayer(-1,0);
				}
				if(keyHeld[2]==true)
				{
					waitTime=movePlayer(1,0);
				}
				if(keyHeld[3]==true)
				{
					waitTime=movePlayer(0,1);
				}
			}
			if(waitTime>0) waitTime--;
			told = new Date();
			if(gs.drawType==7) gs.cw.isMelodium=true;
			else gs.cw.isMelodium=false;
			if(told.compareTo(tnew) >= 0)
			{
				fps = (int)(frame-fold);
				tnew = new Date(told.getTime() + 1000L);
				fold = frame;
				System.out.println(fps + " fps, physics runs at " + (cmt.wps-wpso) + "checks a second");
				wpso = cmt.wps;
			}
			if(multiplayer)
			{
				for(int i=0;i<256;i++)
				{
					if(players[i] != null && players[i].posChanged)
					{
						playerChange = true;
						players[i].posChanged = false;
					}
				}
				if(netThreadRequest>0) parseNTR();
			}
			else
			{
				if(map.blockcheck.size()>0) blockChange=true;
				playerChange = players[255].posChanged;
				players[255].posChanged = false;
			}
			if(!multiplayer || net.loginStage > 0)
			{
				processMouse();
				if(mx != oldmx || my != oldmy || mb != oldmb)
				{
					mouseChange = true;
					oldmx = mx;
					oldmy = my;
					oldmb = mb;
				}
				render();
				if(playerChange || blockChange || mouseChange || netChange)
				{
					netChange = false;
					canvas.draw(mx,my);
					playerChange = false;
					blockChange = false;
					mouseChange = false;
				}
			}
			frame++;
		}
	}
	public void stop()
	{
		gameOn = false;
	}
	public void end()
	{
		if(!multiplayer)
		{
		System.out.print("Saving... ");
		for(int i=0;i<map.chunks.length;i++)
		{
			if(map.chunks[i].isSet || map.chunks[i].isUsed)
			{
				map.saveChunkFile(i);
			}
		}
		}
		else
		{
			//net.sendPackets();
			net.sockClose();
		}
		saveConfig();
		audio.kill();
		//conf.SaveConfig(map.saveDir + "config.txt");
		System.out.println("Done!");
	}
	public void finalize()
	{
		end();
	}
}
