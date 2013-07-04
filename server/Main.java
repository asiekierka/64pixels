package server;
import common.*;

import java.util.*;

public class Main {

	private static Server serv;
	public static void main(String[] args)
	{
		serv = new Server(args);
		ShutdownHook hook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook( hook );
		serv.start();
	}

    private static class ShutdownHook extends Thread {
      public void run() {
		System.out.println("VM shutdown happening...");
		serv.end();
      }
    }
	
}