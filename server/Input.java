package server;
import common.*;

import java.lang.*;
import java.io.*;
import java.net.*;

public class Input implements Runnable
{
	public Server serv;
	
	public Input(Server s)
	{
		serv = s;
	}
	
	public void run()
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String inp = null;
		boolean mrun = true;
		while(serv.run && mrun)
		{
			try
			{
				inp = br.readLine();
				if(inp.length()>=2)
				{
					String t = serv.parseMessage(inp,255);
					if(!t.equals("$N") && !t.equals("")) System.out.println(t);
				}
			Thread.sleep(10);
			}
			catch(NullPointerException ne)
			{
				System.out.println("Input null pointer exception, quitting...");
				mrun = false;
			}
			catch(Exception e)
			{
				System.out.println("Non-fatal Input error.");
				e.printStackTrace();
			}
		}
	}
}
