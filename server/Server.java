package server;
import common.*;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;

public class Server extends ServerShim
{
	public ServerSocket servsock;
	public Client[] clients;
	public String name = "64pixels server";
	public boolean run; 
	public WorldMap map;
	public Input ci;
	public ByteArrayOutputStream out2;
	public boolean anonMode;
	public String[] op_ips;
	public String[] ban_ips;
	public String[] world_names;
	public ArrayList<World> worlds;
	public int spawnX=0;
	public int privmode=0;
	public int spawnY=0;
	public Config config;
	public int nagle=0;
	public int map_tps=10;
	public int map_save_duration=10;
	public int map_cache_size=128;
	public int tpforall=0;
	public boolean passOn = false;
	public String pass;
	public boolean opPassOn = false;
	public String opPass;
	public Warps warps;
	public int po = 25566;
	public boolean mapLock = false;
	public World world;
	public PluginHandler pluginHandler;

	public int countPlayers()
	{
		int t = 0;
		for(int i=0;i<255;i++)
		{
			if(clients[i]!=null && clients[i].dc==0) t++;
		}
		return t;
	}

	public void changeMainSpawnXY(int x, int y)
	{
		spawnX=x; world.spawnX=x;
		spawnY=y; world.spawnY=y;
	}

	public void kill(int pid)
	{
		int tX=clients[pid].world.spawnX;
		int tY=clients[pid].world.spawnY;
		if(pid>=0 && pid<256 && clients[pid]!=null && clients[pid].dc==0 && (clients[pid].x!=tX || clients[pid].y!=tY))
		{
			clients[pid].kill();
		}
	}

	public World findWorld(String name)
	{
		for(int i=0;i<worlds.size();i++)
		{
			if(worlds.get(i).name.equalsIgnoreCase(name))
			{
				return worlds.get(i);
			}
		}
		return null;
	}

	public boolean isOp(String ip)
	{
		for(String s: op_ips)
		{
			if(s.equals(ip.toLowerCase())) return true;
		}
		return false;
	}
	
	public boolean isBanned(String ip)
	{
		for(String s: ban_ips)
		{
			if(s.equals(ip.toLowerCase())) return true;
		}
		return false;
	}
	
	public void addOp(String ip)
	{
		ArrayList<String> t = new ArrayList<String>(op_ips.length+1);
		for(String s:op_ips)
		{
			if(s.equals(ip.toLowerCase())) return;
			t.add(s);
		}
		t.add(ip.toLowerCase());
		String[] z = new String[t.size()];
		int i = 0;
		for(String s : t)
		{
			z[i++]=s.toLowerCase();
		}
		op_ips = z;
	}
	
	public void removeOp(String ip)
	{
		ArrayList<String> t = new ArrayList<String>(op_ips.length+1);
		for(String s:op_ips)
		{
			t.add(s);
		}
		t.remove(ip.toLowerCase());
		String[] z = new String[t.size()];
		int i = 0;
		for(String s : t)
		{
			z[i++]=s.toLowerCase();
		}
		op_ips = z;
	}
	
	public void addWorld(String world)
	{
		ArrayList<String> t = new ArrayList<String>(world_names.length+1);
		for(String s:world_names)
		{
			if(s.equals(world.toLowerCase())) return;
			t.add(s);
		}
		t.add(world.toLowerCase());
		String[] z = new String[t.size()];
		int i = 0;
		for(String s : t)
		{
			z[i++]=s.toLowerCase();
		}
		world_names = z;
	}
	
	public void removeWorld(String world)
	{
		ArrayList<String> t = new ArrayList<String>(world_names.length+1);
		for(String s:world_names)
		{
			t.add(s);
		}
		t.remove(world.toLowerCase());
		String[] z = new String[t.size()];
		int i = 0;
		for(String s : t)
		{
			z[i++]=s.toLowerCase();
		}
		world_names = z;
	}
	
	public void ban(String ip)
	{
		ArrayList<String> t = new ArrayList<String>(ban_ips.length+1);
		for(String s:ban_ips)
		{
			if(s.equals(ip.toLowerCase())) return;
			t.add(s);
		}
		t.add(ip);
		String[] z = new String[t.size()];
		int i = 0;
		for(String s : t)
		{
			z[i++]=s.toLowerCase();
		}
		ban_ips = z;
	}
	
	public void unban(String ip)
	{
		ArrayList<String> t = new ArrayList<String>(op_ips.length+1);
		for(String s:ban_ips)
		{
			t.add(s);
		}
		t.remove(ip.toLowerCase());
		String[] z = new String[t.size()];
		int i = 0;
		for(String s : t)
		{
			z[i++]=s.toLowerCase();
		}
		ban_ips = z;
	}
	
	
	public void saveWorldConfig(World world)
	{
		try
		{
			NumberFormat nf = NumberFormat.getNumberInstance();
			BufferedWriter out = new BufferedWriter(new FileWriter(map.saveDir + world.name + "/config.txt"));
			String s = "" ;
			s = "map-ticks=" + world.tickSpeed;
			out.write(s,0,s.length());
			out.newLine();
			s = "spawn-x=" + world.spawnX;
			out.write(s,0,s.length());
			out.newLine();
			s = "spawn-y=" + world.spawnY;
			out.write(s,0,s.length());
			out.close();	
		}
		catch (Exception e)
		{
			System.out.println("Error writing config data! " + e.getMessage());
		}
	}

	public void loadWorldConfig(World world)
	{
		try
		{
			NumberFormat nf = NumberFormat.getNumberInstance();
			Config nc = new Config(map.saveDir + world.name + "/config.txt");
			for(int i=0;i<nc.keys;i++)
			{
				String key = nc.key[i];
				String val = nc.value[i];
				System.out.println("Config key found: " + key);
				if(key.contains("spawn-x"))
				{
					world.spawnX = nf.parse(val).intValue();
				}
				else if(key.contains("spawn-y"))
				{
					world.spawnY = nf.parse(val).intValue();
				}
				else if(key.contains("map-ticks"))
				{
					world.changeTickSpeed(nf.parse(val).intValue());
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error reading config data! " + e.getMessage());
		}
	}

	public void saveConfig()
	{
		try
		{
			NumberFormat nf = NumberFormat.getNumberInstance();
			BufferedWriter out = new BufferedWriter(new FileWriter(map.saveDir + "config.txt"));
			String s = "";
			if(NetSender.alg>0)
			{
				s = "send-algorithm=" + NetSender.alg;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(nagle>0)
			{
				s = "use-nagle=" + nagle;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(passOn)
			{
				s = "password=" + pass;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(opPassOn)
			{
				s = "op-password=" + opPass;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(anonMode)
			{
				s = "anonymous-mode=1";
				out.write(s,0,s.length());
				out.newLine();
			}
			if(map_cache_size!=128)
			{
				s = "map-cache-size="+map_cache_size;
				out.write(s,0,s.length());
				out.newLine();
			}
			s = "map-save-duration="+map_save_duration;
			out.write(s,0,s.length());
			out.newLine();
			s = "map-ticks=" + map_tps;
			out.write(s,0,s.length());
			out.newLine();
			s = "tp-for-all=" + tpforall;
			out.write(s,0,s.length());
			out.newLine();
			s = "port=" + po;
			out.write(s,0,s.length());
			out.newLine();
			s = "private-mode=" + privmode;
			out.write(s,0,s.length());
			out.newLine();
			s = "name=" + name;
			out.write(s,0,s.length());
			out.newLine();
			s = "spawn-x=" + spawnX;
			out.write(s,0,s.length());
			out.newLine();
			s = "spawn-y=" + spawnY;
			out.write(s,0,s.length());
			out.close();	
		}
		catch (Exception e)
		{
			System.out.println("Error writing config data! " + e.getMessage());
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
				if(key.contains("spawn-x"))
				{
					spawnX = nf.parse(val).intValue();
				}
				else if(key.contains("spawn-y"))
				{
					spawnY = nf.parse(val).intValue();
				}
				else if(key.contains("send-algorithm"))
				{
					NetSender.alg = nf.parse(val).intValue();
				}
				else if(key.contains("use-nagle"))
				{
					nagle = nf.parse(val).intValue();
				}
				else if(key.contains("map-ticks"))
				{
					map_tps=nf.parse(val).intValue();
				}
				else if(key.contains("tp-for-all"))
				{
					tpforall=nf.parse(val).intValue();
				}
				else if(key.contains("op-password"))
				{
					opPassOn=true;
					opPass=val;
				}
				else if(key.contains("password"))
				{
					passOn=true;
					pass=val;
				}
				else if(key.contains("anonymous-mode"))
				{
					if(nf.parse(val).intValue()>0) anonMode=true;
				}
				else if(key.contains("port"))
				{
					po = nf.parse(val).intValue();
				}
				else if(key.contains("map-cache-size"))
				{
					map_cache_size=nf.parse(val).intValue();
				}
				else if(key.contains("name"))
				{
					name=val;
				}
				else if(key.contains("private-mode"))
				{
					privmode=nf.parse(val).intValue();
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Error reading config data! " + e.getMessage());
		}
	}
	
	public String parseMessage(String al, int id)
	{
		if(al.length()<2) return "$N"; 
		if(!al.substring(0,1).equals("/")) return "$N";
		String[] cmdz = al.substring(1).split(" ");
		String[] cmd = new String[cmdz.length];
		for(int i=0;i<cmdz.length;i++)
		{
			cmd[i]=cmdz[i].toLowerCase();
		}
		if(cmd[0].equals("who") || cmd[0].equals("players") || cmd[0].equals("playerlist") || cmd[0].equals("list") || cmd[0].equals("users"))
		{
			String lol = "";
			int ap = 0;
			for(int i=0;i<255;i++)
			{
				if(clients[i] != null && clients[i].dc == 0)
				{
					if(ap>0)
					{
						lol+=", ";
					}
					ap++;
					lol+=clients[i].nick;
				}
			}
			return "&c" + ap + "/255&f - " + lol;
		}
		else if((cmd[0].equals("tp") || cmd[0].equals("teleport")) && id!=255 && (tpforall>0 || clients[id].op))
		{
			int t = findByNick(cmd[1]);
			if(t<0 || t>255)
			{
				return "No such nickname!";
			}
			else
			{
				if(clients[id].map!=clients[t].map) clients[id].changeMap(clients[t].map);
				clients[id].teleport(clients[t].x,clients[t].y);
				return "";
			}
		}
		else if(cmd[0].equals("warp") && id!=255)
		{
			int t = clients[id].world.warps.findWarpID(cmd[1]);
			if(t>=0)
			{
				Warp w = clients[id].world.warps.warps.get(t);
				clients[id].teleport(w.x,w.y);
				return "";
			}
			else
			{
				return "Warp not found!";
			}
		}
		else if(cmd[0].equals("warps"))
		{
			String wt = "Warps (map " + clients[id].world.name + "): ";
			for(int i=0;i<clients[id].world.warps.warps.size();i++)
			{
				if(clients[id].world.warps.warps.get(i)!=null)
				{
					if(i>0) wt+= ", ";
					wt += clients[id].world.warps.warps.get(i).name;
				}
			}
			return wt;
		}
		else if(cmd[0].equals("worlds") || cmd[0].equals("maps"))
		{
			String wt = "Worlds: ";
			for(int i=0;i<world_names.length;i++)
			{
				if(!world_names[i].startsWith("$"))
				{
					wt += world_names[i];
					wt += ", ";
				}
			}
			return wt.substring(0,wt.length()-2);
		}
		else if((cmd[0].equals("id") || cmd[0].equals("identify")) && id!=255)
		{
			if(!opPassOn) return "Identify disabled!";
			if(cmdz.length>1 && cmdz[1].equals(opPass))
			{
				clients[id].op=true;
				clients[id].sendOpPacket(1);
				return "You're opped now!";
			} else return "Incorrect op password! :(";
		}
		else if(cmd[0].equals("cmds") || cmd[0].equals("help"))
		{
			if(id == 255)
			{
				return "Commands: who warps kick nick deop save ban unban delwarp lock unlock worlds addworld delworld msg";
			}
			else if(clients[id].op)
			{

				return "Commands: who tp warp warps me kick fetch copy paste protect unprotect setspawn say nick op deop save ban unban setwarp delwarp id import export pvp lock unlock worlds addworld delworld load return msg raycast";
			}
			else
			{
				return "Commands: who " + ((tpforall!=0)?"tp ":"") + "warp warps me id worlds load return msg";
			}
		}
		else if(cmd[0].equals("me") && id!=255)
		{
			String st = "* " + clients[id].nick + " ";
			for(int i=1;i<cmdz.length;i++)
			{
				if(i>1) st += " ";
				st=st+cmdz[i];
			}
			while(st.length()>38)
			{
				String st2 = st.substring(0,38);
				st=st.substring(38,st.length());
				clients[id].sendChatMsgAll("&5"+st2);
			}
			clients[id].sendChatMsgAll("&5"+st);
			return "";
		}
		else if((cmd[0].equals("load") || cmd[0].equals("goto") || cmd[0].equals("l") || cmd[0].equals("join") || cmd[0].equals("world")) && id!=255)
		{
			World tm = null;
			try {tm = findWorld(cmdz[1]);}
			catch (Exception e){}
			if(tm==null) return "No such world!";
			else
			{
				clients[id].changeMap(tm.map);
				if (cmd[1].startsWith("$")) clients[id].sendChatMsgAll("&e" + clients[id].nick + " loaded a secret map!");
				else if(!cmd[1].equals("map")) clients[id].sendChatMsgAll("&e" + clients[id].nick + " loaded map &f'" + cmdz[1] + "'!"); 
				else clients[id].sendChatMsgAll("&e" + clients[id].nick + " loaded the main map!");
			}
		}
		else if(cmd[0].equals("return") && id!=255)
		{
			clients[id].changeMap(map);
			clients[id].sendChatMsgAll("&e" + clients[id].nick + " loaded the main map!"); 
		}
		else if(cmd[0].equals("m") || cmd[0].equals("msg"))
		{
			if(cmd.length<3) return "No nickname/message specified!";
			int t = findByNick(cmd[1]);
			if(t<0 || t>255)
			{
				return "No such nickname!";
			}
			else
			{
				String msg = "";
				for(int i=2;i<cmdz.length;i++)
				{
					if(i>2) msg+=" ";
					msg+=cmdz[i];
				}
				String msg2 = "&a[PM] >&f" + clients[id].nick + ": " + msg;
				clients[id].sendChatMsgID(msg2,t);
				return msg2;
			}
		}
		else
		{
			if(id != 255 && !clients[id].op) return "$N";
			if(cmd[0].equals("kick"))
			{
				int t = findByNick(cmd[1]);
				if(t<0 || t>255)
				{
					return "No such nickname!";
				}
				else
				{
					clients[t].kick();
					if(id!=255) System.out.println("[KICK] user " + clients[t].nick + ", by user " + clients[id].nick);
					return clients[t].nick + " has been kicked.";
				}
			}
			else if(cmd[0].equals("fetch") && id !=255)
			{
				for(int i=1;i<cmd.length;i++)
				{
					int t = findByNick(cmd[i]);
					if(t<0 || t>255)
					{
						return "No such nickname!";
					}
					else
					{
						if(clients[id].map!=clients[t].map) clients[t].changeMap(clients[id].map);
						clients[t].teleport(clients[id].x,clients[id].y);
						clients[t].sendChatMsgSelf("Fetched by " + clients[id].nick + "!");
						return "User fetched!";
					}
				}
			}
			else if(cmd[0].equals("raycast") && id!=255)
			{
				String tmap = "(map " + clients[id].world.name + ")";
				if(clients[id].map==map) tmap = "(main map)";
				if(clients[id].world.isRaycasted)
				{
					clients[id].world.isRaycasted=false;
					clients[id].sendChatMsgAll("&eVisibility raycasting OFF &f" + tmap);
					for(int i=0;i<255;i++)
					{
						if(clients[i] != null && clients[i].dc == 0 && clients[i].map == clients[id].map)
						{
							clients[i].setRaycasting(false);
						}
					}
				}
				else
				{
					clients[id].world.isRaycasted=true;
					clients[id].sendChatMsgAll("&eVisibility raycasting ON! &f" + tmap);
					for(int i=0;i<255;i++)
					{
						if(clients[i] != null && clients[i].dc == 0 && clients[i].map == clients[id].map)
						{
						    clients[i].setRaycasting(true);
						}
					}
				}
				return "";
			}
			else if(cmd[0].equals("pvp") && id!=255)
			{
				String tmap = "(map " + clients[id].world.name + ")";
				if(clients[id].map==map) tmap = "(main map)";
				else if (clients[id].world.name.startsWith("$")) tmap = "(secret map)";
				if(clients[id].world.isPvP)
				{
					clients[id].world.isPvP=false;
					clients[id].sendChatMsgAll("&ePvP mode OFF &f" + tmap);
					clients[id].sendChatMsgAll("&cDEATHS:");
					for(int i=0;i<255;i++)
					{
						if(clients[i] != null && clients[i].dc == 0 && clients[i].map == clients[id].map)
						{
							clients[id].sendChatMsgAll("&c" + clients[i].nick + "&7 - &e" + clients[i].deaths + " times");
							clients[i].setPvP(false);
						}
					}
				}
				else
				{
					clients[id].world.isPvP=true;
					clients[id].sendChatMsgAll("&ePvP mode ON! &f" + tmap);
					for(int i=0;i<255;i++)
					{
						if(clients[i] != null && clients[i].dc == 0)
						{
							clients[i].resetPvP();
							if(clients[i].map == clients[id].map) clients[i].setPvP(true);
						}
					}
				}
				return "";
			}
			else if(cmd[0].equals("lock"))
			{
				mapLock=true;
				clients[id].sendChatMsgAll("&cMap locked.");
				return "";
			}
			else if(cmd[0].equals("unlock"))
			{
				mapLock=false;
				clients[id].sendChatMsgAll("&aMap unlocked!");
				return "";
			}
			else if(cmd[0].equals("copy") && id!=255)
			{
				clients[id].copyStage=0;
				clients[id].isCopying=true;
				clients[id].isPasting=false;
				clients[id].isProtecting = false;
				clients[id].isUnprotecting = false;
				return "Click on the first corner.";
			}
			else if(cmd[0].equals("import") && id!=255)
			{
				return clients[id].region.load(cmd[1]);
				
			}
			else if(cmd[0].equals("export") && id!=255)
			{
				clients[id].region.save(cmd[1]);
				return "Exported! (i hope)";
			}
			else if(cmd[0].equals("paste") && id!=255)
			{
				clients[id].isCopying=false;
				clients[id].isProtecting = false;
				clients[id].isPasting=true;
				clients[id].isUnprotecting = false;
				return "Click on the top-left destination corner.";
			}
			else if (cmd[0].equals("protect") && id!=255)
			{
				clients[id].protectStage = 0;
				clients[id].isProtecting = true;
				clients[id].isCopying = false;
				clients[id].isPasting = false;
				clients[id].isUnprotecting = false;
				return "Click on the top-left destination corner.";
			}
			else if (cmd[0].equals("unprotect") && id!=255)
			{
				clients[id].unProtectStage = 0;
				clients[id].isProtecting = false;
				clients[id].isCopying = false;
				clients[id].isPasting = false;
				clients[id].isUnprotecting = true;
				return "Click on the top-left destination corner.";
			}
			else if(cmd[0].equals("setspawn") && id!=255)
			{
				if(clients[id].map!=map)
				{
					clients[id].world.spawnX = clients[id].x;
					clients[id].world.spawnY = clients[id].y;
				}
	 			else
				{
					changeMainSpawnXY(clients[id].x,clients[id].y);
				}
				return "New spawn set at [" + clients[id].x + "," + clients[id].y + "].";
			}
			else if(cmd[0].equals("say") && id!=255)
			{
				String st = "";
				for(int i=1;i<cmdz.length;i++)
				{
					if(i>1) st += " ";
					st=st+cmdz[i];
				}
				while(st.length()>38)
				{
					String st2 = st.substring(0,38);
					st=st.substring(38,st.length());
					clients[id].sendChatMsgAll("&7"+st2);
				}
				clients[id].sendChatMsgAll("&7"+st);
				return "";
			}
			else if(cmd[0].equals("nick"))
			{
				if(cmd.length>2)
				{
					int t = findByNick(cmd[1]);
					if(t<0 || t>255)
					{
						return "No such nickname!";
					}
					else
					{
						String tt = clients[t].nick;
						clients[t].changeNickname(cmd[2]);
						if(id!=255) clients[id].sendChatMsgAll("User " + tt + " is now known as " + cmd[2]);
						return "Nickname of user " + tt + " changed.";
					}
				}
				else if(cmd.length>1 && id!=255)
				{
					String tt = clients[id].nick;
					clients[id].changeNickname(cmd[1]);
					clients[id].sendChatMsgAll("User " + tt + " is now known as " + cmd[1]);
					return "You're now known as " + cmd[1];	
				}
				else return "Not enough parameters!";
			}
			else if(cmd[0].equals("op"))
			{
				for(int i=1;i<cmd.length;i++)
				{
					int t = findByNick(cmd[i]);
					if(t<0 || t>255)
					{
						return "No such nickname!";
					}
					else
					{
						addOp(clients[t].socket.getInetAddress().getHostAddress());
						clients[t].sendChatMsgSelf("You're opped now!");
						clients[t].op=true;
						clients[t].sendOpPacket(1);
						saveNamesFile(op_ips,"ops.txt");
					}
				}
			}
			else if(cmd[0].equals("addworld"))
			{
				if(cmd[1].equalsIgnoreCase("map"))
				{
					return "This map name cannot be used.";
				}
				else if (findWorld(cmdz[1])!=null)
				{
					return "World '" + cmdz[1] + "' already exists.";
				}
				addWorld(cmdz[1]);
				WorldMap tm = new WorldMap(true,map_cache_size,cmdz[1]);
				tm.checkDirs(0,0);
				tm.se = this;
				World w = new World(cmdz[1],tm,map_tps, new Warps());
				loadWorldConfig(w);
				worlds.add(w);
				saveNamesFile(world_names,"worlds.txt");
				return "World '" + cmdz[1] + "' added.";
			}
			else if(cmd[0].equals("delworld"))
			{
				if(cmd[1].equalsIgnoreCase("map"))
				{
					return "This map cannot be deleted.";
				}
				else if (findWorld(cmdz[1])==null)
				{
					return "World '" + cmdz[1] + "' doesn't exist.";
				}
				removeWorld(cmdz[1]);
				saveNamesFile(world_names,"worlds.txt");
				for(int i=0;i<255;i++)
				{
					if(clients[i]!=null && clients[i].dc==0 && clients[i].map==findWorld(cmdz[1]).map)
					{
						clients[i].changeMap(map);
					}
				}
				if(findWorld(cmdz[1])!=null)
				{
					saveWorldConfig(findWorld(cmdz[1]));
					worlds.remove(findWorld(cmdz[1]));
				}
				return "World '" + cmdz[1] + "' deleted.";
			}
			else if(cmd[0].equals("deop"))
			{
				for(int i=1;i<cmd.length;i++)
				{
					int t = findByNick(cmd[i]);
					if(t<0 || t>255)
					{
						return "No such nickname!";
					}
					else
					{
						removeOp(clients[t].socket.getInetAddress().getHostAddress());
						clients[t].sendChatMsgSelf("You're not opped anymore.");
						clients[t].op=false;
						clients[t].sendOpPacket(0);
						saveNamesFile(op_ips,"ops.txt");
					}
				}
			}
			else if(cmd[0].equals("save") || cmd[0].equals("savemap"))
			{
				saveMap();
				System.out.println("[ID " + id + "] Map saved by user " + clients[id].nick + "!");
				return "Map saved!";
			}
			else if(cmd[0].equals("ban"))
			{
				for(int i=1;i<cmd.length;i++)
				{
					int t = findByNick(cmd[i]);
					if(t<0 || t>255)
					{
						return "No such nickname!";
					}
					else
					{
						ban(clients[t].socket.getInetAddress().getHostAddress());
						saveNamesFile(ban_ips,"bans.txt");
						clients[t].kick("Banned!");
						if(id!=255) System.out.println("[BAN] user " + clients[t].nick + ", by user " + clients[id].nick);
					}
				}
			}
			else if(cmd[0].equals("unban"))
			{
				for(String s: ban_ips)
				{
					if(s.equals(cmd[1]))
					{
						unban(s);
						saveNamesFile(ban_ips,"bans.txt");
						return "Person unbanned!";
					}
				}
				return "IP not found!";
			}
			else if(cmd[0].equals("setwarp") && id!=255)
			{
				int t = clients[id].world.warps.findWarpID(cmd[1]);
				if(t>=0)
				{
					clients[id].world.warps.warps.get(t).x=clients[id].x;
					clients[id].world.warps.warps.get(t).y=clients[id].y;
					clients[id].world.saveWarps();
					return "Warp location changed.";
				}
				else
				{
					clients[id].world.warps.warps.add(new Warp(clients[id].x,clients[id].y,cmd[1]));
					clients[id].world.saveWarps();
					return "New warp added.";
				}
			}
			else if(cmd[0].equals("delwarp"))
			{
				int t = clients[id].world.warps.findWarpID(cmd[1]);
				if(t>=0)
				{
					clients[id].world.warps.warps.remove(t);
					clients[id].world.saveWarps();
					return "Warp removed.";
				}
				else
				{
					return "Warp not found!";
				}
			}
			else
			{
				return "No such command!";
			}
		}
		return "";
	}
	
	public String[] readNamesFile(String name)
	{
		try
		{
			FileInputStream fis = new FileInputStream(name);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));
			ArrayList<String> a = new ArrayList<String>(64);
			String t = "";
			while(true)
			{
				t = br.readLine();
				if(t==null)break;
				a.add(t);
			}
			String[] z = new String[a.size()];
			int i = 0;
			for(String s : a)
			{
				z[i++]=s.toLowerCase();
			}
			fis.close();
			return z;
		}
		catch(Exception e)
		{
			try 
			{
				new File(name).createNewFile();
				return readNamesFile(name);
			}
			
			catch (IOException a)
			{
				System.out.println("Couldn't read " + name + "!");
				e.printStackTrace();
				String[] z = new String[0];
				return z;
			}
		}
	}
	

	public void saveNamesFile(String[] data, String name)
	{
		try
		{
			FileOutputStream fos = new FileOutputStream(name);
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			for(String s:data)
			{
				bw.write(s,0,s.length());
				bw.newLine();
			}
			bw.close();
			fos.close();
		}
		catch(Exception e)
		{
			System.out.println("Couldn't write " + name + "!");
			e.printStackTrace();
		}
	}
	
	public Server(String[] args)
	{
		try
		{
			config = new Config("config.txt");
			loadConfig();
			op_ips=readNamesFile("ops.txt");
			System.out.println(op_ips.length + " op IPs!");
			ban_ips=readNamesFile("bans.txt");
			System.out.println(ban_ips.length + " banned IPs!");
			map = new WorldMap(true,map_cache_size,"map");
			map.se = this;
			warps = new Warps();
			world = new World("map",map,map_tps,warps);
			map.checkDirs(0,0);
			worlds=new ArrayList<World>();
			world_names=readNamesFile("worlds.txt");
			System.out.println(world_names.length + " worlds!");
			worlds.add(world);
			
			pluginHandler = new PluginHandler();
			pluginHandler.reloadPlugins();
			for(String wn : world_names)
			{
				WorldMap tm = new WorldMap(true,map_cache_size,wn);
				tm.se = this;
				tm.checkDirs(0,0);
				World w = new World(wn,tm,map_tps,new Warps());
				loadWorldConfig(w);
				worlds.add(w);
			}
			if(args.length>0)
			{
				for(int i=0;i<args.length;i++)
				{
					if(args[i].equals("/a"))
					{
						anonMode=true;
					}
					else if(args[i].equals("/h"))
					{
						System.out.println("64px-srvr\nUsage: 64px-srvr [params]\n\nparams - parameters:\n    /a - anonymous mode (default nicknames) (off by default)\n    /h - show help\n    /p port - change port (25566 is default)");
						System.exit(0);
					}
					else if(args[i].equals("/p"))
					{
						i++;
						po = new Integer(args[i]).intValue();
					}
				}
			}
			servsock = new ServerSocket(po);
			clients = new Client[255];
			out2 = new ByteArrayOutputStream(2048);
			out = new DataOutputStream(out2);
		}
		catch(Exception e)
		{

			System.out.println("Fatal Server init error!");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public byte[] getPacket()
	{
		byte[] t = out2.toByteArray();
		out2.reset();
		return t;
	}
	
	public int findByNick(String nick)
	{
		int curr_id = -1;
		int curr_id_prob = 255; 
		for(int i=0;i<255;i++)
		{
			if(clients[i] != null && clients[i].nick.toLowerCase().startsWith(nick.toLowerCase()) && clients[i].dc == 0)
			{
				int t = clients[i].nick.toLowerCase().compareTo(nick.toLowerCase());
				if(t<0) t=-t;
				if(t<curr_id_prob)
				{
					curr_id = i;
					curr_id_prob = t;
				}
			}
		}
		return curr_id;
	}
	public int findByIP(String ip)
	{
		for(int i=0;i<255;i++)
		{
			if(clients[i] != null && clients[i].socket.getInetAddress().getHostAddress().toLowerCase().equals(ip) && clients[i].dc == 0)
			{
				return i;
			}
		}
		return -1;
	}
	
	public void sendOthers(int a, byte[] arr, int len)
	{
		try
		{
			for(int i=0;i<255;i++)
			{
				if(clients[i] != null && clients[i].id != a && clients[i].dc == 0)
				{
					clients[i].sendPacket(arr);
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal Server sendOthers error!");
			System.exit(1);
		}
	}

	public void sendOthersOnMap(int a, byte[] arr, int len)
	{
		try
		{
			for(int i=0;i<255;i++)
			{
				if(clients[i] != null && clients[i].id != a && clients[i].dc == 0 && clients[i].map == clients[a].map)
				{
					clients[i].sendPacket(arr);
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal Server sendOthers error!");
			System.exit(1);
		}
	}
	
	public void sendAll(byte[] arr)
	{
		sendAll(arr,arr.length);
	}
	public void sendOthers(int a, byte[] arr)
	{
		sendOthers(a,arr,arr.length);
	}
	public void sendOthersOnMap(int a, byte[] arr)
	{
		sendOthersOnMap(a,arr,arr.length);
	}
	public void sendAll(byte[] arr, int len)
	{
		sendOthers(256,arr,arr.length);
	}
	public void sendAllOnMap(int mapPlayerID, byte[] arr)
	{
		sendAllOnMap(arr,arr.length,clients[mapPlayerID].map.mapName);
	}
	public void sendAllOnMap(byte[] arr, String map_name)
	{
		sendAllOnMap(arr,arr.length,map_name);
	}
	public void sendAllOnMap(byte[] arr, int len, String map_name)
	{
		try
		{
			for(int i=0;i<255;i++)
			{
				if(clients[i] != null && clients[i].dc == 0 && clients[i].map.mapName.equalsIgnoreCase(map_name))
				{
					clients[i].sendPacket(arr);
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal Server sendAllOnMap error!");
			System.exit(1);
		}
	}
	
	public void playSound(int x, int y, int id, WorldMap mymap)
	{
		for(int i=0;i<255;i++)
		{
			if(clients[i] != null && clients[i].dc == 0 && clients[i].map==mymap) 
			{
				// This screws up meloders.
				// Adrian, see me after class. --GM
				//if(id>256) clients[i].playSound(x,y,(id%4)+252);
				//else clients[i].playSound(x,y,id);
				
				// samples are played with on/off messages
				// we don't need to make the explicit sound --GM
				if(id<256) clients[i].playSound(x,y,id);
			}
		}
	}

	public void writeString(String s, DataOutputStream out)
	{
		try
		{
			byte[] t = s.getBytes();
			synchronized(out)
			{
				out.writeByte(s.length());
				out.write(t,0,s.length());
			}
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal Server writeString error!");
			try
			{
				synchronized(out){out.writeByte(0x00);}
			}
			catch(Exception ee)
			{
				System.out.println("Fatal Server writeString error!");
			}
		} 
	}
	
	public void start()
	{
		System.out.println("64px-srvr version " + Version.getVersionName());
		System.out.println("Svetlana, I'm sorry.");
		System.out.println("Also let's create servers with Haruhi Suzumiya! (yeah, haruhiism plug FTW)");
		System.out.print("Initializing: #");
		run = true;
		ci = new Input(this);
		Thread ti = new Thread(ci);
		ti.start();
		System.out.print("#");
		AutoSaver cas = new AutoSaver(this);
		cas.mapspeed=(map_save_duration*60);
		Thread ti2 = new Thread(cas);
		ti2.start();
		System.out.print("#");
		Thread ti4 = new Thread(new HeartThread(this));
		if(privmode==0) ti4.start();
		System.out.print("#");
		System.out.println("READY!");
		while(run)
		{
			try
			{
				boolean accepted = false;
				Socket t = servsock.accept();
				for(int i=0;i<255;i++)
				{
					if(clients[i] == null || clients[i].dc > 0)
					{
						clients[i] = new Client(t,map,i,this);
						clients[i].world = findWorld("map");
						Thread t1 = new Thread(clients[i]);
						t1.start();
						accepted = true;
						break;
					}
				}
				if(!accepted)
				{
					DataOutputStream to = new DataOutputStream(t.getOutputStream());
					to.writeByte(0xF5);
					writeString("Too many players!",to);
				}
				Thread.sleep(10);
			}
			catch(Exception e)
			{
				System.out.println("Fatal Server loop error!");
				System.exit(1);
			}
		}
	}

	public boolean mapBeSaved=false;

	public void saveMap()
	{
		if(mapBeSaved) return;
		mapBeSaved=true;
		for(int i=0;i<map.chunks.length;i++)
		{
			if(map.chunks[i].isSet || map.chunks[i].isUsed)
			{
				map.saveChunkFile(i);
			}
		}
		for(World w : worlds)
		{
			for(int i=0;i<w.map.chunks.length;i++)
			{
				if(w.map.chunks[i].isSet || w.map.chunks[i].isUsed)
				{
					w.map.saveChunkFile(i);
				}
			}
			w.saveProtections();
		}
		mapBeSaved=false;
	}
	public void end()
	{
		saveMap();
		while(mapBeSaved) { try{Thread.sleep(10);}catch(Exception e){} } // you never know
		saveConfig();
		for(World w : worlds)
		{
			saveWorldConfig(w);
		}
	}
}
