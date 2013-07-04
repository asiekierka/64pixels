package server;
import common.*;

public class AutoSaver implements Runnable
{
	public Server serv;
	public int mapspeed=600;
	
	public AutoSaver(Server s)
	{
		serv=s;
	}
	
	public void run()
	{
		while(serv.run)
		{
			try
			{
			Thread.sleep(mapspeed*1000);
			}
			catch(Exception e){}
			serv.saveMap();
			System.out.println("Map saved!");
		}
	}
}
