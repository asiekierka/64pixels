package server;
import common.*;
import java.io.*;
import java.util.*;
import java.awt.Point;

//Greasemonkey's DUNGEN generator ported to Java.

public class Dungen
{
	private int w, h;
	private int threshold, maxarea, cover;
	private ArrayList<ArrayList<Boolean>> g = new ArrayList<ArrayList<Boolean>>();
	

	public Dungen(int w, int h)
	{
		if (w < 9 || h < 9)
		{
			throw new DungenException("map too small, must be at least 9x9, should be more");
		}
		
		for (int x = 0; x < w; x++)
		{
			g.add(new ArrayList<Boolean>());
			
			for (int y = 0; y < h; y++)
				g.get(x).add(false);
		}
		
		threshold = (w*h*40) / 100; // aim for 40% roominess
		maxarea = 100;
		cover = 0;
		
		doRect(w / 2-3, h / 2-3, w / 2+3, h / 2+3);
		scrawl();
		trim();
	}
					
	private void trim()
	{
		for (int y = 1; y < h - 1; y++)
			for (int x = 1; x < w - 1; x++)
			{
				if ((!g.get(x).get(y)) && ((g.get(x).get(y-1) && g.get(x).get(y+1)) || (g.get(x-1).get(y) && g.get(x+1).get(y))))
					g.get(x).set(y, true);
			}
	}
	
	private void scrawl()
	{
		Random r = new Random();
		while (cover < threshold)
		{
			int x1 = r.nextInt((w-2))+1;
			int x2 = r.nextInt((w-2))+1;
			
			int y1 = r.nextInt((h-2))+1;
			int y2 = r.nextInt((h-2))+1;
			
			if (x1 > x2)
			{
				int temp = x2;
				x2 = x1;
				x1 = temp;
			}
			
			if (y1 > y2)
			{
				int temp = y2;
				y2 = y1;
				y1 = temp;
			}
			
			w = x2-x1+1;
			h = y2-y1+1;
			
			if ((w <= 1) || (h <= 1) || (w*h > maxarea))
				continue;
		}
	}	
	
	private void doRect(int x1, int y1, int x2, int y2)
	{
		boolean notok;
		
		if (cover != 0)
		{
			notok = true;
			for (int y = y1; y < y2 + 1; y++)
				for (int x = x1; x < x2 + 1; x++)
				{
					if (g.get(x).get(y))
						return;
				}
				
			for (int y = y1; y < y2 + 1; y++)
			{
				if (g.get(x1-1).get(y) || g.get(x2+1).get(y))
				{
					notok = false;
					break;
				}
			}
			
			if (notok)
			{	
				for (int x = x1; x < x2 + 1; x++)
				{
					if (g.get(x).get(y1-1) || g.get(x).get(y2+1)) 
					{
						notok = false;
						break;
					}
				}
			}
			
			if (notok) return;
			
			for (int y = y1; y < y2 + 1; y++)
				for (int x = x1; x < x2 + 1; x++)
				{
					if (!g.get(x).get(y))
					{
						g.get(x).set(y, true);
						cover++;
					}
				}
			
			}
		}
		
		public boolean getAt(int x, int y)
		{
			return g.get(x).get(y);
		}
		
		public static void main(String[] argv)
		{
			Dungen d = new Dungen(64, 64);
			for (int y = 0; y < 64; y++)
			{
				System.out.println("");
				for (int x = 0; x < 64; x++)
				{
					if (d.getAt(x, y))
						System.out.print("#");
					else
						System.out.print(" ");
				}
			}
		}
	}


class DungenException extends RuntimeException
{
	public DungenException(String msg)
	{
		super(msg);
	}
}
