package common;

import java.io.*;

public abstract class CraftrServerShim
{
	// TODO: get this class to the point where this can simply be an interface
	// this "public" stuff is just stupid --GM
	public DataOutputStream out;
	
	public abstract byte[] getPacket();
	public abstract void playSound(int x, int y, int id);
	public abstract void sendAll(byte[] arr);
	public abstract void sendAll(byte[] arr, int len);
	public abstract void sendAllOnMap(byte[] arr, String map_name);
	public abstract void sendAllOnMap(byte[] arr, int len, String map_name);
	public abstract void kill(int pid);
}
