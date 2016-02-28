package common;

public class Version
{
	private static final int protocolVer = 269;
	private static final int releaseVer = 0;
	private static final int majorVer = 2;
	private static final int minorVer = 1;
	private static final int patchVer = 0;
	private static final String addon = "";
	//private static final String addon = "";
	public Version()
	{
	}
	public static int getProtocolVersion()
	{
		return protocolVer;
	}
	public static String getVersionName()
	{
		String temp = "" + releaseVer + "." + majorVer;
		if(minorVer != 0 || patchVer != 0)
		{
			temp += "." + minorVer;
			if(patchVer!=0) temp += "." + patchVer;
		}
		return temp+addon;
	}
}
