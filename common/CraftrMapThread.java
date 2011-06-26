package common;

public class CraftrMapThread implements Runnable
{
	public CraftrMap map;
	public int speed = 100;
	public long wps = 0;
	public boolean isRunning = true;

	public CraftrMapThread(CraftrMap m)
	{
		map=m;
	}
	
	public void run()
	{
		while(isRunning)
		{
			try
			{
			if(speed>0)Thread.sleep(speed);
			}
			catch(Exception e){}
			map.physics.tick(map);
			wps++;
		}
	}
}
