package server;
import common.*;

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

	public void changeTickSpeed(int ts)
	{
		tickSpeed=ts;
		if(tickSpeed>100 || tickSpeed<=0) tickSpeed=10;
		mt.speed=(1000/tickSpeed);
	}
}
