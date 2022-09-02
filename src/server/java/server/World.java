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
	public ArrayList<Rectangle> protections;

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
		protections = new ArrayList<Rectangle>();
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
		dos.writeByte((byte)0x01); // VERSION
		dos.writeInt(protections.size()); // AMOUNT
		for (Rectangle r: protections)
		{
			dos.writeInt(r.getX());
			dos.writeInt(r.getY());
			dos.writeInt(r.getW());
			dos.writeInt(r.getH());
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
				if(dis.readByte() == 0x01) {
					int amount = dis.readInt();
					for (int i = 0; i < amount; i++)
					{
						int x = dis.readInt();
						int y = dis.readInt();
						int w = dis.readInt();
						int h = dis.readInt();
						setProtected(new Rectangle(x,y,w,h), true);
					}
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

	public void setProtected(Rectangle rectangle, boolean mode) {
		if(mode == true) {
			protections.add(rectangle);
		} else {
			protections.remove(rectangle);
		}
	}
	public boolean isProtected(int x, int y) {
		for(Rectangle r: protections) {
			if(r.insideRect(x, y)) return true;
		}
		return false;
	}
}
