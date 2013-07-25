package plug;

public class PlugTest implements Plugin
{
	public void onEnable()
	{
		System.out.println("It was enabled.");
	}

	public void onDisable()
	{
		System.out.println("It was disabled.");
	}
}