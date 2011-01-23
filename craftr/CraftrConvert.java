public final class CraftrConvert
{
	public CraftrConvert()
	{
	
	}
	
	public static final byte[] shortArray(short i)
	{
		byte[] out = new byte[2];
		out[0] = (byte)(i&255);
		out[1] = (byte)((i>>8)&255);
		return out;
	}
	
	public static final byte[] intArray(short i)
	{
		byte[] out = new byte[4];
		out[0] = (byte)(i&255);
		out[1] = (byte)((i>>8)&255);
		out[2] = (byte)((i>>16)&255);
		out[3] = (byte)((i>>24)&255);
		return out;
	}
	
	public static final short arrShort(byte[] i)
	{
		return (short)(i[0] | (i[1]<<8));
	}
	
	public static final int arrInt(byte[] i)
	{
		return i[0] | (i[1]<<8) | (i[2]<<16) + (i[3]<<24);
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
				if(test<=0 || test>32767) test=25566;
				return test;
			}
			else return 25566;
		}
		catch(Exception e)
		{
			System.out.println("getPort error!");
			e.printStackTrace();
			return 25566;
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