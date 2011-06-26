package server;
import common.*;

public class CraftrWorld
{
	public String name;
	public CraftrMap map;
	private CraftrMapThread mt;
	private Thread t;

	public CraftrWorld(String n, CraftrMap m, int speed)
	{
		name=n;
		map=m;
		mt = new CraftrMapThread(map);
		mt.speed = (1000/speed);
		if(mt.speed<10 || mt.speed>1000) mt.speed=100;
		t = new Thread(mt);
		t.start();
	}

	public void stop()
	{
		mt.isRunning = false;
	}
}