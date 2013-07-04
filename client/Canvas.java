package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.*;

import javax.swing.*;

public class Canvas extends JComponent
{
	public int GRID_W = 32;
	public int GRID_H = 25;
	public int FULLGRID_W = GRID_W+1;
	public int FULLGRID_H = GRID_H+1;
	public int WIDTH = ((FULLGRID_W-1)*16);
	public int HEIGHT = (FULLGRID_H*16)+8;
	// Constants.
	public static Random rand = new Random();
	public int mx, my;
	public Screen cs;
	public static String chome;
	public double scaleX;
	public double scaleY;
	public Dimension size;
	public int sizeX = WIDTH;
	public int sizeY = HEIGHT;
	public boolean resizePlayfield = true;

	public BufferedImage img;
	
	public byte cga[];
	public int palette[];
	public IndexColorModel globpal;
	public IndexColorModel tmppal;
	public BufferedImage charsetImage[][];
	public BufferedImage charsetImage2[][];
	public int pixelchrn[];

	private Graphics g;
	// CONSTRUCTORS
	
	public Canvas()
	{
		this(0);
	}
	public Canvas(int hq)
	{
		Dimension d = new Dimension(WIDTH, HEIGHT);
		setSize(d);
		setPreferredSize(d);
		size = d;
		scaleX = 1;
		scaleY = 1;
		palette = new int[] {	0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
								0xAA0000, 0xAA00AA, 0xAAAA00, 0xAAAAAA,
								0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
								0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
		cga = new byte[2048];
		try
		{
			chome = System.getProperty("user.home") + "/.64pixels";
		}
		catch(Exception e)
		{
			System.out.println("Cannot use default chome due to exception stuff");
			chome = "";
		}
		InputStream ain = null;
		FileInputStream in = null;
		File tcf = null;
		try {
			if(chome!="") tcf = new File(chome + "/palette.bin"); 
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
			if(chome!="") tcf = new File(chome + "/charset.bin");
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
				ain = Canvas.class.getResourceAsStream("rawcga.bin");
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
		charsetImage = new BufferedImage[256][16];
		RedrawCharset();
	}

	public void scale(int newX, int newY)
	{
		scaleX = (double)newX/WIDTH;
		scaleY = (double)newY/HEIGHT;
		size = new Dimension(newX,newY);
		sizeX = newX;
		sizeY = newY;
		if(resizePlayfield) remakeVariables();
		scaleX = (double)newX/WIDTH;
		scaleY = (double)newY/HEIGHT;
		setSize(size);
		setPreferredSize(size);
	}

	public void remakeVariables()
	{
		WIDTH = sizeX-(sizeX%16);
		HEIGHT = sizeY-(sizeY%16)+8;
		if(sizeY < HEIGHT) HEIGHT-=16;
		GRID_W = (WIDTH>>4);
		FULLGRID_W = GRID_W+1;
		FULLGRID_H = ((HEIGHT-8)>>4);
		GRID_H = FULLGRID_H-1;
		if(cs!=null) cs.setCanvas(this);
	}

	public void RedrawCharset()
	{
		pixelchrn = new int[256];
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
							pixelchrn[c]++;
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
	// DrawChar
	
	public void DrawChar(int x, int y, byte bChr, byte bCol)
	{
		int aCol = 255&(int)bCol;
		if(palette[(aCol>>4)] > 0) FillRect(palette[(aCol>>4)],x,y,16,16);
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage(charsetImage2[(255&(int)bChr)][(aCol&15)],null,x,y);
	}

	public void DrawChar1x(int x, int y, byte bChr, byte bCol)
	{
		int aCol = 255&(int)bCol;
		if(palette[(aCol>>4)] > 0) FillRect(palette[(aCol>>4)],x,y,8,8);
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage(charsetImage[(255&(int)bChr)][(aCol&15)],null,x,y);
	}

	public void paintComponent(Graphics gz)
	{
		int tx = sizeX;
		int ty = sizeY;
		if(tx<WIDTH) tx=WIDTH;
		if(ty<HEIGHT) ty=HEIGHT;
		BufferedImage bi = new BufferedImage(tx,ty,BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		g = (Graphics)g2;
		cs.paint(mx,my);
		Graphics2D g2o = (Graphics2D)gz;
		g2o.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		img=bi;
		g2o.drawRenderedImage(bi,AffineTransform.getScaleInstance((float)sizeX/WIDTH,(float)sizeY/HEIGHT));
	}
	
	public void draw(int _mx, int _my)
	{
		mx = _mx;
		my = _my;
		repaint();
	}

	// Utilities
	
	public void FillRect(int col, int x, int y, int w, int h)
	{
		g.setColor(new Color(col));
		g.fillRect(x,y,w,h);
	}

	public void DrawRect(int col, int x, int y, int w, int h)
	{
		g.setColor(new Color(col));
		g.drawRect(x,y,w,h);
	}
	public int Index(int x, int y)
	{
		return x+(y*FULLGRID_W);
	}
	
	public void DrawString(int x, int y, String str, int col)
	{
		char[] ca = str.toCharArray();
		int j = 0;
		int k = 0;
		for(int i=0;i<ca.length;i++)
		{
			if((x+(i<<4)-k)>=WIDTH)
			{
				k+=WIDTH;
				j+=16;
			}
			DrawChar(x+(i<<4)-k,y+j,(byte)ca[i],(byte)col);
		}
	}
	
	public void DrawString1x(int x, int y, String str, int col)
	{
		char[] ca = str.toCharArray();
		int j = 0;
		int k = 0;
		for(int i=0;i<ca.length;i++)
		{
			if((x+(i<<3)-k)>=WIDTH)
			{
				k+=WIDTH;
				j+=8;
			}
			DrawChar1x(x+(i<<3)-k,y+j,(byte)ca[i],(byte)col);
		}
	}
}
