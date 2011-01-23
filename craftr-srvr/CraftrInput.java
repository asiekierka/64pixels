import java.lang.*;
import java.io.*;
import java.net.*;

public class CraftrInput implements Runnable
{
	public CraftrServer serv;
	
	public CraftrInput(CraftrServer s)
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
			}
			catch(NullPointerException ne)
			{
				System.out.println("CraftrInput null pointer exception, quitting...");
				mrun = false;
			}
			catch(Exception e)
			{
				System.out.println("I bet it's a non-fatal CraftrInput error.");
				e.printStackTrace();
			}
		}
	}
}