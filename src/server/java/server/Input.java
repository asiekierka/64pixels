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
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String input = null;
		boolean running = true;
		while(serv.run && running)
		{
			try
			{
				input = stdin.readLine();
				if(input.length()>=2)
				{
					String output = serv.parseMessage(input,255);
					if(!output.equals("$N") && !output.equals("")) System.out.println(output);
				}
				Thread.sleep(10);
			}
			catch(NullPointerException ne)
			{
				System.out.println("Input null pointer exception, quitting...");
				running = false;
			}
			catch(Exception e)
			{
				System.out.println("Non-fatal Input error.");
				e.printStackTrace();
			}
		}
	}
}
