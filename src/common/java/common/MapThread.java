package common;
import java.util.*;
public class MapThread implements Runnable
{
	public WorldMap map;
	public int speed = 100;
	public long wps = 0;
	public boolean isRunning = true;

	public MapThread(WorldMap m)
	{
		map=m;
	}
	
	public void run()
	{
		int overhead=0;
		while(isRunning)
		{
			try
			{
				if(speed>0)
				{
					if(overhead<speed)
					{
						Thread.sleep(speed-overhead);
						overhead=0;
					} else {
						overhead-=speed;
					}
				}
			}
			catch(Exception e){}
			Date told = new Date();
			map.physics.tick(map);
			overhead+=(new Date().getTime())-told.getTime();
			wps++;
		}
	}
}
