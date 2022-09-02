package common;

/**
 * Contains constants for packets sent by the client.
 */
public class NetConstClient {
	public static final int LOGIN = 0x0F;
	public static final int CHUNK_REQUEST = 0x10;
	public static final int MOVE_DELTA = 0x23;
	public static final int RESPAWN = 0x25;
	public static final int MOVE_ABSOLUTE = 0x28;
	public static final int DISCONNECT = 0x2A;
	public static final int MOVE_COMPRESSED = 0x2C;
	public static final int PUT_BLOCK = 0x30;
	public static final int CHAT = 0x40;
	public static final int DECRYPTED_DATA = 0x51;
	public static final int SHOOT = 0x70;
	public static final int PUSH = 0xE0;
	public static final int PING = 0xF0;
	public static final int PONG = 0xF1;
}
