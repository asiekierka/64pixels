package server;

import common.*;
import java.util.*;

public class CraftrGenerationThread implements Runnable
{
	public CraftrServer server;
	public CraftrMapGenerator generator;
	public CraftrMap map;
	public int owner, width, height;

	public CraftrGenerationThread(CraftrServer se, int id, CraftrMapGenerator gen, CraftrMap m, int w, int h) {
		server=se;
		generator=gen;
		owner=id;
		map=m;
		width=w;
		height=h;
	}

	public void run() {
                boolean status = false;
		try {
			status = generator.generate(map,width,height);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		if(server.clients[owner]!=null && server.clients[owner].dc!=1) {
			if(!status) server.clients[owner].sendChatMsgSelf("&cERROR: &fGeneration unsuccessful!");
			else server.clients[owner].sendChatMsgSelf("Map made successfully!");
		}
	}
}
