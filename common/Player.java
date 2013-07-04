package common;

public class Player
{
	public int px, py, health;
	public byte pchr, pcol, ncol;
	public boolean posChanged;
	public String name;
	
	public Player(int _px, int _py, byte _pchr, byte _pcol, String _pn)
	{
		px = _px;
		py = _py;
		pchr = _pchr;
		pcol = _pcol;
		name = _pn;
		posChanged = true;
		ncol = 0;
	}
	public Player(int _px, int _py)
	{
		this(_px,_py,(byte)2,(byte)31,"You");
	}
	public Player(int _px, int _py, byte _pchr, byte _pcol)
	{
		this(_px,_py,_pchr,_pcol,"You");
	}
	
	public void move(int _px, int _py)
	{
		px = _px;
		py = _py;
		posChanged = true;
	}
	public void moveDelta(int _px, int _py)
	{
		px += _px;
		py += _py;
		posChanged = true;
	}
}
