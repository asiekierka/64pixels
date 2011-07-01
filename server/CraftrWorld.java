package server;
import common.*;

public class CraftrWorld
{
	public String name;
	public CraftrMap map;
	private CraftrMapThread mt;
	private Thread t;
	public boolean isPvP = false;
	public CraftrWarps warps;
	public int tickSpeed = 10;
	public int spawnX = 0;
	public int spawnY = 0;

	public CraftrWorld(String n, CraftrMap m, int speed, CraftrWarps w)
	{
		name=n;
		map=m;
		warps=w;
		if(!name.equals("map")) warps.loadFile(name + "/warps.dat");
		else warps.loadFile("warps.dat");
		mt = new CraftrMapThread(map);
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