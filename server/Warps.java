package server;
import common.*;

import java.util.*;
import java.io.*;
public class Warps
{
	ArrayList<Warp> warps;
	private DataInputStream in;
	private DataOutputStream out;
	public Warps()
	{
		warps=new ArrayList<Warp>(256);
	}
	public String readString()
	{
		try
		{
			int la = in.readUnsignedByte();
			byte[] t = new byte[la];
			in.read(t,0,la);
			return new String(t);
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal Warps readString error!");
			return "";
		} 
	}
	
	public void writeString(String s)
	{
		try
		{
			byte[] t = s.getBytes();
			synchronized(out)
			{
				out.writeByte(s.length());
				out.write(t,0,s.length());
			}
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal Warps writeString error!");
			try
			{
				synchronized(out){out.writeByte(0x00);}
			}
			catch(Exception ee)
			{
			}
		} 
	}
	
	public int findWarpID(String name)
	{
		for(int i=0;i<warps.size();i++)
		{
			if(warps.get(i)!=null && warps.get(i).name.equals(name)) return i;
		}
		return -1;
	}
	public void saveFile(String filename)
	{
		try
		{
			FileOutputStream fout = new FileOutputStream(filename);
			out = new DataOutputStream(fout);
			out.writeInt(warps.size());
			for(int i=0;i<warps.size();i++)
			{
				out.writeInt(warps.get(i).x);
				out.writeInt(warps.get(i).y);
				writeString(warps.get(i).name);
			}
		}
		catch(Exception e) {}
	}
	
	public void loadFile(String filename)
	{
		try
		{
			FileInputStream fin = new FileInputStream(filename);
			in = new DataInputStream(fin);
			int length = in.readInt();
			warps=new ArrayList<Warp>(length+16);
			if(length>0)
			{
				for(int i=0;i<length;i++)
				{
					int x = in.readInt();
					int y = in.readInt();
					warps.add(new Warp(x,y,readString()));
				}
			}
			in.close();
			fin.close();
		}
		catch(Exception e)
		{}
	}
}