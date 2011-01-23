import java.util.*;

public class Main {

	private static CraftrServer serv;
	public static void main(String[] args)
	{
		serv = new CraftrServer(args);
		CraftrShutdownHook hook = new CraftrShutdownHook();
		Runtime.getRuntime().addShutdownHook( hook );
		serv.start();
	}

    private static class CraftrShutdownHook extends Thread {
      public void run() {
		System.out.println("VM shutdown happening...");
		serv.end();
      }
    }
	
}