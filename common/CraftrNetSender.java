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
						while(packets.size()>0)
						{
							byte[] t;
							synchronized(packets) { t = packets.poll(); }
							if(t!=null && t.length>0) out.write(t);
						}
						break;
					case 1:
						byte[] arr = new byte[1024];
						int arrp = 0;
						while(packets.size()>0 && arrp<1024)
						{
							byte[] tmp;
							synchronized(packets)
							{
								tmp = packets.poll();
							}
							int i = 0;
							if(tmp.length+arrp>=1024)
							{
								out.write(arr,0,arrp);
								arr = new byte[1024];
								arrp=0;
							}
							System.arraycopy(tmp,i,arr,arrp,tmp.length-i);
							arrp+=tmp.length-i;
						}
						if(arrp>0) out.write(arr,0,arrp);
						break;
				}
				if(alg != -1) Thread.sleep(10);
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
