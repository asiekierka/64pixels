package client;
import common.*;
import java.util.*;
import java.lang.*;

public class CraftrGameInterface
{
	public CraftrMap map;
	private CraftrPlayer[] players;
	private CraftrSound audio;
	public boolean update;
	public CraftrNet net;
	private CraftrGame game;

	public CraftrGameInterface(CraftrMap m, CraftrPlayer[] p, CraftrSound a, CraftrNet n)
	{
		map = m;
		players = p;
		audio = a;
		net = n;
	}
	
	public void warrantUpdate()
	{
		update = true;
	}

	public boolean checkKick()
	{
		// TODO
		return false;
	}

	public void kick(String reason)
	{
		// TODO
	}

	public void requestUIChange(int req)
	{
		// TODO
	}

	public void sendChatMessage(String msg)
	{
		// TODO
	}

	public CraftrPlayer getPlayer(int id)
	{
		if(id>=0 && id<256 && players!=null) return players[id];
		return null;
	}

	public void setPlayer(int id,CraftrPlayer p)
	{
		if(id>=0 && id<256 && players!=null && p!=null) players[id]=p;
	}
	public void playSound(int tx, int ty, int val)
	{
		if(val>=256)
		{
			playSample(tx,ty,val-256);
			return;
		}		
		int x=players[255].px-tx;
		int y=players[255].py-ty;
		audio.playNote(x,y,val,1.0);
	}
	public void playSample(int tx, int ty, int val)
	{
		int x=players[255].px-tx;
		int y=players[255].py-ty;
		audio.playSampleByNumber(x,y,val,1.0);
	}
}