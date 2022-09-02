package server;
import common.*;

public class AutoSaver implements Runnable
{
	public Server server;
	public int mapspeed=600;
	
	public AutoSaver(Server s)
	{
		server=s;
	}
	
	public void run()
	{
		while(server.run)
		{
			try { Thread.sleep(mapspeed*1000); }
			catch(Exception e){}
			server.saveMap();
			System.out.println("Map saved!");
		}
	}
}
