public class CraftrAutoSaver implements Runnable
{
	public CraftrServer serv;
	
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
			Thread.sleep(10*60*1000);
			}
			catch(Exception e){}
			serv.saveMap();
			System.out.println("Map saved!");
		}
	}
}