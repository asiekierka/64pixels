package common;

public class Player
{
	public int x, y, health;
	public byte chr, col;
	public boolean posChanged;
	public String name;
	
	public Player(int _px, int _py, byte _pchr, byte _pcol, String _pn)
	{
		x = _px;
		y = _py;
		chr = _pchr;
		col = _pcol;
		name = _pn;
		posChanged = true;
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
		x = _px;
		y = _py;
		posChanged = true;
	}
	public void moveDelta(int _px, int _py)
	{
		x += _px;
		y += _py;
		posChanged = true;
	}
}
