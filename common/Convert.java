package common;

public final class Convert
{
	public Convert()
	{
	}

	public static final int getPort(String hn)
	{
		try
		{
			int splitter = hn.indexOf(":");
			if(splitter>0)
			{
				String tmp = hn.substring(splitter+1,hn.length());
				int test = new Integer(tmp).intValue();
				if(test<=0 || test>32767) test=20064;
				return test;
			}
			else return 20064;
		}
		catch(Exception e)
		{
			System.out.println("getPort error!");
			e.printStackTrace();
			return 20064;
		}
	}
	public static final String getHost(String hn)
	{
		try
		{
			int splitter = hn.indexOf(":");
			if(splitter>0)
			{
				String tmp = hn.substring(0,splitter);
				if(tmp=="") tmp="127.0.0.1";
				return tmp;
			} else return hn;
		}
		catch(Exception e)
		{
			System.out.println("getHost error! using localhost...");
			e.printStackTrace();
			return "127.0.0.1";
		}
	}
}
