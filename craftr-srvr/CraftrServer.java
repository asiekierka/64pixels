import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.text.*;

public class CraftrServer
{
	public ServerSocket servsock;
	public CraftrClient[] clients;
	public boolean run; 
	public CraftrMap map;
	public CraftrInput ci;
	public DataOutputStream out;
	public ByteArrayOutputStream out2;
	public boolean anonMode;
	public String[] op_ips;
	public String[] ban_ips;
	public int spawnX=0;
	public int spawnY=0;
	public CraftrConfig config;
	public int nagle=0;
	public int map_tps=10;
	public int tpforall=0;
	public boolean passOn = false;
	public String pass;
	public CraftrWarps warps;
	
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
			if(tpforall!=0)
			{
				s = "tp-for-all=" + tpforall;
				out.write(s,0,s.length());
				out.newLine();
			}
			if(passOn)
			{
				s = "password=" + pass;
				out.write(s,0,s.length());
				out.newLine();
			}
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
				else if(key.contains("password"))
				{
					passOn=true;
					pass=val;
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
		String[] cmd = al.substring(1).split(" ");
		for(int i=0;i<cmd.length;i++)
		{
			cmd[i]=cmd[i].toLowerCase();
		}
		if(cmd[0].equals("who"))
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
			return ap + "/255 - " + lol;
		}
		else if(cmd[0].equals("tp") && id!=255 && (tpforall>0 || clients[id].op))
		{
			int t = findByNick(cmd[1]);
			if(t<0 || t>255)
			{
				return "No such nickname!";
			}
			else
			{
				clients[id].teleport(clients[t].x,clients[t].y);
			}
		}
		else if(cmd[0].equals("warp"))
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
					return clients[t].nick + " has been kicked.";
				}
			}
			else if(cmd[0].equals("copy") && id!=255)
			{
				clients[id].copyStage=0;
				clients[id].isCopying=true;
				clients[id].isPasting=false;
				return "Click on the first corner.";
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
			else if(cmd[0].equals("op"))
			{
				int t = findByNick(cmd[1]);
				if(t<0 || t>255)
				{
					return "No such nickname!";
				}
				else
				{
					addOp(clients[t].socket.getInetAddress().getHostAddress());
					clients[t].sendChatMsgSelf("You're opped now!");
					clients[t].op=true;
				}
			}
			else if(cmd[0].equals("deop"))
			{
				int t = findByNick(cmd[1]);
				if(t<0 || t>255)
				{
					return "No such nickname!";
				}
				else
				{
					removeOp(clients[t].socket.getInetAddress().getHostAddress());
					clients[t].sendChatMsgSelf("You're not an op anymore.");
					clients[t].op=false;
				}
			}
			else if(cmd[0].equals("ban"))
			{
				int t = findByNick(cmd[1]);
				if(t<0 || t>255)
				{
					return "No such nickname!";
				}
				else
				{
					ban(clients[t].socket.getInetAddress().getHostAddress());
					clients[t].kick("Just banned!");
				}
			}
			else if(cmd[0].equals("unban"))
			{
				for(String s: ban_ips)
				{
					if(s.equals(cmd[1]))
					{
						unban(s);
						return "Person unbanned!";
					}
				}
				return "IP not found!";
			}
			else if(cmd[0].equals("setwarp"))
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
					return "New warp aWiriumdded.";
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
			map = new CraftrMap(128);
			int po = 25566;
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
						map = new CraftrMap(new Integer(args[i]).intValue());
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
			if(clients[i] != null && clients[i].nick.toLowerCase().equals(nick) && clients[i].dc == 0)
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
	
	public void start()
	{
		run = true;
		ci = new CraftrInput(this);
		Thread ti = new Thread(ci);
		ti.start();
		Thread ti2 = new Thread(new CraftrAutoSaver(this));
		ti2.start();
		CraftrMapThread ti3m = new CraftrMapThread(map);
		ti3m.speed = (1000/map_tps);
		if(ti3m.speed<10 || ti3m.speed>1000) ti3m.speed=100;
		Thread ti3 = new Thread(ti3m);
		if(map_tps>0) ti3.start();
		warps = new CraftrWarps();
		warps.loadFile("warps.dat");
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
	public void saveMap()
	{
		for(int i=0;i<map.chunks.length;i++)
		{
			if(map.chunks[i].isSet || map.chunks[i].isUsed)
			{
				map.saveChunkFile(i);
			}
		}
	}
	public void end()
	{
		saveMap();
		saveNamesFile(op_ips,"ops.txt");
		saveNamesFile(ban_ips,"bans.txt");
		warps.saveFile("warps.dat");
		saveConfig();
	}
}
