package common;

public class CraftrBlockPos
{
	private final int x;
	private final int y;
	
	public CraftrBlockPos(int tx, int ty)
	{
		x=tx;
		y=ty;
	}
	public int getX()
	{
		return x;
	}
	public int getY()
	{
		return y;
	}
	public boolean equals(Object other)
	{
		if(other==null) return false;
		if(!(other instanceof CraftrBlockPos)) return false;
		CraftrBlockPos co = (CraftrBlockPos)other;
		return (co.x==x && co.y==y);
	}
	public int hashCode()
	{
		int hash=1;
		hash=hash*31+x;
		hash=hash*31+y;
		return hash;
	}
}
