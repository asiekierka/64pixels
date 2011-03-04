package server;
import common.*;

public class CraftrAutoSaver implements Runnable
{
	public CraftrServer serv;
	public int mapspeed=600;
	
	public CraftrAutoSaver(CraftrServer s)
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
