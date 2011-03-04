package server;
import common.*;

import java.io.*;
import java.net.*;

public class CraftrHeartThread implements Runnable
{
	public CraftrServer se;
	public int speed = 120*1000;
	
	public CraftrHeartThread(CraftrServer sss)
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
				String tt = "http://64pixels.org/heartbeat.php?name=" + se.name + "&port=" + se.po + "&players=" + se.countPlayers() + "&maxplayers=255";
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
			catch(Exception e){e.printStackTrace();try{is.close();}catch(Exception e2){}}
		}
	}
}
