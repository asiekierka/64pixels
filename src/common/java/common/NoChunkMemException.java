package common;

public class NoChunkMemException extends Exception
{
	public NoChunkMemException() {}
	public NoChunkMemException(int x, int y)
	{
		super("No memory found for chunk: x=" + x + ", y=" + y + ".");
	}
}
