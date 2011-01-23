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
				while(packets.size()>0)
				{
					byte[] t;
					synchronized(packets) { t = packets.poll(); }
					if(t!=null && t.length>0) out.write(t);
				}
				//Thread.sleep();
				//out.flush();
				Thread.sleep(33);
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