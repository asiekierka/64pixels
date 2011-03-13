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
	public boolean isKick = false;
	public CraftrMapThread cmt;
	public int cmtsp=30;
	public CraftrConfig config;
	public CraftrNet net;
	public CraftrGameScreen gs;
	public CraftrInScreen is;
	public CraftrSound audio;
	public int chrArrowWaiter = 0;
	public boolean[] keyHeld;
	public boolean advMouseMode = false;
	public int netThreadRequest = 0;
	public int key_up = KeyEvent.VK_UP;
	public int key_left = KeyEvent.VK_LEFT;
	public int key_right = KeyEvent.VK_RIGHT;
	public int key_down = KeyEvent.VK_DOWN;
	public int kim = 0;
	public int scm = 0;
	public int scm2 = 0;
	public String isKickS;
	public void playSound(int tx, int ty, int val)
	{
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
		canvas = new CraftrCanvas(scm);
		canvas.scm2=scm2;
		gs.c=canvas;
		canvas.cs = (CraftrScreen)gs;
		if(cmtsp>0) cmt.speed=(1000/cmtsp);
		else cmt.speed=0;
		keyHeld = new boolean[4];
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
			u1 = new URL("http://64pixels.org/serverlist.php?asie=1");
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
				else if(key.contains("dch|"))
				{
					String[] dchi = key.split("\\|");
					/* as | is a special character here, we need to escape it with \. *
					 *  but \ is also special so we escape THAT with another \         */
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
	public void mousePressed(MouseEvent ev)
	{
		mb = ev.getButton();
		updateMouse(ev.getX(),ev.getY());
		ev_no = ev.NOBUTTON;
		ev_1 = ev.BUTTON1;
		ev_2 = ev.BUTTON2;
		ev_3 = ev.BUTTON3;
		advMouseMode = ev.isControlDown();
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
				if(gs.cw.type == 4) gs.cwOpen = !gs.cwOpen;
				else gs.cwOpen = true;
				gs.cw.type = 4;
			}
			 else if (insideRect(mx,my,8*16+8,gs.BARPOS_Y,24,8)) // mode, chr
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
						int ct =(((mx-((gs.cw.x+1)<<3))>>3)%(gs.cw.w-2)) + ((((my>>3)-(gs.cw.y+1))%(gs.cw.h-2))*(gs.cw.w-2));
						if(ct<=255)
						{
							gs.sdrawChr(ct);
							gs.chrBarOff = ct-8;
							if(gs.chrBarOff<0) gs.chrBarOff+=256;
						}					
						break;
					case 2:
						gs.sdrawCol((((mx-((gs.cw.x+1)<<3))>>3)&15) | (((my-((gs.cw.y+1)<<3))<<1)&240));
						break;
					case 3:
						if(insideRect(mx,my,(gs.cw.x+2)<<3,(gs.cw.y+2)<<3,(gs.cw.w-4)<<3,(gs.cw.h-4)<<3))
						{
							int ix = (mx-((gs.cw.x+2)<<3))>>4;
							int iy = (my-((gs.cw.y+2)<<3))>>4;
							int ip = ix+iy*4;
							gs.drawType = gs.cw.recBlockType[ip];
							gs.sdrawChr(gs.cw.recBlockChr[ip]);
							gs.chrBarOff = gs.gdrawChr()-8;
							if(gs.chrBarOff<0) gs.chrBarOff+=256;
							gs.sdrawCol(gs.cw.recBlockCol[ip]);
							gs.cwOpen = false;
							canMousePress = false;
							mouseChange = true;
						}
						break;
					case 4:
						gs.drawType=((my-((gs.cw.y+1)<<3))>>3)-1;
						break;
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
					gs.sdrawCol((mx-(12*16+8))>>4);
				}
			}
			else if (gs.barselMode == 1 && gs.drawType == 3)
			{
				if(insideRect(mx,my,12*16+8,gs.BARPOS_Y,64,16))
				{
					gs.sdrawChr(24+((mx-(12*16+8))>>4));
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
			processMouse();
	}
	public void mouseReleased(MouseEvent ev) { mb = ev_no; canMousePress = true; advMouseMode = false;}

	public void mouseMoved(MouseEvent ev) {
		updateMouse(ev.getX(),ev.getY());
		advMouseMode = ev.isControlDown();
	}
	public void mouseDragged(MouseEvent ev) { updateMouse(ev.getX(),ev.getY()); advMouseMode = ev.isControlDown(); } // this can be quite handy
	
	public void updateMouse(int umx, int umy)
	{
		mx=(int)(umx/canvas.scaleX);
		my=(int)(umy/canvas.scaleY);
		if(mx >= 0 && mx < gs.WIDTH && my >= 0 && my < (gs.GRID_H<<4))
		{
			int tx = (players[255].px+(mx>>4))-15;

			int ty = (players[255].py+(my>>4))-12;
			gs.hov_type=map.getBlock(tx,ty).getTypeWithVirtual();
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
					gs.cw.addRecBlock((byte)gs.drawType,(byte)gs.gdrawChr(),(byte)gs.gdrawCol());
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
 						gs.cw.addRecBlock(tmparr[0],tmparr[2],tmparr[3]);
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
		else map.physics.addBlockToCheck(new CraftrBlockPos(sx,sy));
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
			if (gs.cwOpen && gs.cw.type==1) {
				char chr = ev.getKeyChar();
				if(chr >= 32 && chr <= 127)
				{
					gs.sdrawChr(0xFF&(int)((byte)chr));
					gs.chrBarOff = gs.gdrawChr()-8;
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
			return 3;
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
		int px = players[255].px;
		int py = players[255].py;
		int sx = px-15;
		int sy = py-12;
		try
		{
			for(int iy=0;iy<gs.FULLGRID_H;iy++)
			{

				for(int ix=0;ix<gs.FULLGRID_W;ix++)
				{
					gs.blocks[(iy*gs.FULLGRID_W)+ix] = map.getBlock(ix+sx,iy+sy);
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
				if(tx>=0 && ty>=0 && tx<32 && ty<25)
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
				Thread.sleep(100);
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal loopInScreen error!");
			System.exit(1);
		}
	}

	private String configure_gets1() { return "Key mode: " + ((kim>0)?"WSAD":"Arrows"); }
	public String configure()
	{
		boolean inconf = true;
		is = new CraftrInScreen(canvas,2,"Main menu");
		String[] modes = new String[3];
		modes[0] = "Singleplayer";
		modes[1] = "Multiplayer";
		modes[2] = configure_gets1();
		String ostr = "";
		while(inconf)
		{
			is = new CraftrInScreen(canvas,2,"Main menu");
			is.isRunning=true;
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
						System.out.println("not fetched (means glados)");
						cks.mName="SERVERLIST NOT FOUND";
						cks.name="PLEASE DON'T PANIC, ONE SECOND...";
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
					modes[2] = configure_gets1();
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
			if(isKick) realKickOut();
			try
			{
			Thread.sleep(33);
			}
			catch (Exception e) { }
			gs.cw.typeChosen=gs.drawType;
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
			net.sockClose();
		}
		saveConfig();
		audio.kill();
		System.out.println("Done!");
	}
	public void finalize()
	{
		end();
	}
}
