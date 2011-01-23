public class CraftrBlock
{
	public int x = 0;
	public int y = 0;
	public byte type = 0;
	public byte param = 0;
	public byte col = 0;
	public byte chr = 0;
	public boolean setOnlyType = false;
	
	public CraftrBlock()
	{
	}
	public CraftrBlock(int ax, int ay)
	{
		this(ax,ay,0,0,0,0);
	}
	public CraftrBlock(int ax, int ay, int at, int ach, int aco)
	{
		this(ax,ay,at,0,ach,aco);
	}
	public CraftrBlock(int ax, int ay, int at, int ap, int ach, int aco)
	{
		x=ax;
		y=ay;
		type=(byte)at;
		param=(byte)ap;
		chr=(byte)ach;
		col=(byte)aco;
	}
	public CraftrBlock(int ax, int ay, int at, int ap, int ach, int aco, boolean st)
	{
		x=ax;
		y=ay;
		setOnlyType=st;
		type=(byte)at;
		param=(byte)ap;
		chr=(byte)ach;
		col=(byte)aco;
	}
	/*
	public boolean equals(Object other)
	{
		if(other==null) return false;
		if(!(other instanceof CraftrBlock)) return false;
		CraftrBlock co = (CraftrBlock)other;
		return (co.x==x && co.y==y);
	}
	public int hashCode()
	{
		int hash = 1;
		hash = hash * 31 + x;
		hash = hash * 31 + y;
		return hash;
	}
	*/
	public CraftrBlock copy()
	{
		CraftrBlock tz = new CraftrBlock(x,y,type,param,chr,col);
		tz.setOnlyType=setOnlyType;
		return tz;
	}
}