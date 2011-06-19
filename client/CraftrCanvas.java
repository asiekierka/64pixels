package client;
import common.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.*;

import javax.swing.*;

public class CraftrCanvas extends JComponent
{
	public static final int GRID_W = 32;
	public static final int GRID_H = 25;
	public static final int FULLGRID_W = GRID_W+1;
	public static final int FULLGRID_H = GRID_H+1;
	public static final int WIDTH = ((FULLGRID_W-1)*16);
	public static final int HEIGHT = (FULLGRID_H*16)+8;
	// Constants.
	public static Random rand = new Random();
	public int mx, my;
	public CraftrScreen cs;
	public static String chome;
	public double scaleX;
	public double scaleY;
	public Dimension size;
	public int sizeX = WIDTH;
	public int sizeY = HEIGHT;
	
	public BufferedImage img;
	
	public static byte cga[];
	public static int palette[];
	public static IndexColorModel globpal;
	public static IndexColorModel tmppal;
	public static BufferedImage charsetImage[][];
	public static BufferedImage charsetImage2[][];
	public static int pixelchrn[];
	public static int scm = -1;
	public static int scm2 = 0;
	// CONSTRUCTORS
	
	public CraftrCanvas()
	{
		this(0);
	}
	public CraftrCanvas(int hq)
	{
		if (hq != scm)
		{
			scm = hq;
			charsetImage = new BufferedImage[256][16];
			RedrawCharset();
		}
		Dimension d = new Dimension(WIDTH, HEIGHT);
		setSize(d);
		setPreferredSize(d);
		size = d;
		scaleX = 1;
		scaleY = 1;
	}
	
	static
	{
		palette = new int[] {	0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
								0xAA0000, 0xAA00AA, 0xAAAA00, 0xAAAAAA,
								0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
								0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF };
		cga = new byte[2048];
		chome = System.getProperty("user.home") + "/.64pixels";
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
				ain = CraftrCanvas.class.getResourceAsStream("rawcga.bin");
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

	public void scale(int newX, int newY)
	{
		scaleX = (double)newX/WIDTH;
		scaleY = (double)newY/HEIGHT;
		size = new Dimension(newX,newY);
		sizeX = newX;
		sizeY = newY;
		setSize(size);
		setPreferredSize(size);
	}
	
	public short endianswaps(int val)
	{
		int t = ((val>>8)&0xFF) | ((val<<8)&0xFF00);
		return (short)t;
	}
	
	public int endianswap(int val)
	{
		return ((val&0xFF)<<24) | ((val&0xFF00)<<8) | ((val&0xFF00000)>>8) | ((val>>24)&0xFF);
	}
	
	public void screenshot(String filename)
	{
		FileOutputStream fin;
		DataOutputStream din;
		try
		{
			fin=new FileOutputStream(filename);
			din=new DataOutputStream(fin);
			din.writeByte('B');
			din.writeByte('M'); // header
			din.writeInt(endianswap(54+(WIDTH*HEIGHT*3))); // size
			din.writeInt(0); // reserved 1 and 2 (shorts)
			din.writeInt(endianswap(54)); // offset to bitmap data
			// * BMPFILEHEADER * BEGIN
			din.writeInt(endianswap(40)); // size of BMPFILEHEADER
			din.writeInt(endianswap(WIDTH));
			din.writeInt(endianswap(HEIGHT));
			din.writeShort(endianswaps(1)); // planes
			din.writeShort(endianswaps(24)); // bit count
			din.writeInt(0); // compression (none)
			din.writeInt(endianswap((WIDTH*HEIGHT*3))); // size
			din.writeInt(0);
			din.writeInt(0);
			din.writeInt(0); // used colours
			din.writeInt(0); // important colours
			// * BMPFILEHEADER * END
			for(int y=(HEIGHT-1);y>=0;y--)
			{
				for(int x=0;x<WIDTH;x++)
				{
					int t = img.getRGB(x,y);
					din.writeByte((byte)(t&255));
					din.writeByte((byte)((t>>8)&255));
					din.writeByte((byte)((t>>16)&255));
				}
			}
			din.close();
			fin.close();
		}
		catch(Exception e)
		{
			System.out.println("[CANVAS] Exception while screenshotting!");
		}
	}
	public static void RedrawCharset()
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
				switch(scm)
				{
					case 1:
						charsetImage2[c][col] = new ImageScale2x(charsetImage[c][col]).getScaledImage();
						break;
					default:
						charsetImage2[c][col] = new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
						Graphics2D g2c = (Graphics2D)charsetImage2[c][col].getGraphics();
						g2c.drawImage(charsetImage[c][col],new AffineTransformOp(scale2,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),0,0);
						break;
				}
			}
		}
		System.out.println("[CANVAS] Preparing charset: 100% [DONE]");
	}
	// DrawChar
	
	public void DrawChar(int x, int y, byte bChr, byte bCol, Graphics g)
	{
		int aCol = 255&(int)bCol;
		if(palette[(aCol>>4)] > 0)
		{
			g.setColor(new Color(palette[(aCol>>4)]));
			g.fillRect(x,y,16,16);
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage(charsetImage2[(255&(int)bChr)][(aCol&15)],null,x,y);
	}

	public void DrawChar1x(int x, int y, byte bChr, byte bCol, Graphics g)
	{
		int aCol = 255&(int)bCol;
		if(palette[(aCol>>4)] > 0)
		{
			g.setColor(new Color(palette[(aCol>>4)]));
			g.fillRect(x,y,8,8);
		}
		Graphics2D g2 = (Graphics2D)g;
		g2.drawImage(charsetImage[(255&(int)bChr)][(aCol&15)],null,x,y);
	}

	public void paintComponent(Graphics g)
	{
		int tx = sizeX;
		int ty = sizeY;
		if(tx<WIDTH) tx=WIDTH;
		if(ty<HEIGHT) ty=HEIGHT;
		BufferedImage bi = new BufferedImage(tx,ty,BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = bi.createGraphics();
		cs.paint((Graphics)g2,mx,my);
		Graphics2D g2o = (Graphics2D)g;
		switch(scm2)
		{
			case 1:
				g2o.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				break;
			default:
				g2o.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				break;
		}
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
	
	public int Index(int x, int y)
	{
		return x+(y*FULLGRID_W);
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
