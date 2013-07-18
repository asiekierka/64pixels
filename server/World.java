package server;
import common.*;
import java.io.*;
import java.util.*;
import java.awt.Point;

public class World
{
	public String name;
	public WorldMap map;
	private MapThread mt;
	private Thread t;
	public boolean isPvP = false;
	public Warps warps;
	public int tickSpeed = 10;
	public int spawnX = 0;
	public int spawnY = 0;
	public boolean isRaycasted = false;

	public World(String n, WorldMap m, int speed, Warps w)
	{
		name=n;
		map=m;
		warps=w;
		if(!name.equals("map")) warps.loadFile(name + "/warps.dat");
		else warps.loadFile("warps.dat");
		loadProtections();
		mt = new MapThread(map);
		tickSpeed = speed;
		if(tickSpeed>100 || tickSpeed<=0) tickSpeed=10;
		mt.speed = (1000/tickSpeed);
		t = new Thread(mt);
		t.start();
	}

	public void stop()
	{
		mt.isRunning = false;
	}

	public void saveWarps()
	{
		if(name!="map") warps.saveFile(name + "/warps.dat");
		else warps.saveFile("warps.dat");
	}
	
	public void saveProtections()
	{
		File f = new File(name + "/protections.dat");
		try
		{
		if (!f.exists())
			f.createNewFile();
			
		FileOutputStream fos = new FileOutputStream(f);
		DataOutputStream dos = new DataOutputStream(fos);
		HashSet<Point> prot = map.getProtections();
		for (Point p : prot)
		{
			dos.writeLong((long)p.getX());
			dos.writeLong((long)p.getY());
		}
		} catch (IOException e) {System.out.println("Cannot create protections file!");}
	}
	
	public void loadProtections()
	{
		File f = new File(name + "/protections.dat");
		try
		{
			if (f.exists())
			{
				FileInputStream fis = new FileInputStream(f);
				DataInputStream dis = new DataInputStream(fis);
				for (int i = 0; i < ((f.length()/8)); i += 2)
				{
					map.setProtected((int)dis.readLong(), (int)dis.readLong(), true);
				}
			}
		} catch (Exception e) {System.out.println("LoadProtections error!");}
	}

	public void changeTickSpeed(int ts)
	{
		tickSpeed=ts;
		if(tickSpeed>100 || tickSpeed<=0) tickSpeed=10;
		mt.speed=(1000/tickSpeed);
	}
}
