import java.lang.*;
import java.util.*;

public class CraftrChatMsg
{
	public char[] message;
	public int msglen=0;
	public Date time;
	public Date expirytime;
	public int source=255;
	
	public CraftrChatMsg(String _message)
	{
		source = 255; // system/self
		time = new Date();
		expirytime = new Date(time.getTime() + (10500L)); // 1000L = ONE_SECOND
		message = _message.toCharArray();
		msglen = message.length;
	}
	
	public CraftrChatMsg(String _message, int _sourceofmsg)
	{
		source = _sourceofmsg;
		time = new Date();
		expirytime = new Date(time.getTime() + (10500L)); // 1000L = ONE_SECOND
		message = _message.toCharArray();
		msglen = message.length;
	}
}
