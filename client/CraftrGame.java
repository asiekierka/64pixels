package client;
import common.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import java.lang.*;

public class CraftrGame extends JComponent
implements MouseListener, MouseMotionListener, KeyListener
{
	public JFrame window;
	public boolean gameOn;
	public CraftrMap map;
	public CraftrPlayer[] players;
	public CraftrSound audio;
	public Date told = new Date();
	public Date tnew;
	public int fps = 0;
	public int ix = 0;
	public int iy = 0;
	public long frame = 0;
	public long fold = 0;
	public CraftrGame()
	{
		window = new JFrame("64pixels");
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		window.addMouseListener(this);
		window.addMouseMotionListener(this);
		window.addKeyListener(this);
		gameOn = true;
	}
	public void mouseEntered(MouseEvent ev) {}
	public void mouseExited(MouseEvent ev) {}
	public void mouseClicked(MouseEvent ev) {} // this one sucks
	public void mousePressed(MouseEvent ev) {}
	public void mouseReleased(MouseEvent ev) {}

	public void mouseMoved(MouseEvent ev) {}
	public void mouseDragged(MouseEvent ev) {} // this can be quite handy

	public void keyTyped(KeyEvent ev) {} // this one sucks even more
	public void keyPressed(KeyEvent ev) {
	}
	public void keyReleased(KeyEvent ev) {}
	
	public void init()
	{
	}

	public void start(String[] args)
	{
		// i know it's a hack but still
		//window.add(canvas);
		window.pack(); // makes everything a nice size
		window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		window.setVisible(true);
		tnew = new Date(told.getTime() + 1000L);
		while(gameOn)
		{
			try
			{
			Thread.sleep(33);
			}
			catch (Exception e) { }
			told = new Date();
			if(told.compareTo(tnew) >= 0)
			{
				fps = (int)(frame-fold);
				tnew = new Date(told.getTime() + 1000L);
				fold = frame;
				System.out.println(fps + " fps");
			}
			frame++;
		}
	}
	public void stop()
	{

	}
	public void end()
	{

	}
}