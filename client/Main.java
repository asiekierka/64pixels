package client;
import common.*;

import java.util.*;

public class Main {

	private static CraftrGame game;
	public static void main(String[] args)
	{
		System.out.println("loading 64pixels...");
		CraftrShutdownHook hook = new CraftrShutdownHook();
		Runtime.getRuntime().addShutdownHook( hook );
		game = new CraftrGame();
		game.init();
		game.start(args);
	}
	
    private static class CraftrShutdownHook extends Thread {
      public void run() {
		System.out.println("VM shutdown happening...");
        game.stop();
		game.end();
      }
    }

}