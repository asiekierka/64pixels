package client;
import common.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import java.lang.*;
import java.text.*;
import java.net.*;

public class CraftrGame extends JComponent
implements MouseListener, MouseMotionListener, KeyListener, ComponentListener, FocusListener, CraftrGameShim
{
	public CraftrCanvas canvas;
	public JFrame window;
	public JApplet applet;
	public boolean gameOn;
	public static Random rand = new Random();

	public CraftrMap map;
	public CraftrPlayer players[] = new CraftrPlayer[256];
	
	public boolean isApplet;
	public boolean blockChange = false;
	public boolean playerChange = false;
	public boolean mouseChange = false;
	public boolean netChange = false;
	public boolean multiplayer;
	public boolean isKick = false;
	public CraftrMapThread cmt;
	public int cmtsp=30;
	public int overhead=0;
	public CraftrConfig config;
	public CraftrNet net;
	public CraftrGameScreen gs;
	public CraftrInScreen is;
	public CraftrSound audio;
	public boolean advMouseMode = false;
	public int netThreadRequest = 0;
	public int kim = 0;
	public String isKickS;
	public boolean skipConfig = false;
	public boolean muted = false;
	public boolean raycasting = false;

	private int fps = 0;
	private long frame = 0;
	private long fold = 0;
	private int waitTime = 0;
	private int nagle=0;
	private int chrArrowWaiter = 0;
	private boolean[] keyHeld;
	private long wpso = 0;
	private Date overdate;
	private Date told = new Date();
	private Date tnew;
	private int ix = 0;
	private int iy = 0;
	private int mx = -1;
	private int my = -1;
	private int oldmx = -1;
	private int oldmy = -1;
	private int lpx = 0;
	private int lpy = 0;
	private boolean canMousePress;
	private boolean isShift = false;
	private int mb = 0;
	private int oldmb = 0;
	private int ev_no,ev_1,ev_2,ev_3;
	private int key_up = KeyEvent.VK_UP;
	private int key_left = KeyEvent.VK_LEFT;
	private int key_right = KeyEvent.VK_RIGHT;
	private int key_down = KeyEvent.VK_DOWN;
	private static final byte[] extendDir = { 30, 31, 16, 17 };
	private CraftrGameThread gt;

	public void playSound(int tx, int ty, int val)
	{
		if(muted) return;
		if(val>=256)
		{
			playSample(tx,ty,val-256);
			return;
		}		
		int x=players[255].px-tx;
		int y=players[255].py-ty;
		audio.playNote(x,y,val,1.0);
	}
	public void playSample(int tx, int ty, int val)
	{
		if(muted) return;
		int x=players[255].px-tx;
		int y=players[255].py-ty;
		audio.playSampleByNumber(x,y,val,1.0);
	}
	public void changeKeyMode(int t)
	{
		kim=t;
		if(t==0)
		{
			key_up = KeyEvent.VK_UP;
			key_left = KeyEvent.VK_LEFT;
			key_right = KeyEvent.VK_RIGHT;
			key_down = KeyEvent.VK_DOWN;
		} else {
			key_up = KeyEvent.VK_W;
			key_down = KeyEvent.VK_S;
			key_left = KeyEvent.VK_A;
			key_right = KeyEvent.VK_D;
		}
	}
	public static String getVersion()
	{
		return CraftrVersion.getVersionName();
	}
	public CraftrGame()
	{
		audio = new CraftrSound();
		isApplet = false;
		File sdchk = new File(System.getProperty("user.home") + "/.64pixels");
		if(!sdchk.exists()) sdchk.mkdir();
		window = new JFrame("64pixels " + getVersion());
		gameOn = true;
		map = new CraftrMap(false,64);
		map.game = this;
		map.saveDir = System.getProperty("user.home") + "/.64pixels/";
		players[255] = new CraftrPlayer(0,0);
		canMousePress = true;
		config = new CraftrConfig(map.saveDir + "config.txt");
		cmt = new CraftrMapThread(map);
		gs = new CraftrGameScreen(null);	
		loadConfig();
		canvas = new CraftrCanvas();
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
		map = new CraftrMap(false,64);
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
		canvas = new CraftrCanvas();
		gs.c=canvas;
		canvas.cs = (CraftrScreen)gs;
		if(cmtsp>0) cmt.speed=(1000/cmtsp);
		else cmt.speed=0;
		keyHeld = new boolean[4];
		String t3 = ja.getParameter("nick");
		String t4 = ja.getParameter("skip");
		if(t4!=null && !t4.equals("")) skipConfig = true;
		if(t3!=null && !t3.equals("")) players[255].name = t3;
	} 
	
	public void kickOut(String why)
	{
		isKick=true;
		isKickS = why;
	}
	public String escapeSlashes(String orig)
	{
		char[] temp = orig.toCharArray();
		String newS = "";
		for(int i=0;i<temp.length;i++)
		{
			if(temp[i]=='\\') i++;
			if(i<temp.length) newS+=temp[i];
		}
		return newS;
	}
	public boolean fetchSList()
	{
		URL u1;
		InputStream is = null;
		FileOutputStream fos;
		try
		{
			u1 = new URL("http://admin.64pixels.org/serverlist.php?asie=1");
			is = u1.openStream();
			fos = new FileOutputStream(map.saveDir + "slist.txt");
			int count = 1;
			while(count>0)
			{
				byte[] t = new byte[64];
				count=is.read(t,0,64);
				if(count>0)
				{
					System.out.println("read " + count + " bytes");
					fos.write(t,0,count);
				}
			}
		}
		catch(Exception e) { e.printStackTrace(); return false;}
		finally { try{is.close();}catch(Exception e){} }
		return true;
	}

	public void realKickOut()
	{
		CraftrKickScreen cks=new CraftrKickScreen(canvas,isKickS);
		canvas.cs=(CraftrScreen)cks;
		while(true)
		{
			canvas.draw(mx,my);
			try { Thread.sleep(100); } catch(Exception e) { }
		}
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
		is.minLen=1;
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
			for(int i=0;i<256;i++)
			{
				if(gs.drawChrA[i]!=1)
				{
					s = "dch|" + i + "=" + gs.drawChrA[i];
					out.write(s,0,s.length());
					out.newLine();
				}
				if(gs.drawColA[i]!=15)
				{
					s = "dco|" + i + "=" + gs.drawColA[i];
					out.write(s,0,s.length());
					out.newLine();
				}
			}
			s = "player-char=" + players[255].pchr;
			out.write(s,0,s.length());
			out.newLine();
			s = "player-color=" + players[255].pcol;
			out.write(s,0,s.length());
			out.newLine();
			if(map.cachesize != 64)
			{
				s = "map-cache-size=" + map.cachesize;
				out.write(s,0,s.length());
				out.newLine();
			}
			s = "player-name=" + players[255].name;
			out.write(s,0,s.length());
			out.newLine();
			if(map.genMode != 0)
			{
				s = "mapgen-mode=" + map.genMode;
				out.write(s,0,s.length());
				out.newLine();
			}
			s = "wsad-mode=" + kim;
			out.write(s,0,s.length());
			out.newLine();
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
			if(!multiplayer)
			{
				lpx = players[255].px;
				lpy = players[255].py;
			}
			if(gs.hideousPrompts)
			{
				s = "hideous-prompts=1";
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
			System.out.println("Error saving config data! " + e.getMessage());
		}
	}
	
	public void loadConfig()
	{
		try
		{
			NumberFormat nf = NumberFormat.getNumberInstance();
			for(int i=0;i<config.keys;i++)
			{
				String key = config.key[i].toLowerCase();
				String val = config.value[i];
				if(key.contains("drawn-type"))
				{
					gs.drawType = nf.parse(val).byteValue();
					if(gs.drawType>map.maxType || gs.drawType<-1) gs.drawType = 0;
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
					changeKeyMode(kim);
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
				else if(key.contains("dch|"))
				{
					String[] dchi = key.split("\\|");
					/* as | is a special character here, we need to escape it with \. *
					 *  but \ is also special so we escape THAT with another \		 */
					if(dchi.length==2)
					{
						gs.drawChrA[nf.parse(dchi[1]).intValue()]=nf.parse(val).intValue();
					}
				}
				else if(key.contains("dco|"))
				{
					String[] dcoi = key.split("\\|");
					if(dcoi.length==2)
					{
						gs.drawColA[nf.parse(dcoi[1]).intValue()]=nf.parse(val).intValue();
					}
				}
				else if(key.contains("hideous-prompts"))
				{
					if(nf.parse(val).intValue()>0) gs.hideousPrompts=true;
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error reading config data! " + e.getMessage());
		}
	}

	public void mouseEntered(MouseEvent ev) {}
	public void mouseExited(MouseEvent ev) {}
	public void mouseClicked(MouseEvent ev) {}

	public boolean isDragging = false;
	public boolean isConfig = true;
	public int dragX = 0;
	public int dragY = 0;
	public int dragID = 0;
	public void mousePressed(MouseEvent ev)
	{
		mb = ev.getButton();
		updateMouse(ev.getX(),ev.getY());
		ev_no = ev.NOBUTTON;
		ev_1 = ev.BUTTON1;
		ev_2 = ev.BUTTON2;
		ev_3 = ev.BUTTON3;
		advMouseMode = ev.isControlDown();
		if(isConfig)
		{
			processWindows();
			return;
		}
		if(isKick) return;
		if (insideRect(mx,my,7*16+8,gs.BARPOS_Y,8,8)) // type, up
		{
			gs.drawType = (gs.drawType-1);
			if(gs.drawType < -1) gs.drawType = map.maxType;
		} else if (insideRect(mx,my,7*16+8,gs.BARPOS_Y+8,8,8)) // type, down
		{
			gs.drawType = (gs.drawType+1);
			if(gs.drawType > map.maxType) gs.drawType = -1;
		}
		else if (insideRect(mx,my,7*16,gs.BARPOS_Y+8,8,8)) // T
		{
			gs.toggleWindow(4);
		}
		 else if (insideRect(mx,my,8*16+8,gs.BARPOS_Y,24,8)) // mode, chr
		{
			gs.barselMode = 1;
		} else if (insideRect(mx,my,8*16+8,gs.BARPOS_Y+8,24,8)) // mode, col
		{
			gs.barselMode = 2;
		} else if(insideRect(mx,my,30*16,gs.BARPOS_Y,16,16))
		{
			gs.toggleWindow(1);
		}
		else if(insideRect(mx,my,31*16,gs.BARPOS_Y,16,16))
		{
			gs.toggleWindow(2);
		}
		else if (gs.drawType == 2)
		{
			if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,128,16))
			{
				gs.sdrawCol((mx-(12*16+8))>>4);
			}
		}
		else if (gs.barselMode == 1 && gs.drawType == 3) // p-nand dir
		{
			if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,64,16))
			{
				gs.sdrawChr(24+((mx-(12*16+8))>>4));
			}
		}
		else if (gs.barselMode == 1 && gs.drawType == 15) // extend dir
		{
			if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,64,16))
			{
				gs.sdrawChr(extendDir[((mx-(12*16+8))>>4)]);
			}
		}
		else if (gs.barselMode == 1) // checkings, chr
		{
			if(insideRect(mx,my,13*16,gs.BARPOS_Y,256,16))
			{
				gs.sdrawChr(((mx-(13*16))>>4)+gs.chrBarOff);
			}
			else if(mb==ev_3 && insideRect(mx,my,12*16+8,gs.BARPOS_Y+1,8,14))
			{
				gs.chrBarOff -= 16;
				mouseChange=true;
				if(gs.chrBarOff<0) gs.chrBarOff += 256;
			}
			else if(mb==ev_3 && insideRect(mx,my,29*16,gs.BARPOS_Y+1,8,14))
			{
				gs.chrBarOff += 16;
				mouseChange=true;
				if(gs.chrBarOff>255) gs.chrBarOff -= 256;
			}
		} else if (gs.barselMode == 2) // checkings, col
		{
			if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,128,16))
			{
				int colChoose = (mx-(12*16+8))>>3;
				int colMode = my-gs.BARPOS_Y;
				if(colMode>7) // FG
				{
					gs.sdrawCol((gs.gdrawCol()&240)|(colChoose&15));
				} else // BG
				{
					gs.sdrawCol((gs.gdrawCol()&15)|((colChoose&15)<<4));
				}
			}
		}
		processWindows();
		processMouse();
	}
	public void mouseReleased(MouseEvent ev) { mb = ev_no; canMousePress = true; advMouseMode = false;}

	int confChr = 0;
	int confCol = 0; 
	public void processWindows()
	{
		ArrayList<CraftrWindow> w;
		if(isConfig) w=is.windows;
		else w=gs.windows;
		try
		{
			synchronized(w)
			{
				if(w.size()>0)
					for(CraftrWindow cw : w)
					{
						if(!isConfig && gs.obstructedWindow(cw,mx,my)) { }
						else if(isConfig && is.obstructedWindow(cw,mx,my)) { }
						else if(insideRect(mx,my,(cw.x+cw.w-1)<<3,cw.y<<3,8,8))
						{
							// close button, any window
							if(isConfig) is.toggleWindow(cw.type);
							else gs.toggleWindow(cw.type);
							canMousePress = false;
						} else if(insideRect(mx,my,(cw.x+1)<<3,(cw.y+1)<<3,(cw.w-2)<<3,(cw.h-2)<<3))
						{
							switch(cw.type)
							{
								case 1: // char selecting, char window only
									confChr = (((mx-((cw.x+1)<<3))>>3)%(cw.w-2)) + ((((my>>3)-(cw.y+1))%(cw.h-2))*(cw.w-2));
									if(confChr<=255)
									{
										gs.sdrawChr(confChr);
										gs.chrBarOff = confChr-8;
										if(gs.chrBarOff<0) gs.chrBarOff+=256;
										if(isConfig) is.getWindow(1).charChosen = confChr;
									}				
									break;
								case 2:
									confCol = (((mx-((cw.x+1)<<3))>>3)&15) | (((my-((cw.y+1)<<3))<<1)&240);
									gs.sdrawCol(confCol);
									if(isConfig) is.getWindow(2).colorChosen = confCol;
									break;
								case 3:
									if(insideRect(mx,my,(cw.x+2)<<3,(cw.y+2)<<3,(cw.w-4)<<3,(cw.h-4)<<3))
									{
										int ix = (mx-((cw.x+2)<<3))>>4;
										int iy = (my-((cw.y+2)<<3))>>4;
										int ip = ix+iy*4;
										gs.drawType = cw.recBlockType[ip];
										gs.sdrawChr(cw.recBlockChr[ip]);
										gs.chrBarOff = gs.gdrawChr()-8;
										if(gs.chrBarOff<0) gs.chrBarOff+=256;
										gs.sdrawCol(cw.recBlockCol[ip]);
										gs.toggleWindow(3);
										canMousePress = false;
										mouseChange = true;
									}
									break;
								case 4:
									gs.drawType=((my-((cw.y+1)<<3))>>3)-1;
									break;
								default:
									break;
							}
						} else if(insideRect(mx,my,cw.x<<3,cw.y<<3,cw.w<<3,cw.h<<3))
						{ // DRAGGING WINDOWS! :D
							dragX = (mx-(cw.x<<3))>>3;
							dragY = (my-(cw.y<<3))>>3;
							dragID = w.indexOf(cw);
							isDragging = true;
						}
					}
			}
		}
		catch(Exception e) { }
	}
	public void mouseMoved(MouseEvent ev) {
		updateMouse(ev.getX(),ev.getY());
		advMouseMode = ev.isControlDown();
	}
	public void mouseDragged(MouseEvent ev) { updateMouse(ev.getX(),ev.getY()); advMouseMode = ev.isControlDown(); } // this can be quite handy
	
	public void updateMouse(int umx, int umy)
	{
		mx=(int)(umx/canvas.scaleX);
		my=(int)(umy/canvas.scaleY);
		if(!isConfig && (mx >= 0 && mx < gs.WIDTH && my >= 0 && my < (gs.GRID_H<<4)))
		{
			int tx = (players[255].px+(mx>>4))-15;

			int ty = (players[255].py+(my>>4))-12;
			gs.hov_type=map.getBlock(tx,ty).getTypeWithVirtual();
		}
		if(isDragging)
		{
			ArrayList<CraftrWindow> w = gs.windows;
			if(isConfig) w = is.windows;
			synchronized(w)
			{
				CraftrWindow dcw = w.get(dragID);
				int dragRX = (mx-((dcw.x+dragX)<<3))>>3;
				int dragRY = (my-((dcw.y+dragY)<<3))>>3;
				dcw.x+=dragRX;
				dcw.y+=dragRY;
				w.set(dragID,dcw);
			}
			if(mb != ev_1) isDragging = false;
		}
	}	
	public void processMouse()
	{
		if(mb != ev_no && canMousePress && !isDragging)
		{
			if(insideRect(mx,my,0,0,canvas.WIDTH,(canvas.GRID_H<<4)) && !gs.inWindow(mx,my))
			{
				CraftrWindow cw = gs.getWindow(3);
				for(int i=0;i<256;i++)
				{
					if(gs.players[i] != null && gs.players[i].px == mx>>4 && gs.players[i].py == my>>4) return;
				}
				byte[] tmparr = new byte[4];
				if(mb == ev_1)
				{
					tmparr[0] = (byte)gs.drawType;
					tmparr[1] = (byte)0;
					tmparr[2] = (byte)gs.gdrawChr();
					tmparr[3] = (byte)gs.gdrawCol();
				}
				if(mb == ev_2)
				{
					CraftrBlock capturedBlock = map.getBlock(players[255].px-15+(mx>>4),players[255].py-12+(my>>4));
 					if(!advMouseMode) gs.drawType = capturedBlock.getTypeWithVirtual();
 					gs.sdrawChr(capturedBlock.getChar());
 					gs.sdrawCol(capturedBlock.getColor());
					gs.chrBarOff = gs.gdrawChr()-8;
					if(gs.chrBarOff<0) gs.chrBarOff+=256;
					if(cw!=null) cw.addRecBlock((byte)gs.drawType,(byte)gs.gdrawChr(),(byte)gs.gdrawCol());
				}
				else if(oldmb != mb || (mx>>4 != oldmx>>4 || my>>4 != oldmy>>4))
				{
					mouseChange = true;
					oldmx = mx;
					oldmy = my;
					oldmb = mb;
					int ttx = players[255].px-15+(mx>>4);
					int tty = players[255].py-12+(my>>4);
					if(!multiplayer) synchronized(map.physics)
					{
						map.physics.addBlockToCheck(new CraftrBlockPos(ttx,tty));
						for(int i=0;i<4;i++)
						{
							map.physics.addBlockToCheck(new CraftrBlockPos(ttx+map.xMovement[i],tty+map.yMovement[i]));
						}
					}
					if(mb == ev_1 && advMouseMode) tmparr[0] = (byte)map.getBlock(ttx,tty).getTypeWithVirtual();
					if(gs.drawType==2 && mb == ev_1)
					{
						tmparr[3]=(byte)(tmparr[3]&7);
						tmparr[0]=(byte)2;
					}
					blockChange=true;
					if(tmparr[0]==-1)
					{
						map.setPushable(ttx,tty,tmparr[2],tmparr[3]);
						map.setBlock(ttx,tty,(byte)0,(byte)0,(byte)0,(byte)0);
					} else {
 						map.setPushable(ttx,tty,(byte)0,(byte)0);
 						if(cw!=null) cw.addRecBlock(tmparr[0],tmparr[2],tmparr[3]);
 						map.setBlock(ttx,tty,tmparr);
 						if(mb == ev_1 || mb == ev_3)
						{
							CraftrBlock blockPlaced = map.getBlock(ttx,tty);
							byte[] bPdata = blockPlaced.getBlockData();
							map.setBlock(ttx,tty,bPdata[0],bPdata[1],(byte)map.updateLook(blockPlaced),bPdata[3]);
							for(int i=0;i<4;i++)
							{
								blockPlaced = map.getBlock(ttx+map.xMovement[i],tty+map.yMovement[i]);
								bPdata = blockPlaced.getBlockData();
								map.setBlock(ttx+map.xMovement[i],tty+map.yMovement[i],bPdata[0],bPdata[1],(byte)map.updateLook(blockPlaced),bPdata[3]);
							}
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

	public void shoot(int dir)
	{
		int sx=players[255].px+map.xMovement[dir];
		int sy=players[255].py+map.yMovement[dir];
		map.setBullet(sx,sy,(byte)(dir+1));
		blockChange=true;
		if(multiplayer)
		{
			net.shoot(sx,sy,(dir+1));
		}
		else
		{
			map.physics.addBlockToCheck(new CraftrBlockPos(sx,sy));
			for(int i=0;i<4;i++) map.physics.addBlockToCheck(new CraftrBlockPos(sx+map.xMovement[i],sy+map.yMovement[i]));
		}
	}

	public void keyTyped(KeyEvent ev) {} // this one sucks even more
	public void keyPressed(KeyEvent ev)
	{
		if(isKick) return;
		if(is != null)
		{
			is.parseKey(ev);
			return;
		}
		int kc = ev.getKeyCode();
		isShift = ev.isShiftDown();
		if(is == null && gs.barType == 0)
		{
			if(kc==key_left)
				keyHeld[1] = true;
			else if(kc==key_right)
				keyHeld[2] = true;
			else if(kc==key_up)
				keyHeld[0] = true;
			else if(kc==key_down)
				keyHeld[3] = true;
			if (gs.getWindow(1)!=null) {
				char chr = ev.getKeyChar();
				if(chr >= 32 && chr <= 127)
				{
					gs.sdrawChr(0xFF&(int)((byte)chr));
					gs.chrBarOff = gs.gdrawChr()-8;
					mouseChange=true;
				}
			}
			else
			{
				switch(kc)
				{
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
					case KeyEvent.VK_F:
						if(!multiplayer||net.isOp)
						{
							mouseChange=true;
							gs.viewFloorsMode=!gs.viewFloorsMode;
						}
						break;
					case KeyEvent.VK_B:
						gs.toggleWindow(3);
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
			else if(ev.isControlDown())
			{
				if(kc==KeyEvent.VK_V)
				{
					try
					{
						// Ctrl+V pressed, let's-a paste!
						Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
						if(clip.isDataFlavorAvailable(DataFlavor.stringFlavor))
						{
							gs.chatMsg += (String)clip.getData(DataFlavor.stringFlavor);
							mouseChange = true;
						}
					}
					catch(Exception e)
					{
						System.out.println("Clipboard pasting failed!");
						e.printStackTrace();
					}
				}
				else if(kc==KeyEvent.VK_C)
				{
					try
					{
						Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
						if(clip.isDataFlavorAvailable(DataFlavor.stringFlavor))
						{
							clip.setContents(new StringSelection(gs.chatMsg),null);
						}
					}
					catch(Exception e)
					{
						System.out.println("Clipboard copying failed!");
						e.printStackTrace();
					}
				}
			}
			else if (gs.chatMsg.length()<120)
			{
				char chr = ev.getKeyChar();
				if(chr >= 32 && chr <= 127) gs.chatMsg += chr;
				mouseChange = true;
			}
		}
		if(kc==KeyEvent.VK_F7)
		{
			muted=!muted;
			if(muted) gs.addChatMsg("&6Sound muted.");
			else gs.addChatMsg("&6Sound unmuted.");
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
		CraftrBlock blockMoveTo=map.getBlock(px,py);
		if(isShift && blockMoveTo.isEmpty())
		{
			for(int i=0;i<4;i++)
			{
				int tx = players[255].px+map.xMovement[i];
				int ty = players[255].py+map.yMovement[i];
				if(tx==px && ty==py)
				{
					shoot(i);
					waitTime=9;
				}
			}
		}
		else if(map.pushAttempt(px,py,dpx,dpy))
		{
			if(multiplayer)
			{
				net.playerPush(dpx,dpy);
			} else {
				map.setPlayer(players[255].px,players[255].py,0);
				map.setPlayer(px,py,1);
				map.setPlayer(px+dpx,py+dpy,1);
				oldmx=-1;
				oldmy=-1;
				players[255].move(px,py);
				playerChange = true;
			}
			return 2;
		}
		else if(blockMoveTo.isEmpty())
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
			return 2;
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
		int px = players[255].px;
		int py = players[255].py;
		int sx = px-15;
		int sy = py-12;
		CraftrBlock t;

		try
		{
			if (!raycasting)
			{
				for(int iy=0;iy<gs.FULLGRID_H;iy++)
				{
					for(int ix=0;ix<gs.FULLGRID_W;ix++)
					{
	 					t = map.getBlock(ix+sx,iy+sy);
						gs.blocks[(iy*gs.FULLGRID_W)+ix] = t;
						gs.blockChr[(iy*gs.FULLGRID_W)+ix] = (byte)t.getDrawnChar();
						gs.blockCol[(iy*gs.FULLGRID_W)+ix] = (byte)t.getDrawnColor();
					}
				}
			}else{
				gs.blocks = new CraftrBlock[gs.FULLGRID_W*gs.FULLGRID_H];
				for(double angle=0;angle<360;angle+=0.15)
				{
					double sin = Math.sin(Math.toRadians(angle));
					double cos = Math.cos(Math.toRadians(angle));
					for(double len=0;len<64;len+=0.25)
					{
						int x = (int)(15.5+sin*len);
						int y = (int)(12.5+cos*len);
						if(x>=0 && y>=0 && x<32 && y<25)
						{
							if(gs.blocks[(y*gs.FULLGRID_W)+x] == null)
							{
			 					t = map.getBlock(x+sx,y+sy);
								gs.blocks[(y*gs.FULLGRID_W)+x] = t;
								gs.blockChr[(y*gs.FULLGRID_W)+x] = (byte)t.getDrawnChar();
								gs.blockCol[(y*gs.FULLGRID_W)+x] = (byte)t.getDrawnColor();
								if(!t.isEmpty())
									break;
							}else{
								if(!gs.blocks[(y*gs.FULLGRID_W)+x].isEmpty()) break;
							}
						}else break;
					}
				}
			}
			for (int i=0;i<256;i++)
			{
				if(players[i] == null)
				{
					gs.removePlayer(i);
					continue;
				}
				int tx = (players[i].px-players[255].px)+15;
				int ty = (players[i].py-players[255].py)+12;
				gs.removePlayer(i);
				if(tx>=0 && ty>=0 && tx<32 && ty<25 && gs.blocks[(ty*gs.FULLGRID_W)+tx] != null)
				{
					CraftrBlock blockAtPlayer = map.getBlock(players[i].px,players[i].py);
					if(blockAtPlayer.getType()!=8) gs.addPlayer(i,tx,ty,players[i].name,players[i].pchr,players[i].pcol);
				}
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
		if(isApplet)
		{
			applet.getContentPane().add(canvas);
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
				canvas.draw(mx,my);
				if(waitTime>0) waitTime--;
				if(mx != oldmx || my != oldmy || mb != oldmb)
				{
					mouseChange = true;
					oldmx = mx;
					oldmy = my;
					oldmb = mb;
				}
				Thread.sleep(33);
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
		boolean inconf = true;
		is = new CraftrInScreen(canvas,2,"Main menu");
		String[] modes = new String[6];
		modes[0] = "Singleplayer";
		modes[1] = "Multiplayer";
		modes[4] = "Change player char ->";
		modes[5] = "Change player color ->";
		String ostr = "";
		while(inconf)
		{
			is = new CraftrInScreen(canvas,2,"Main menu");
			is.isRunning=true;
			modes[2] = "Key mode: " + ((kim>0)?"WSAD":"Arrows");
			modes[3] = "Hideous prompts: " + ((gs.hideousPrompts)?"On":"Off");
			is.addStrings(modes);
			canvas.cs = (CraftrScreen)is;
			loopInScreen();
			switch(is.inSel)
			{
				case 0:
					multiplayer = false;
					inconf = false;
					break;
				case 1:
					multiplayer = true;
					boolean doCustom = true;
					CraftrKickScreen cks = new CraftrKickScreen(canvas,"Loading serverlist...");
					cks.mName="DON'T PANIC";
					cks.bgcolor = new Color(128,128,128);
					canvas.cs = (CraftrScreen) cks;
					canvas.draw(mx,my);
					System.out.print("fetching... ");
					if(fetchSList())
					{
						System.out.println("fetched!");
						doCustom=false; // for now
						CraftrConfig csl = new CraftrConfig();
						csl.load(map.saveDir + "slist.txt");
						// by now csl stores the serverlist D:
						String[] csll = new String[csl.keys+2];
						csll[0]="Custom address";
						for(int i=1;i<=csl.keys;i++)
						{
							csll[i]=escapeSlashes(csl.keyo[i-1]);
						}
						csll[csll.length-1]="<- Back";
						is = new CraftrInScreen(canvas,2,"Choose server");
						is.addStrings(csll);
						canvas.cs= (CraftrScreen) is;
						loopInScreen();
						if(is.inSel==0) doCustom=true;
						else if(is.inSel==(csll.length-1)) { inconf = true; break; }
						else ostr=csl.value[is.inSel-1];
					}
					else
					{
						System.out.println("not fetched (probably means glados)");
						cks.bgcolor = new Color(170,0,0);
						cks.mName="SERVERLIST NOT FOUND";
						cks.name="PLEASE DON'T PANIC, ONE SECOND...";
						if(isApplet) cks.name="DON'T PANIC (does the applet have proper permissions?)";
						canvas.draw(mx,my);
						try{Thread.sleep(1800);}catch(Exception e){}
					}
					if(doCustom)
					{
						is = new CraftrInScreen(canvas,1,"Input address:");
						is.minLen=0;
						is.maxLen=60;
						canvas.cs = (CraftrScreen)is;
						loopInScreen();
						ostr = is.inString;
					}
					is = new CraftrInScreen(canvas,1,"Enter nickname:");
					is.minLen=1;
					is.maxLen=16;
					if(players[255].name != "You") is.inString = players[255].name;
					canvas.cs = (CraftrScreen)is;
					loopInScreen();
					net.nick = is.inString;
					inconf = false;
					break;
				case 2:
					changeKeyMode(1-(kim%2));
					break;
				case 3:
					gs.hideousPrompts=!gs.hideousPrompts;
					break;
				case 4:
					is.toggleWindow(1);
					while(is.getWindow(1)!=null)
					{
						canvas.draw(mx,my);
						try{ Thread.sleep(33); } catch(Exception e){}
					}
					players[255].pchr = (byte)confChr;
					break;
				case 5:
					is.toggleWindow(2);
					while(is.getWindow(2)!=null)
					{
						canvas.draw(mx,my);
						try{ Thread.sleep(33); } catch(Exception e){}
					}
					players[255].pcol = (byte)confCol;
					break;
			}
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
		ev_1=65535;
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
		gt = new CraftrGameThread(this);
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
		String thost = "127.0.0.1";
		if(skipConfig && isApplet)
		{
			String t1 = applet.getParameter("ip");
			String t2 = applet.getParameter("port");
			if(t1!=null && !t1.equals(""))
			{
				multiplayer=true;
				thost=t1;
				if(t2!=null && !t1.equals("")) thost+= ":" + t2;
				else thost+=":25566";
				net.nick = players[255].name;
			}
			else multiplayer=false;
		}
		else if (skipConfig && !isApplet) multiplayer=false;
		else thost = configure();
		isConfig=false;
		if(!multiplayer)
		{
			gs.addChatMsg("you're running 64pixels " + getVersion());
			gs.addChatMsg("created by asiekierka and GreaseMonkey.");
			Thread tmap = new Thread(cmt);
			tmap.start();
		}
		else
		{
			System.out.println("Connecting...");
			CraftrKickScreen cks = new CraftrKickScreen(canvas,"Wait a second...");
			cks.mName="CONNECTING...";

			cks.bgcolor = new Color(128,128,128);
			canvas.cs = (CraftrScreen)cks;
			net.connect(CraftrConvert.getHost(thost),CraftrConvert.getPort(thost), nagle);
			System.out.println("Connected! Logging in...");
			canvas.cs = (CraftrScreen)gs;
			map.net = net;
			net.gaa = this;
			map.multiplayer = true;
			Thread t1 = new Thread(net);
			t1.start();
			net.chunkRequest(0,0);
		}
		Thread t2 = new Thread(gt);
		t2.start();
	}
	public void runOnce()
	{
		if(isKick) { gt.isRunning=false; realKickOut(); }
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
		else waitTime--;
		while(overhead>33)
		{
			if(waitTime>0) waitTime--;
			if(chrArrowWaiter>0) chrArrowWaiter--;
			overhead-=33;
		}
		if(chrArrowWaiter>0) chrArrowWaiter--;
		else if(mb == ev_1) {
			if(insideRect(mx,my,12*16+8,gs.BARPOS_Y+1,8,14))
			{
				gs.chrBarOff -= 1;
				chrArrowWaiter=2;
				mouseChange=true;
				if(gs.chrBarOff<0) gs.chrBarOff = 255;
			}
			else if(insideRect(mx,my,29*16,gs.BARPOS_Y+1,8,14))
			{
				gs.chrBarOff += 1;
				chrArrowWaiter=2;
				mouseChange=true;
				if(gs.chrBarOff>255) gs.chrBarOff=0;
			}
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
			canvas.draw(mx,my);
		}
		frame++;
		overdate = new Date();
		overhead+=overdate.getTime()-told.getTime()-33;
		told = overdate;
		if(told.compareTo(tnew)>=0)
		{
			tnew = new Date(told.getTime() + 1000L);
			fold = frame;
			fps = (int)gt.fps;
			gt.fps=0;
			System.out.println(fps + " fps, physics runs at " + (cmt.wps-wpso) + "checks a second");
			wpso = cmt.wps;
		}
		gt.isRunning=gameOn;
	}
	public void stop()
	{
		gt.isRunning=false;
		gameOn = false;
	}
	public void end()
	{
		System.out.print("Saving... ");
		gt.isRunning=false;
		gameOn=false;
		if(map.saveDir != "")
		{
			if(!multiplayer)
			{
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
				net.sockClose();
			}
			saveConfig();
		}
		audio.kill();
		System.out.println("Done!");
	}
	public void finalize()
	{
		end();
	}
}
