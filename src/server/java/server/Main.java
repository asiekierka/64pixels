package server;
import common.*;

import java.util.*;

public class Main {

	private static Server server;
	public static void main(String[] args)
	{
		server = new Server(args);
		ShutdownHook hook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook( hook );
		server.start();
	}

	private static class ShutdownHook extends Thread {
		public void run() {
			System.out.println("Shutting down...");
			server.end();
		}
	}
}
