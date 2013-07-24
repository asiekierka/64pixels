package common;

/**
 * Contains constants for packets sent by the server.
 */
public class NetConstServer {
	public static final int LOGIN = 0x01;
	public static final int DATA_START = 0x11;
	public static final int DATA = 0x12;
	public static final int DATA_END = 0x13;
	public static final int SPAWN = 0x20;
	public static final int MOVE_DELTA = 0x21;
	public static final int DESPAWN = 0x22;
	public static final int MOVE_ABSOLUTE = 0x24;
	public static final int PLAYER_NICKNAME = 0x26;
	public static final int OP = 0x28;
	public static final int STEP = 0x2A;
	public static final int MOVE_COMPRESSED = 0x2C; // TODO: NOT ACTUALLY SENT BY SERVER
	public static final int PLACE_BLOCK_PLAYER = 0x31;
	public static final int PLACE_PUSHABLE_PLAYER = 0x32;
	public static final int PLACE_BLOCK_MAP = 0x33;
	public static final int CLEAR_BLOCK_MAP = 0x34;
	public static final int CHAT = 0x41;
	public static final int ENCRYPTED_DATA = 0x50;
	public static final int PLAY_SOUND = 0x60;
	public static final int BULLET = 0x70;
	public static final int CHANGE_MAP = 0x80;
	public static final int RAYCAST_OFF = 0x81;
	public static final int RAYCAST_ON = 0x82;
	public static final int HEALTH = 0x91;
	public static final int PVP = 0x92;
	public static final int PUSH = 0xE1;
	public static final int PULL = 0xE2;
	public static final int PING = 0xF0;
	public static final int PONG = 0xF1;
	public static final int KICK = 0xF5;

	public static final int DATA_TYPE_CHUNK = 0x01;
}
