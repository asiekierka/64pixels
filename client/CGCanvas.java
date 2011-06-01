package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.*;

import javax.swing.*;

public class CGCanvas extends JComponent
{
	public int GRID_W = 32;
	public int GRID_H = 26;
	public int WIDTH = GRID_W*16;
	public int HEIGHT = GRID_H*16;
	// Constants.
	private int mx, my;
	private static String chome;
	private static byte cga[];
	public static int palette[];
	private Graphics my_g;
	private static BufferedImage charsetImage[][];
	private static BufferedImage charsetImage2[][];
	// CONSTRUCTORS

	public CGCanvas()
	{
		Dimension d = new Dimension(WIDTH, HEIGHT);
		setSize(d);
		setPreferredSize(d);
	}
	
	public void scale(int x, int y)
	{
		WIDTH=x;
		HEIGHT=y;
		GRID_W=x>>4;
		GRID_H=y>>4;
		Dimension d = new Dimension(x,y);
		setSize(d);
		setPreferredSize(d);
	}
	static
	{
		palette = new int[] {	0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
								0xAA0000, 0xAA00AA, 0xAAAA00, 0xAAAAAA,
								0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
								0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
		cga = new byte[2048];
		chome = CraftrVersion.getHomeDir();
		InputStream ain = null;
		FileInputStream in = null;
		try {
			File tcf = new File(chome + "/palette.bin"); 
			if(tcf != null && tcf.exists())
			{
				System.out.println("[CANVAS] Custom palette found!");
				in = new FileInputStream(chome + "/palette.bin");
				for(int i=0;i<16;i++)
				{
					byte[] dat = new byte[3];
					in.read(dat,0,3);
					palette[i] = ((255&(byte)dat[0])<<16)|((255&(byte)dat[1])<<8)|(255&(byte)dat[2]);
				}
				in.close();
			}
			else System.out.println("[CANVAS] Using default palette!");
			tcf = new File(chome + "/charset.bin");
			if(tcf != null && tcf.exists())
			{
				System.out.println("[CANVAS] Custom charset found!");
				in = new FileInputStream(chome + "/charset.bin");
				for(int i=0; i<256; i++)
				{
					in.read(cga,i<<3,8);
				}
				in.close();
			}
			else
			{
				System.out.println("[CANVAS] Using default charset!");
				ain = CGCanvas.class.getResourceAsStream("rawcga.bin");
				for(int i=0; i<256; i++)
				{
					ain.read(cga,i<<3,8);
				}
				ain.close();
			}
		}
		catch(Exception e) {
			System.out.println("[CANVAS] Couldn't load charset/palette! " + e.getMessage());
			System.exit(1);
		}
		//charsetImage = new BufferedImage[256][16];
		//RedrawCharset();
	}

	public static void RedrawCharset()
	{
		charsetImage = new BufferedImage[256][16];
		charsetImage2 = new BufferedImage[256][16];
		AffineTransform scale2 = new AffineTransform();
		scale2.scale(2, 2);
		int cgat = 0;
		int[] palcol = new int[16];
		for(int pt=0;pt<16;pt++)
				palcol[pt] = (0xFF000000 | palette[pt]);
		for(int c=0;c<256;c++)
		{
			int temp1 = (c<<3);
			if((c&31) == 0) System.out.println("[CANVAS] Preparing charset: " + ((c*100)>>8) + "%");
			for(int col=0;col<16;col++)
			{
				charsetImage[c][col] = new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
				for(int t3=0;t3<8;t3++)
				{
					cgat = 255&(int)cga[temp1+t3];
					for(int t2=7;t2>=0;t2--)
					{
						if ((cgat&1) == 1 )
						{
							charsetImage[c][col].setRGB(t2,t3, palcol[col]);
						} else {
							charsetImage[c][col].setRGB(t2,t3, 0);
						}
						cgat>>=1;
					}
				}
				charsetImage2[c][col] = new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2c = (Graphics2D)charsetImage2[c][col].getGraphics();
				g2c.drawImage(charsetImage[c][col],new AffineTransformOp(scale2,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),0,0);
			}
		}
		System.out.println("[CANVAS] Preparing charset: 100% [DONE]");
	}
	
	public void FillRect(int x, int y, int w, int h, int c)
	{
		my_g.setColor(new Color(c));
		my_g.fillRect(x,y,w,h);
	}

	public void DrawChar(int x, int y, byte bChr, byte bCol, Graphics g)
	{
		int aCol = 255&(int)bCol;
		if(palette[(aCol>>4)] > 0)
		{
			my_g=g;
			FillRect(x,y,16,16,palette[(aCol>>4)]);
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage(charsetImage2[(255&(int)bChr)][(aCol&15)],null,x,y);
	}

	public void DrawChar1x(int x, int y, byte bChr, byte bCol, Graphics g)
	{
		int aCol = 255&(int)bCol;
		if(palette[(aCol>>4)] > 0)
		{
			my_g=g;
			FillRect(x,y,8,8,palette[(aCol>>4)]);
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage(charsetImage[(255&(int)bChr)][(aCol&15)],null,x,y);
	}

	public void paintComponent(Graphics g)
	{

	}
	
	public void draw(int _mx, int _my)
	{
		mx = _mx;
		my = _my;
		repaint();
	}

	public void DrawString(int x, int y, String str, int col, Graphics g)
	{
		char[] ca = str.toCharArray();
		int j = 0;
		int k = 0;
		for(int i=0;i<ca.length;i++)
		{
			if((x+(i<<4)-k)>=WIDTH)
			{
				k-=WIDTH;
				j+=16;
			}
			DrawChar(x+(i<<4)-k,y+j,(byte)ca[i],(byte)col,g);
		}
	}
	
	public void DrawString1x(int x, int y, String str, int col, Graphics g)
	{
		char[] ca = str.toCharArray();
		int j = 0;
		int k = 0;
		for(int i=0;i<ca.length;i++)
		{
			if((x+(i<<3)-k)>=WIDTH)
			{
				k-=WIDTH;
				j+=8;
			}
			DrawChar1x(x+(i<<3)-k,y+j,(byte)ca[i],(byte)col,g);
		}
	}
}
