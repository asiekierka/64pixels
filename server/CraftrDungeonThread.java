package server;
import common.*;
import java.util.*;
public class CraftrDungeonThread implements Runnable
{
	public CraftrServer s;
	public int myid;
	public CraftrDungeonGenerator dun;
	public CraftrMap m;
	public int width, height;
	public CraftrDungeonThread(CraftrServer se, int id, CraftrDungeonGenerator dungeon, CraftrMap map, int w, int h)
	{
		s=se;
		dun=dungeon;
		myid=id;
		m=map;
		width=w;
		height=h;
	}
	
	public void run()
	{
		int status = -9001;
		try
		{
			status=dun.generate(m,width,height);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		if(s.clients[myid]!=null && s.clients[myid].dc!=1)
		{
			if(status!=0) s.clients[myid].sendChatMsgSelf("&cERROR: &f"+CraftrDungeonGenerator.getError(status));
			else s.clients[myid].sendChatMsgSelf("Dungeon made successfully!");
		}
	}
}
