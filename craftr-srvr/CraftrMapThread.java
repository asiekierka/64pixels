public class CraftrMapThread implements Runnable
{
	public CraftrMap map;
	public int speed = 100;
	
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
			Thread.sleep(speed);
			}
			catch(Exception e){}
			map.loop();
		}
	}
}