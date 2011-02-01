import java.lang.*;
import java.util.*;
import java.io.*;

public class CraftrConfig
{
	public String[] key;
	public String[] value;
	public String[] keyo;

	public static final int defLen = 512;
	public int maxkeys, keys;
	public CraftrConfig()
	{
		key = new String[defLen];
		keyo = new String[defLen];
		value = new String[defLen];
		maxkeys = 512;
	}
	public CraftrConfig(int len)
	{
		keyo = new String[len];
		key = new String[len];
		value = new String[len];
		maxkeys = len;
	}
	public CraftrConfig(String filename)
	{
		keyo = new String[defLen];
		key = new String[defLen];
		value = new String[defLen];
		maxkeys = 512;
		load(filename);
	}
	public CraftrConfig(String filename, int len)
	{
		keyo = new String[len];
		key = new String[len];
		value = new String[len];
		maxkeys = len;
		load(filename);
	}
	
	public void load(String fn)
	{
		keys=0;
		String tl = "";
		BufferedReader in;
		try
		{
			File tst = new File(fn);
			if(tst != null && tst.exists())
			{
				in = new BufferedReader(new FileReader(fn));
				while(tl != null)
				{
					tl = in.readLine();
					if(tl != null)
					{
						String[] t = tl.split("=");
						if(t.length==2)
						{
							keyo[keys] = t[0];
							key[keys]=t[0].toLowerCase();
							value[keys] = t[1];
							keys++;
						}
					}
				}
				in.close();
			}
		}
		catch(Exception e)
		{
			System.out.println("Couldn't load config file! " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void clear()
	{
		keys=0;
		for(int i=0;i<maxkeys;i++)
		{
			key[i]="";
			keyo[i]="";
			value[i]="";
		}
	}
	
	public void add(String k, String v)
	{
		key[keys]=k;
		value[keys]=v;
		keys++;
	}
	
	public void remove(String k)
	{
		for(int i=0;i<keys;i++)
		{
			if(key[i] == k)
			{
				System.arraycopy(key,i+1,key,i,keys-i);
				System.arraycopy(value,i+1,value,i,keys-i);
				return;
			}
		}
	}
	
	public void save(String fn)
	{
		BufferedWriter fw;
		try
		{
			fw = new BufferedWriter(new FileWriter(fn));
			for (int i=0;i<keys;i++)
			{
				String ts = key[i] + "=" + value[i] + '\n';
				fw.write(ts,0,ts.toCharArray().length);
			}
			fw.close();
		}
		catch(Exception e)
		{
			System.out.println("Couldn't save config file! " + e.getMessage());
		}
	}
}
