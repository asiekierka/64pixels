package server;
import common.*;

import java.io.*;
import java.net.*;

public class HeartThread implements Runnable
{
	public Server server;
	public int speed = 120*1000;
	
	public HeartThread(Server s)
	{
		server=s;
	}
	
	public void run()
	{
		while(true)
		{
			InputStream input = null;
			try
			{
				String urlString = "http://admin.64pixels.org/heartbeat.php?name=" + server.name + "&port=" + server.po + "&players=" + server.countPlayers() + "&maxplayers=255&version=" + Version.getProtocolVersion();
				urlString = urlString.replaceAll(" ","%20");
				URL url = new URL(urlString);
				input = url.openStream();
				int count = 1;
				byte[] temp = new byte[64];
				while(count>0) { count = input.read(temp,0,64); }
				Thread.sleep(speed);
			}
			catch(Exception e) {
				try
				{
					System.out.println("Serverlist seems to be down!");
					Thread.sleep(speed); 
					input.close();
				}
				catch(Exception e2)
				{
					System.out.println("Serverlist is down!");
				}
			}
		}
	}
}
