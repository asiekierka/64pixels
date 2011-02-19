public class CraftrBlock
{
	public int x = 0;
	public int y = 0;
	private byte[] block = new byte[6];
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
	public CraftrBlock(int ax, int ay, int at, int ap, int ach, int aco, boolean st)
	{
		x=ax;
		y=ay;
		setOnlyType=st;
		block[0]=(byte)at;
		block[1]=(byte)ap;
		if(block[0]==(byte)-1)
		{
			block[4]=(byte)ach;
			block[5]=(byte)aco;
		}
		else
		{
			block[2]=(byte)ach;
			block[3]=(byte)aco;
		}
	}
	public CraftrBlock(int ax, int ay, int at, int ap, int ach, int aco)
	{
		this(ax,ay,at,ap,ach,aco,false);	
	}
	public CraftrBlock(int ax, int ay, byte[] bd)
	{
		x=ax;
		y=ay;
		block=bd;
	}

	public boolean isEmpty()
	{
		if(isPushable()) return false;
		if(block[0]==0 || block[0]==2 || block[0]==5 || (block[0]==6 && ((int)block[1]&0x80)!=0) || block[0]==8) return true;
		return false;
	}

	public boolean isWiriumNeighbour()
	{
		if((block[0]>=2 && block[0]<=7) || block[0]==9 || block[0]==10) return true;
		return false;
	}

	public boolean isPushable()
	{
		if(block[5]!=0) return true;
		return false;
	}

	public int getType()
	{
		return 0xFF&(int)block[0];
	}

	public int getTypeWithVirtual()
	{
		if(isPushable()) return -1;
		return 0xFF&(int)block[0];
	}

	public int getParam()
	{
		return 0xFF&(int)block[1];
	}

	public int getChar()
	{
		if(isPushable()) return 0xFF&(int)block[4];
		return 0xFF&(int)block[2];
	}

	public int getColor()
	{
		if(isPushable()) return 0xFF&(int)block[5];
		return 0xFF&(int)block[3];
	}
	public static int getBDSize()
	{
		return 6;
	}

	public byte[] getBlockData()
	{
		return block;
	}
}
