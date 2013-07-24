package client;
import common.*;

import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import java.lang.*;
import java.text.*;
import java.net.*;

public class Game extends JComponent
implements MouseListener, MouseMotionListener, KeyListener, ComponentListener, FocusListener, GameShim
{
	public Canvas canvas;
	public JFrame window;
	public boolean gameOn;
	public static Random rand = new Random();

	public WorldMap map;
	public Player players[] = new Player[256];
	public Player player;
	
	public boolean hasShot;
	public boolean blockChange = false;
	public boolean playerChange = false;
	public boolean mouseChange = false;
	public boolean netChange = false;
	public boolean multiplayer;
	public boolean isKick = false;
	public MapThread cmt;
	public int cmtsp=30;
	public int overhead=0;
	public Config config;
	public Net net;
	public GameScreen gs;
	public TextInputScreen textInputScreen;
	public OptionScreen optionScreen;
	public Sound audio;
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
	private GameThread gt;

	public void setHealth(int h)
	{
		player.health = h;
		gs.health = player.health;
	}

	public void kill()
	{
		if(multiplayer) return;
		setHealth(player.health-1);
		if(player.health==0)
		{
			gs.addChatMsg("&cYou were killed!");
			player.health=5;
			gs.health=5;
			map.setPlayer(player.x,player.y,0);
			map.setPlayer(0,0,1);
			oldmx=-1;
			oldmy=-1;
			player.move(0,0);
			playerChange = true;
		}
	}

	public void playSound(int tx, int ty, int val)
	{
		if(muted) return;
		if(val>=256)
		{
			playSample(tx,ty,val-256);
			return;
		}		
		int x=player.x-tx;
		int y=player.y-ty;
		audio.playNote(x,y,val,1.0);
	}
	public void playSample(int tx, int ty, int val)
	{
		if(muted) return;
		int x=player.x-tx;
		int y=player.y-ty;
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
		return Version.getVersionName();
	}
	public Game()
	{
		audio = new Sound();
		File sdchk = new File(System.getProperty("user.home") + "/.64pixels");
		if(!sdchk.exists()) sdchk.mkdir();
		window = new JFrame("64pixels^2 " + getVersion());
		gameOn = true;
		map = new WorldMap(false,64);
		map.game = this;
		map.saveDir = System.getProperty("user.home") + "/.64pixels/";
		players[255] = new Player(0,0);
		player = players[255];
		canMousePress = true;
		config = new Config(map.saveDir + "config.txt");
		cmt = new MapThread(map);
		gs = new GameScreen(null);	
		loadConfig();
		canvas = new Canvas();
		gs.setCanvas(canvas);
		canvas.cs = (Screen)gs;
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
		KickScreen cks=new KickScreen(canvas,isKickS);
		canvas.cs=(Screen)cks;
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
		TextInputScreen screen = new TextInputScreen(canvas,name);
		canvas.cs = (Screen)screen;
		screen.maxLen=len;
		screen.minLen=1;
		loopScreen();
		canvas.cs = (Screen)gs;
		return screen.inString;
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
			s = "player-char=" + player.chr;
			out.write(s,0,s.length());
			out.newLine();
			s = "player-color=" + player.col;
			out.write(s,0,s.length());
			out.newLine();
			if(map.cachesize != 64)
			{
				s = "map-cache-size=" + map.cachesize;
				out.write(s,0,s.length());
				out.newLine();
			}
			s = "player-name=" + player.name;
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
				lpx = player.x;
				lpy = player.y;
			}
			if(gs.hideousPrompts)
			{
				s = "hideous-prompts=1";
				out.write(s,0,s.length());
				out.newLine();
			}
			if(!canvas.resizePlayfield)
			{
				s = "resize-playfield=false";
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
					if(!Block.isPlaceable(gs.drawType)) gs.drawType = 0;
				}
				else if(key.contains("player-char"))
				{
					player.chr = nf.parse(val).byteValue();
				}
				else if(key.contains("mapgen-mode"))
				{
					map.genMode = nf.parse(val).byteValue();
				}
				else if(key.contains("player-name"))
				{
					player.name = val;
				}
				else if(key.contains("player-color"))
				{
					player.col = nf.parse(val).byteValue();
				}
				else if(key.contains("map-cache-length"))
				{
					map.resizeChunks(nf.parse(val).intValue());
				}
				if(key.contains("player-x"))
				{
					player.x = nf.parse(val).intValue();
					lpx = player.x;
				}
				else if(key.contains("player-y"))
				{
					player.y = nf.parse(val).intValue();
					lpy = player.y;
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
					 *  but \ is also special so we escape THAT with another \	 */
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
				else if(key.contains("resize-playfield"))
				{
					if(val.contains("false")) canvas.resizePlayfield = false;
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
		mouseChange = gs.mousePressed(ev);
		processWindows();
		processMouse();
	}
	public void mouseReleased(MouseEvent ev) { mb = ev_no; canMousePress = true; advMouseMode = false;}

	int confChr = 0;
	int confCol = 0; 
	public void processWindows()
	{
		ArrayList<Window> w;
		OptionScreen os = null;
		if(canvas.cs instanceof OptionScreen) os = (OptionScreen)canvas.cs;
		if(isConfig && canvas.cs instanceof OptionScreen) w = os.windows;
		else w=gs.windows;
		try
		{
			synchronized(w)
			{
				if(w.size()>0)
					for(Window cw : w)
					{
						if(!isConfig && gs.obstructedWindow(cw,mx,my)) { }
						else if(isConfig && os != null && os.obstructedWindow(cw,mx,my)) { }
						else if(insideRect(mx,my,(cw.x+cw.w-1)<<3,cw.y<<3,8,8))
						{
							// close button, any window
							if(isConfig && os != null) os.toggleWindow(cw.type);
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
										if(isConfig && os != null) os.getWindow(1).charChosen = confChr;
									}				
									break;
								case 2:
									confCol = (((mx-((cw.x+1)<<3))>>3)&15) | (((my-((cw.y+1)<<3))<<1)&240);
									gs.sdrawCol(confCol);
									if(isConfig && os != null) os.getWindow(2).colorChosen = confCol;
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
									gs.drawType=Window.getBlockType((my-((cw.y+1)<<3))>>3);
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
		if(!isConfig && (mx >= 0 && mx < canvas.WIDTH && my >= 0 && my < (canvas.GRID_H<<4)))
		{
			int tx = (player.x+(mx>>4))-(canvas.FULLGRID_W/2)+1;
			int ty = (player.y+(my>>4))-(canvas.FULLGRID_H/2)+1;
			gs.hov_type=map.getBlock(tx,ty).getTypeWithVirtual();
		}
		if(isDragging)
		{
			ArrayList<Window> w = gs.windows;
			if(isConfig && canvas.cs instanceof OptionScreen) {
				OptionScreen os = (OptionScreen)canvas.cs;
				w = os.windows;
			}
			synchronized(w)
			{
				Window dcw = w.get(dragID);
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
				Window cw = gs.getWindow(3);
				for(int i=0;i<256;i++)
				{
					if(gs.players[i] != null && gs.players[i].x == mx>>4 && gs.players[i].y == my>>4) return;
				}
				byte[] tmparr = new byte[4];
				Block capturedBlock = map.getBlock(player.x-(canvas.FULLGRID_W/2)+1+(mx>>4),player.y-(canvas.FULLGRID_H/2)+1+(my>>4));
				if(!capturedBlock.isPlaceable()) return;
				if(mb == ev_1)
				{
					tmparr[0] = (byte)gs.drawType;
					tmparr[1] = (byte)Block.getParam(gs.drawType);
					tmparr[2] = (byte)gs.gdrawChr();
					tmparr[3] = (byte)gs.gdrawCol();
				}
				if(mb == ev_2)
				{
					if(capturedBlock.isPlaceable() && !advMouseMode) gs.drawType = capturedBlock.getTypeWithVirtual();
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
					int ttx = player.x-(canvas.FULLGRID_W/2)+1+(mx>>4);
					int tty = player.y-(canvas.FULLGRID_H/2)+1+(my>>4);
					if(!multiplayer) synchronized(map.physics)
					{
						map.physics.addBlockToCheck(new Point(ttx,tty));
						for(int i=0;i<4;i++)
						{
							map.physics.addBlockToCheck(new Point(ttx+map.xMovement[i],tty+map.yMovement[i]));
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
							Block blockPlaced = map.getBlock(ttx,tty);
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
		int sx=player.x+map.xMovement[dir];
		int sy=player.y+map.yMovement[dir];
		map.setBullet(sx,sy,(byte)(dir+1));
		blockChange=true;
		if(multiplayer)
		{
			net.shoot(sx,sy,(dir+1));
		}
		else
		{
			map.physics.addBlockToCheck(new Point(sx,sy));
			for(int i=0;i<4;i++) map.physics.addBlockToCheck(new Point(sx+map.xMovement[i],sy+map.yMovement[i]));
		}
	}

	public void keyTyped(KeyEvent ev) {} // this one sucks even more
	public void keyPressed(KeyEvent ev)
	{
		if(isKick) return;
		if(canvas.cs != gs)
		{
			canvas.cs.parseKey(ev);
			return;
		}
		int kc = ev.getKeyCode();
		isShift = ev.isShiftDown();
		if(canvas.cs == gs && gs.barType == 0)
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
						System.out.println("player pos: x = " + player.x + ", y = " + player.y + ".");
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
						if(multiplayer && (player.x != 0 || player.y != 0))
						{
							net.respawnRequest();
						}
						break;
					case KeyEvent.VK_F:
						if(!multiplayer || player.op)
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
		if(hasShot) return waitTime;
		int px = player.x+dpx;
		int py = player.y+dpy;
		Block blockMoveTo=map.getBlock(px,py);
		if(isShift && blockMoveTo.isEmpty())
		{
			for(int i=0;i<4;i++)
			{
				int tx = player.x+map.xMovement[i];
				int ty = player.y+map.yMovement[i];
				if(tx==px && ty==py)
				{
					shoot(i);
					hasShot = true;
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
				map.setPlayer(player.x,player.y,0);
				map.setPlayer(px,py,1);
				map.setPlayer(px+dpx,py+dpy,1);
				oldmx=-1;
				oldmy=-1;
				player.move(px,py);
				playerChange = true;
			}
			return 2;
		}
		else if(blockMoveTo.isEmpty())
		{
			if(multiplayer) net.playerMove(dpx,dpy);
			else
			{
				map.setPlayer(player.x,player.y,0);
				map.setPlayer(px,py,1);
			}
			oldmx=-1;
			oldmy=-1;
			player.move(px,py);
			playerChange = true;
			return 2;
 		}
		return waitTime;
	}
	public void spawnPlayer(int cx, int cy, int id)
	{
		Chunk pc;
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
				if(pc.getBlockType(i,j) == 0) { players[id].x = cx+i; players[id].y = cy+j; return; }
			}
		}
	}
	
	public void render()
	{
		int px = player.x;
		int py = player.y;
		int sx = px-(canvas.FULLGRID_W/2)+1;
		int sy = py-(canvas.FULLGRID_H/2)+1;
		Block t;
		try
		{
			if (!raycasting)
			{
				for(int iy=0;iy<canvas.FULLGRID_H;iy++)
				{
					for(int ix=0;ix<canvas.FULLGRID_W;ix++)
					{
						gs.blocks[(iy*canvas.FULLGRID_W)+ix] = map.getBlock(ix+sx,iy+sy);
					}
				}
			}
			else
			{
				for(int iy=0;iy<canvas.FULLGRID_H;iy++)
				{
					for(int ix=0;ix<canvas.FULLGRID_W;ix++)
					{
						gs.blocks[(iy*canvas.FULLGRID_W)+ix] = null;
					}
				}
				// this is the recursive route.
				gs.blocks[(((canvas.FULLGRID_H/2)-1)*canvas.FULLGRID_W)+(canvas.FULLGRID_W/2)-1] = map.getBlock(px,py);
				castRayPillars(px,py,-1, 0,-1,-1,-1, 1,(canvas.FULLGRID_W/2)+2);
				castRayPillars(px,py, 1, 0, 1,-1, 1, 1,(canvas.FULLGRID_W/2)+2);
				castRayPillars(px,py, 0,-1,-1,-1, 1,-1,(canvas.FULLGRID_H/2)+2);
				castRayPillars(px,py, 0, 1,-1, 1, 1, 1,(canvas.FULLGRID_H/2)+2);
			}
			for (int i=0;i<256;i++)
			{
				if(players[i] == null)
				{
					gs.removePlayer(i);
					continue;
				}
				int tx = (players[i].x-player.x)+(canvas.FULLGRID_W/2)-1;
				int ty = (players[i].y-player.y)+(canvas.FULLGRID_H/2)-1;
				gs.removePlayer(i);
				if(tx>=0 && ty>=0 && tx<canvas.FULLGRID_W && ty<canvas.FULLGRID_H && gs.blocks[(ty*canvas.FULLGRID_W)+tx] != null)
				{
					Block blockAtPlayer = map.getBlock(players[i].x,players[i].y);
					if(blockAtPlayer.getType()!=8) gs.addPlayer(i,tx,ty,players[i].name,players[i].chr,players[i].col);
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
	
	private void castRayPillars(int sx, int sy, int dx, int dy, int x1, int y1, int x2, int y2, int maxtrace)
	{
		assert(x1 <= x2 && y1 <= y2);
		
		// TODO: make it aim for the block corners.
		//       that way we can get the FULL visibility,
		//       rather than a slightly clipped one.
		
		int ox1 = x1;
		int oy1 = y1;
		int ox2 = x2;
		int oy2 = y2;
		
		
		int adx = (dx < 0 ? -dx : dx);
		int ady = (dy < 0 ? -dy : dy);
		
		while(maxtrace > 0)
		{
			//System.out.printf("maxtrace %d %d %d: %d %d -> %d %d\n", maxtrace, dx, dy, x1, y1, x2, y2);
			boolean hitone = false;
			boolean hittingone = false;
			int x = x1, y = y1;
			
			// AFAIK this is pretty similar to Bresenham's thing.
			while(x <= x2 && y <= y2)
			{
				// RANGE CHECK!
				if(x >= 0-(canvas.FULLGRID_W/2)+1 && x < canvas.FULLGRID_W-((canvas.FULLGRID_W/2)-1) && y >= 0-(canvas.FULLGRID_H/2)+1 && y < canvas.FULLGRID_H-((canvas.FULLGRID_H/2)-1))
				{
					Block t = map.getBlock(x+sx,y+sy);

					// first check: block behind is empty
					// second check: block is aligned with the axis
					// third check: block right/left is empty
					boolean antidiagcheck = 
						   map.getBlock(x+sx-dx,y+sy-dy).isEmpty()
						|| x == 0 || y == 0
						|| map.getBlock(x+sx-(x < 0 ? -ady : ady),y+sy-(y < 0 ? -adx : adx)).isEmpty();

					//if(antidiagcheck || !t.isEmpty()) // no corners for you - TODO: fix the "flicker"
					if(antidiagcheck)
						gs.blocks[((y+((canvas.FULLGRID_H/2)-1))*canvas.FULLGRID_W)+x+((canvas.FULLGRID_W/2)-1)] = t;
					
					if(!(t.isEmpty() && antidiagcheck))
					{
						if(!hittingone)
						{
							hitone = true;
							hittingone = true;
							// we must split this.
							if(x1 != x || y1 != y)
								castRayPillars(sx,sy,dx,dy,x1,y1,x-ady,y-adx,maxtrace);
						}
					} else if(hittingone) {
						hittingone = false;
						x1 = x;
						y1 = y;
					}
				}
				x += ady;
				y += adx;
			}
			
			// touch walls if necessary
			x = x1;
			y = y1;
			{
				Block t2 = map.getBlock(x+sx,y+sy);

				if(t2.isEmpty())
				{
					x -= ady;
					y -= adx;
					if(x >= 0-(canvas.FULLGRID_W/2)+1 && x < canvas.FULLGRID_W-((canvas.FULLGRID_W/2)-1) && y >= 0-(canvas.FULLGRID_H/2)+1 && y < canvas.FULLGRID_H-((canvas.FULLGRID_H/2)-1))
					{
						Block t = map.getBlock(x+sx,y+sy);

						if(!t.isEmpty())
							gs.blocks[((y+((canvas.FULLGRID_H/2)-1))*canvas.FULLGRID_W)+x+((canvas.FULLGRID_W/2)-1)] = t;		
					}
				}
			}
			
			x = x2;
			y = y2;
			{
				Block t2 = map.getBlock(x+sx,y+sy);

				if(t2.isEmpty())
				{
					x += ady;
					y += adx;
					if(x >= 0-(canvas.FULLGRID_W/2)+1 && x < canvas.FULLGRID_W-((canvas.FULLGRID_W/2)-1) && y >= 0-(canvas.FULLGRID_H/2)+1 && y < canvas.FULLGRID_H-((canvas.FULLGRID_H/2)-1))
					{
						Block t = map.getBlock(x+sx,y+sy);

						if(!t.isEmpty())
							gs.blocks[((y+((canvas.FULLGRID_H/2)-1))*canvas.FULLGRID_W)+x+((canvas.FULLGRID_W/2)-1)] = t;		
					}
				}
			}
			
			if(hitone)
			{
				if(!hittingone)
				{
					castRayPillars(sx,sy,dx,dy,x1,y1,x2,y2,maxtrace);
				}
				
				return;
			}
			
			if(dy == 0)
			{
				x1 += dx;
				x2 += dx;
			} else {
				y1 += dy;
				y2 += dy;
			}
			
			if(dy == 0)
			{
				y1 = (int)(oy1*x1/ox1);
				y2 = (int)(oy2*x2/ox2);
			} else {
				x1 = (int)(ox1*y1/oy1);
				x2 = (int)(ox2*y2/oy2);
			}
			
			maxtrace--;
		}
	}
	
	public void init()
	{
		window.add(canvas);
		window.pack(); // makes everything a nice size
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
		tnew = new Date(told.getTime() + 1000L);
	}
	
	public void loopScreen()
	{
		try
		{
			while(canvas.cs.isRunning)
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
			System.out.println("Fatal loopScreen error!");
			System.exit(1);
		}
	}

	public String configure()
	{
		boolean inconf = true;
		String[] modes = new String[7];
		modes[0] = "Singleplayer";
		modes[1] = "Multiplayer";
		modes[4] = "Change player char ->";
		modes[5] = "Change player color ->";
		String ostr = "";
		while(inconf)
		{
			OptionScreen screen = new OptionScreen(canvas,"Main menu");
			modes[2] = "Key mode: " + ((kim>0)?"WSAD":"Arrows");
			modes[3] = "Hideous prompts: " + ((gs.hideousPrompts)?"On":"Off");
			modes[6] = "Resize mode: " + ((canvas.resizePlayfield)?"Playfield":"Scale");
			screen.addStrings(modes);
			canvas.cs = (Screen)screen;
			loopScreen();
			switch(screen.inSel)
			{
				case 0:
					multiplayer = false;
					inconf = false;
					break;
				case 1: {
					multiplayer = true;
					boolean doCustom = true;
					KickScreen cks = new KickScreen(canvas,"Loading serverlist...");
					cks.mName="DON'T PANIC";
					cks.bgcolor = 0x808080;
					canvas.cs = (Screen) cks;
					canvas.draw(mx,my);
					System.out.print("fetching... ");
					if(fetchSList())
					{
						System.out.println("fetched!");
						doCustom=false; // for now
						Config csl = new Config();
						csl.load(map.saveDir + "slist.txt");
						// by now csl stores the serverlist D:
						String[] csll = new String[csl.keys+2];
						csll[0]="Custom address";
						for(int i=1;i<=csl.keys;i++)
						{
							csll[i]=escapeSlashes(csl.keyo[i-1]);
						}
						csll[csll.length-1]="<- Back";
						screen = new OptionScreen(canvas,"Choose server");
						screen.addStrings(csll);
						canvas.cs= (Screen)screen;
						loopScreen();
						if(screen.inSel==0) doCustom=true;
						else if(screen.inSel==(csll.length-1)) { inconf = true; break; }
						else ostr=csl.value[screen.inSel-1];
					}
					else
					{
						System.out.println("not fetched (probably means glados)");
						cks.bgcolor = 0xAA0000;
						cks.mName="SERVERLIST NOT FOUND";
						cks.name="PLEASE DON'T PANIC, ONE SECOND...";
						canvas.draw(mx,my);
						try{Thread.sleep(1800);}catch(Exception e){}
					}
					if(doCustom)
					{
						TextInputScreen aScreen = new TextInputScreen(canvas,"Input address:");
						aScreen.minLen=0;
						aScreen.maxLen=60;
						canvas.cs = (Screen)aScreen;
						loopScreen();
						ostr = aScreen.inString;
					}
					TextInputScreen nScreen = new TextInputScreen(canvas,"Enter nickname:");
					nScreen.minLen=1;
					nScreen.maxLen=16;
					if(player.name != "You") nScreen.inString = player.name;
					canvas.cs = (Screen)nScreen;
					loopScreen();
					inconf = false;
					} break;
				case 2:
					changeKeyMode(1-(kim%2));
					break;
				case 3:
					gs.hideousPrompts=!gs.hideousPrompts;
					break;
				case 4:
					screen.toggleWindow(1);
					while(screen.getWindow(1)!=null)
					{
						canvas.draw(mx,my);
						try{ Thread.sleep(33); } catch(Exception e){}
					}
					if(confChr!=0) player.chr = (byte)confChr;
					break;
				case 5:
					screen.toggleWindow(2);
					while(screen.getWindow(2)!=null)
					{
						canvas.draw(mx,my);
						try{ Thread.sleep(33); } catch(Exception e){}
					}
					if(confCol!=0) player.col = (byte)confCol;
					break;
				case 6:
					canvas.resizePlayfield=!canvas.resizePlayfield;
					break;
			}
		}
		canvas.cs = (Screen)gs;
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
		window.getRootPane().addComponentListener(this);
		window.addFocusListener(this);
		window.addKeyListener(this);
		addKeyListener(this);
		addComponentListener(this);
		net = new Net(this);
		gt = new GameThread(this);
		window.getRootPane().addMouseListener(this);
		window.getRootPane().addMouseMotionListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		String thost = "127.0.0.1";
		if (skipConfig) multiplayer=false;
		else thost = configure();
		isConfig=false;
		setHealth(5);
		if(!multiplayer)
		{
			gs.addChatMsg("you're running 64pixels^2 " + getVersion());
			gs.addChatMsg("created by asiekierka and GreaseMonkey.");
			Thread tmap = new Thread(cmt);
			tmap.start();
		}
		else
		{
			System.out.println("Connecting...");
			KickScreen cks = new KickScreen(canvas,"Please wait");
			cks.mName="CONNECTING...";
			cks.bgcolor = 0x808080;
			canvas.cs = (Screen)cks;
			net.connect(Convert.getHost(thost),Convert.getPort(thost), nagle);
			System.out.println("Connected! Logging in...");
			canvas.cs = (Screen)gs;
			gs.showHealthBar = false;
			map.net = net;
			map.multiplayer = true;
			Thread t1 = new Thread(net);
			t1.start();
			net.chunkRequest(0,0);
		}
		Thread t2 = new Thread(gt);
		t2.start();
		gs.setCanvas(canvas);
	}
	public void runOnce()
	{
		hasShot = false;
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
			playerChange = player.posChanged;
			player.posChanged = false;
		}
		if(playerChange) {
			map.physics.players[255] = player;
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
			System.out.println("SPEED: " + fps + " fps (physics: " + (cmt.wps-wpso) + " fps)");
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
