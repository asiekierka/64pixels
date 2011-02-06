public class CraftrMapThread implements Runnable
{
	public CraftrMap map;
	public int speed = 100;
	public long wps = 0;
	public CraftrMapThread(CraftrMap m)
	{
		map=m;
	}
	
	public void run()
	{
		while(true)
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
