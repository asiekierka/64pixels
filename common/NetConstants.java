package common;

public class NetConstants
{
	//Sends
	public static final int QUIT = 0x2A;
	public static final int ASK_CHUNK = 0x10;
	public static final int SHOOT = 0x70;
	public static final int PLAYER_PUSH = 0xE0; //??????
	public static final int CHAT = 0x40;
	public static final int SEND_BLOCK = 0x30;
	public static final int PLAYER_MOVE = 0x23;
	public static final int RESPAWN_REQUEST = 0x25;
	public static final int SEND_DECRYPT = 0x51;
	public static final int LOGIN_ATTEMPT = 0x0F;
	public static final String LOGIN_MAGIC_STRING = "eeeeh";
	public static final int LOGIN_COMPAT = 0x7F;
	
	//Receives
	public static final int LOGIN_ACCEPTED = 0x01;
	public static final int CHUNK_TRANSMISSION_START = 0x11;
	public static final int CHUNK_TRANSMISSION = 0x12;
	public static final int CHUNK_TRANSMISSION_END = 0x13;
	public static final int NEW_PLAYER = 0x20;
	public static final int MOVE_PLAYER = 0x21;
	public static final int DEL_PLAYER = 0x22;
	public static final int MOVE_PLAYER2 = 0x24; //Whaaa...??
	public static final int SET_PLAYER_NAME = 0x26;
	public static final int CHECK_PLAYER_OP = 0x28;
	public static final int MOVE_PLAYER3 = 0x2F; //This is getting dumb.
	public static final int DEL_BLOCK = 0x34;
	public static final int RECEIVE_CHAT = 0x41;
	public static final int MELODER_PLAY = 0x60;
	public static final int NEW_BULLET = 0x70;
	public static final int RELOAD_MAP = 0x80;
	public static final int RAYCAST_ON = 0x81;
	public static final int RAYCAST_OFF = 0x82;
	public static final int DIE = 0x90;
	public static final int SET_HEALTH = 0x91;
	public static final int TOGGLE_HEALTH = 0x92;
	public static final int PUSH_ME = 0xE2;
	public static final int PONG = 0xF0;
	public static final int PING = 0xF1;
	public static final int KICK = 0xF5;
}
