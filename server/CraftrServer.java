package server;
import common.*;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;

public class CraftrServer extends CraftrServerShim
{
	public ServerSocket servsock;
	public CraftrClient[] clients;
	public String name = "64pixels server";
	public boolean run; 
	public CraftrMap map;
	public CraftrInput ci;
	//public DataOutputStream out; // SHIMMED
	public ByteArrayOutputStream out2;
	public boolean anonMode;
	public String[] op_ips;
	public String[] ban_ips;
	public int spawnX=0;
	public int privmode=0;
	public int spawnY=0;
	public CraftrConfig config;
	public int nagle=0;
	public int map_tps=10;
	public int map_save_duration=10;
	public int tpforall=0;
	public boolean passOn = false;
	public String pass;
	public boolean opPassOn = false;
	public String opPass;
	public CraftrWarps warps;
	public int po = 25566;

	public int countPlayers()
	{
		int t = 0;
		for(int i=0;i<255;i++)
		{
			if(clients[i]!=null && clients[i].dc==0) t++;
		}
		return t;
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
		t.add(ip);
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
	
	
	public void saveConfig()
	{
		try
		{
			NumberFormat nf = NumberFormat.getNumberInstance();
			BufferedWriter out = new BufferedWriter(new FileWriter(map.saveDir + "config.txt"));
			String s = "";
			if(CraftrNetSender.alg>0)
			{
				s = "send-algorithm=" + CraftrNetSender.alg;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(nagle>0)
			{
				s = "use-nagle=" + nagle;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(map_tps!=10)
			{
				s = "map-ticks=" + map_tps;
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
			if(map.chunks.length!=128)
			{
				s = "map-cache-size="+map.chunks.length;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(map_save_duration!=10)
			{
				s = "map-save-duration="+map_save_duration;
				out.write(s,0,s.length());
				out.newLine();
			}
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
					CraftrNetSender.alg = nf.parse(val).intValue();
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
					map = new CraftrMap(true,nf.parse(val).intValue());
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
		if(cmd[0].equals("who") || cmd[0].equals("players") || cmd[0].equals("playerlist"))
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
				clients[id].teleport(clients[t].x,clients[t].y);
				return "";
			}
		}
		else if(cmd[0].equals("warp") && id!=255)
		{
			int t = warps.findWarpID(cmd[1]);
			if(t>=0)
			{
				CraftrWarp w = warps.warps.get(t);
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
			String wt = "Warps: ";
			for(int i=0;i<warps.warps.size();i++)
			{
				if(warps.warps.get(i)!=null)
				{
					if(i>0) wt+= " ";
					wt += warps.warps.get(i).name;
				}
			}
			return wt;
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
				return "Commands: who warps kick nick deop save ban unban delwarp";
			}
			else if(clients[id].op)
			{
				return "Commands: who tp warp warps me kick fetch copy paste setspawn say nick op deop save ban unban setwarp delwarp id import export";
			}
			else
			{
				return "Commands: who " + ((tpforall!=0)?"tp ":"") + "warp warps me id";
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
						clients[t].teleport(clients[id].x,clients[id].y);
						clients[t].sendChatMsgSelf("Fetched by " + clients[id].nick + "!");
						return "User fetched!";
					}
				}
			}
			else if(cmd[0].equals("copy") && id!=255)
			{
				clients[id].copyStage=0;
				clients[id].isCopying=true;
				clients[id].isPasting=false;
				return "Click on the first corner.";
			}
			else if(cmd[0].equals("import") && id!=255)
			{
				return clients[id].cc.load(cmd[1]);
				
			}
			else if(cmd[0].equals("export") && id!=255)
			{
				clients[id].cc.save(cmd[1]);
				return "Exported! (i hope)";
			}
			else if(cmd[0].equals("paste") && id!=255)
			{
				clients[id].isCopying=false;
				clients[id].isPasting=true;
				return "Click on the top-left destination corner.";
			}
			else if(cmd[0].equals("setspawn") && id!=255)
			{
				spawnX=clients[id].x;
				spawnY=clients[id].y;
				return "New spawn set.";
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
				int t = warps.findWarpID(cmd[1]);
				if(t>=0)
				{
					warps.warps.get(t).x=clients[id].x;
					warps.warps.get(t).y=clients[id].y;
					return "Warp location changed.";
				}
				else
				{
					warps.warps.add(new CraftrWarp(clients[id].x,clients[id].y,cmd[1]));
					return "New warp added.";
				}
			}
			else if(cmd[0].equals("delwarp"))
			{
				int t = warps.findWarpID(cmd[1]);
				if(t>=0)
				{
					warps.warps.remove(t);
					warps.saveFile("warps.dat");
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
			System.out.println("Couldn't read " + name + "!");
			e.printStackTrace();
			String[] z = new String[0];
			return z;
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
	
	public CraftrServer(String[] args)
	{
		try
		{
			config = new CraftrConfig("config.txt");
			loadConfig();
			op_ips=readNamesFile("ops.txt");
			System.out.println(op_ips.length + " op IPs!");
			ban_ips=readNamesFile("bans.txt");
			System.out.println(ban_ips.length + " banned IPs!");
			map = new CraftrMap(true,128);
			if(args.length>0)
			{
				for(int i=0;i<args.length;i++)
				{
					if(args[i].equals("/a"))
					{
						anonMode=true;
					}
					else if(args[i].equals("/c"))
					{
						i++;
						map = new CraftrMap(true,new Integer(args[i]).intValue());
					}
					else if(args[i].equals("/h"))
					{
						System.out.println("64px-srvr\nUsage: 64px-srvr [params]\n\nparams - parameters:\n    /a - anonymous mode (default nicknames) (off by default)\n    /c 128 - change map buffer size (128 is default)\n    /h - show help\n    /p port - change port (25566 is default)");
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
			clients = new CraftrClient[255];
			map.se = this;
			out2 = new ByteArrayOutputStream(2048);
			out = new DataOutputStream(out2);
		}
		catch(Exception e)
		{

			System.out.println("Fatal CraftrServer init error!");
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
		for(int i=0;i<255;i++)
		{
			if(clients[i] != null && clients[i].nick.toLowerCase().startsWith(nick.toLowerCase()) && clients[i].dc == 0)
			{
				return i;
			}
		}
		return -1;
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
				    //System.out.println("Sending packet " + arr[0] + " id " + a + ", " + i);
					clients[i].sendPacket(arr);
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal CraftrServer sendOthers error!");
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
	public void sendAll(byte[] arr, int len)
	{
		try
		{
			for(int i=0;i<255;i++)
			{
				if(clients[i] != null && clients[i].dc == 0)
				{
					clients[i].sendPacket(arr);
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal CraftrServer sendAll error!");
			System.exit(1);
		}
	}
	
	public void playSound(int x, int y, int id)
	{
		for(int i=0;i<255;i++)
		{
			if(clients[i] != null && clients[i].dc == 0) 
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
	
	public void start()
	{
		System.out.println("64px-srvr version 0.0.12.1");
		System.out.println("Bonus points for GreaseMonkey's revolution(ary) plans");
		System.out.print("Initializing: #");
		run = true;
		ci = new CraftrInput(this);
		Thread ti = new Thread(ci);
		ti.start();
		System.out.print("#");
		CraftrAutoSaver cas = new CraftrAutoSaver(this);
		cas.mapspeed=(map_save_duration*60);
		Thread ti2 = new Thread(cas);
		ti2.start();
		System.out.print("#");
		CraftrMapThread ti3m = new CraftrMapThread(map);
		ti3m.speed = (1000/map_tps);
		Thread ti4 = new Thread(new CraftrHeartThread(this));
		if(privmode==0) ti4.start();
		System.out.print("#");
		if(ti3m.speed<10 || ti3m.speed>1000) ti3m.speed=100;
		Thread ti3 = new Thread(ti3m);
		if(map_tps>0) ti3.start();
		System.out.print("#");
		warps = new CraftrWarps();
		warps.loadFile("warps.dat");
		System.out.println("#");
		System.out.println("READY!");
		while(run)
		{
			try
			{
				Socket t = servsock.accept();
				for(int i=0;i<255;i++)
				{
					if(clients[i] == null || clients[i].dc > 0)
					{
						clients[i] = new CraftrClient(t,map,i,this);
						Thread t1 = new Thread(clients[i]);
						t1.start();
						break;
					}
				}
			}
			catch(Exception e)
			{
				System.out.println("Fatal CraftrServer loop error!");
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
/*
		saveNamesFile(op_ips,"ops.txt");
		saveNamesFile(ban_ips,"bans.txt");
		warps.saveFile("warps.dat");
*/
		mapBeSaved=false;
	}
	public void end()
	{
		saveMap();
		while(mapBeSaved) { try{Thread.sleep(2);}catch(Exception e){} } // bleh
		saveConfig();
	}
}
