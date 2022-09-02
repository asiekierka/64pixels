package client;
import java.util.*;
public class GameThread implements Runnable
{
	public Game game;
	public long fps = 0;
	public boolean isRunning = true;

	public GameThread(Game g)
	{
		game=g;
	}
	
	public void run()
	{
		int overhead=0;
		while(isRunning)
		{
			try
			{
				if(overhead<33)
				{
					Thread.sleep(33-overhead);
					overhead=0;
				} else {
					overhead-=33;
				}
			}
			catch(Exception e){}
			Date told = new Date();
			game.runOnce();
			overhead+=(new Date().getTime())-told.getTime();
			fps++;
		}
	}
}
