package common;

import java.util.*;

public class ExtendedBlock
{
	private final int x;
	private final int y;
	private byte[] data;
	private int flags;

	public ExtendedBlock(int tx, int ty, byte[] tdata, int tflags) {
		x=tx;
		y=ty;
		data = tdata;
		flags=tflags;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	public int getFlags() {
		return flags;
	}
	public boolean isNetwork() {
		return (flags&1)==1;
	}

	public byte[] getData() {
		return data;
	}
	public void setData(byte[] _data) {
		data = _data;
	}

	public boolean equals(Object other) {
		if(other==null) return false;
		if(!(other instanceof ExtendedBlock)) return false;
		ExtendedBlock co = (ExtendedBlock)other;
		return (co.x==x && co.y==y && Arrays.equals(data,co.data));
	}
	public int hashCode() {
		int hash=1;
		hash=hash*31+x;
		hash=hash*31+y;
		return hash;
	}
}
