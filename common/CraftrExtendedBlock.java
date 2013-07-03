package common;

import java.util.*;

public class CraftrExtendedBlock
{
	private final int x;
	private final int y;
	private byte[] data;

	public CraftrExtendedBlock(int tx, int ty, byte[] tdata)
	{
		x=tx;
		y=ty;
		data = tdata;
	}
	public int getX()
	{
		return x;
	}
	public int getY()
	{
		return y;
	}
	public byte[] getData()
	{
		return data;
	}
	public void setData(byte[] _data)
	{
		data = _data;
	}
	public boolean equals(Object other)
	{
		if(other==null) return false;
		if(!(other instanceof CraftrExtendedBlock)) return false;
		CraftrExtendedBlock co = (CraftrExtendedBlock)other;
		return (co.x==x && co.y==y && Arrays.equals(data,co.data));
	}
	public int hashCode()
	{
		int hash=1;
		hash=hash*31+x;
		hash=hash*31+y;
		return hash;
	}
}
