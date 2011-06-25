package common;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class CraftrNetSender implements Runnable
{
	public boolean isRunning;
	public LinkedBlockingQueue<byte[]> packets;
	public OutputStream out;
	public static int alg = -1;
	
	public CraftrNetSender(OutputStream tout)
	{
		isRunning=true;
		packets = new LinkedBlockingQueue<byte[]>();
		out = tout;
	}

	public void run()
	{
		try
		{
			while(isRunning)
			{
				switch(alg)
				{
					case -1:
					case 0:
						byte[] t;
						while(packets.size()>0)
						{
							synchronized(packets) { t = packets.poll(); }
							if(t!=null) out.write(t);
						}
						break;
				}
				out.flush();
				if(alg!=-1) Thread.sleep(2);
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal CraftrNetSender error!");
			e.printStackTrace();
			isRunning = false;
		}
	}
}
