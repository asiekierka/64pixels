package server;
import common.*;

import java.io.*;
import java.net.*;

public class HeartThread implements Runnable
{
	public Server se;
	public int speed = 120*1000;
	
	public HeartThread(Server sss)
	{
		se=sss;
	}
	
	public void run()
	{
		while(true)
		{
			InputStream is = null;
			try
			{
				String tt = "http://admin.64pixels.org/heartbeat.php?name=" + se.name + "&port=" + se.po + "&players=" + se.countPlayers() + "&maxplayers=255&version=" + Version.getProtocolVersion();
				tt = tt.replaceAll(" ","%20");
				URL u1 = new URL(tt);
				is = u1.openStream();
				int count = 1;
				while(count>0)
				{
					byte[] t = new byte[64];
					count=is.read(t,0,64);
				}	
				Thread.sleep(speed);
			}
			catch(Exception e){e.printStackTrace();try{Thread.sleep(speed); is.close();}catch(Exception e2){}}
		}
	}
}
