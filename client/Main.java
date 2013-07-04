package client;
import common.*;

import java.util.*;

public class Main {

	private static Game game;
	public static void main(String[] args)
	{
		System.out.println("loading 64pixels...");
		ShutdownHook hook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook( hook );
		game = new Game();
		game.init();
		game.start(args);
	}
	
    private static class ShutdownHook extends Thread {
      public void run() {
		System.out.println("VM shutdown happening...");
        game.stop();
		game.end();
      }
    }

}