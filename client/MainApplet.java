package client;
import common.*;

import java.util.*;

import java.awt.*;
import javax.swing.*;

public class MainApplet extends JApplet {

	public static CraftrGame game;
	
	public void init()
	{
		game = new CraftrGame(this);
		//addMouseListener(game);
		//addMouseMotionListener(game);
		//addKeyListener(game);
		game.init();
		requestFocusInWindow();
	}
	
	public void start()
	{
		requestFocusInWindow();
		(new Thread(new AppletGameRun())).start();
	}
	
	public void stop()
	{
		//game.stop();
	}

	public void destroy()
	{
		game.gameOn = false;
		game.stop();
		game.end();
	}
	
	private static class AppletGameRun implements Runnable {

    public void run() {
        game.start(new String[0]);
    }

	}
}
