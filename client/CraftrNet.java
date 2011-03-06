package client;
import common.*;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;

public class CraftrNet implements Runnable, CraftrNetShim
{
	public Socket socket;
	public DataOutputStream out;
	public ByteArrayOutputStream out2;
	//public OutputStream outf;
	private DataInputStream in;
	private byte[] cbuffer;
	public String nick;
	public int loginStage = 0;
	public boolean isLoadingChunk = false;
	public int loadChunkID, lcX, lcY, lcP;
	public int chunkPacketsLeft, chunkSize;
	public CraftrNetSender ns;
	public boolean isOp=false;
	public int frames=0;
	public int pingsWaiting=0;
	public CraftrAuth auth;
	private byte[] msgenc = new byte[32];
	
	public CraftrNet()
	{
		// Here we initalize the socket.
		try
		{ 
			cbuffer = new byte[65536];
		} catch(Exception e)
		{
			System.out.println("Fatal CraftrNet error!");
			System.exit(1);
		}
	}
	public void connect(String host, int port, int nagle)
	{
		try
		{
			socket = new Socket(host,port);
			while(!socket.isConnected())
			{
				Thread.sleep(10);
			}
			socket.setTcpNoDelay(true);
			in = new DataInputStream(socket.getInputStream());
			out2 = new ByteArrayOutputStream(2048);
			out = new DataOutputStream(out2);
			ns = new CraftrNetSender(socket.getOutputStream());
			Thread tns = new Thread(ns);
			tns.start();
		} catch(Exception e)
		{
			System.out.println("Fatal CraftrNet error!");
			System.exit(1);
		}
	}
	public void sendPacket()
	{
		sendPacket(getPacket());
	}
	
	public int abs(int v)
	{
		return v<0?-v:v;
	}
	public void sendPacket(byte[] t)
	{
		try
		{
			synchronized(ns.packets)
			{
				ns.packets.offer(t);
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal CraftrServer sendPacket(byte[]) error!");
			System.exit(1);
		} 
	}
	
	public void sockClose()
	{
		try
		{
			if(socket.isConnected())
			{
				synchronized(out)
				{
					out.writeByte(0x2A);
				}
				sendPacket();
			}
			socket.close();
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal CraftrServer sockClose error!");
		} 
	}
	
	
	public byte[] getPacket()
	{
		byte[] t = out2.toByteArray();
		out2.reset();
		return t;
	}
	public String readString()
	{
		try
		{
			int la = in.readUnsignedByte();
			byte[] t = new byte[la];
			in.read(t,0,la);
			return new String(t);
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal CraftrServer readString error!");
			return "";
		} 
	}
	
	public void writeString(String s)
	{
		try
		{
			byte[] t = s.getBytes();
			synchronized(out)
			{
				out.writeByte(s.length());
				out.write(t,0,s.length());
			}
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal CraftrServer writeString error!");
			try
			{
				synchronized(out){out.writeByte(0x00);}
			}
			catch(Exception ee)
			{
				System.out.println("Fatal CraftrServer writeString error!");
				System.exit(1);
			}
		} 
	}
	
	public void chunkRequest(int x, int y)
	{
		try
		{
			System.out.println("request-net: " + x + " " + y);
			synchronized(out)
			{
				out.writeByte(0x10);
				out.writeInt(x);
				out.writeInt(y);
				sendPacket();
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal craftrNet chunkRequest Error!");
			System.exit(1);
		}
	}
	
	private int min(int a1, int a2)
	{
		if(a1>a2)
		{
			return a2;
		} else return a1;
	}
	
 	public void playerPush(int dx, int dy)
 	{
 		try
 		{
 			synchronized(out)
 			{
 				out.writeByte(0xE0);
 				out.writeByte((byte)dx);
 				out.writeByte((byte)dy);
 				sendPacket();
 			}
 		}
 		catch(Exception e)
		{
 			System.out.println("Fatal craftrNet playerPush Error!");
 			System.exit(1);
 		}
 	}

	public void sendChatMsg(String msg)
	{
		try
		{
			synchronized(out)
			{
				out.writeByte(0x40);
				writeString(msg);
				sendPacket();
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal craftrNet sendChatMsg Error!");
			System.exit(1);
		}
	}
	
	public void changeNickname(String n)
	{
		try
		{
			synchronized(out)
			{
				out.writeByte(0x26);
				writeString(n);
				sendPacket();
			}
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal craftrNet changeNickname error!");
		}
	}
	public void sendBlock(int dx, int dy, byte t, byte ch, byte co)
	{
		try
		{
			synchronized(out)
			{
				out.writeByte(0x30);
				out.writeInt(dx);
				out.writeInt(dy);
				out.writeByte(t);
				out.writeByte(ch);
				out.writeByte(co);
				sendPacket();
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal craftrNet sendBlock Error!");
			System.exit(1);
		}
	}
	public void playerMove(int dx, int dy)
	{
		try
		{
			int i = 0;
			for(i=0;i<4;i++)
			{
				if(CraftrMap.xMovement[i]==dx && CraftrMap.yMovement[i]==dy)
				{
					i+=0x2C;
					break;
				}
			}
			synchronized(out)
			{
				if(i>=0x2C)
				{
					out.writeByte((byte)i);
				}
				else
				{
					out.writeByte(0x23);
					out.writeByte((byte)dx);
					out.writeByte((byte)dy);
				}
				sendPacket();
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal craftrNet playerMove Error!");
			System.exit(1);
		}
	}
	
	CraftrGame gaa;
	
	public void run()
	{
		try
		{
			while(loginStage != 255 && socket.isConnected())
			{
				loop(gaa);
				Thread.sleep(5);
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal CraftrNet thread run() error!");
			System.exit(1);
		}
	}
	
	public void respawnRequest()
	{
		try
		{
			synchronized(out)
			{
				out.writeByte(0x25);
				sendPacket();
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal craftrNet playerMove Error!");
			System.exit(1);
		}
	}
	
	public void sendDecrypt(String pass)
	{
		try
		{
			synchronized(out)
			{
				out.writeByte(0x51);
				auth = new CraftrAuth(pass);
				byte[] dec = auth.decryptClient(msgenc);
				out.write(dec,0,32);
				sendPacket();
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal craftrNet sendDecrypt Error!");
			System.exit(1);
		}
	}
	public void loop(CraftrGame game)
	{
		try
		{
			if(loginStage == 0)
			{
				synchronized(out)
				{
					out.writeByte(0x0F);
					writeString(nick);
					writeString("Dude, you lag too much, you must live in New Zealand");
					out.writeByte(0x00);
					out.writeByte(0x7F); // compatibility purposes, NEVER REMOVE
					out.writeInt(CraftrVersion.getProtocolVersion());
					out.writeByte(game.players[255].pchr);
					out.writeByte(game.players[255].pcol);
					sendPacket();
				}
				loginStage = 1;
			}
			else
			{
				if(game.isKick) { ns.isRunning=false; }
				int len = 1;
				if(!socket.isConnected() || !ns.isRunning)
				{
					if(!game.isKick) game.kickOut("Disconnected!");
					return;
					//System.exit(0);
				}
				while(len>0)
				{
					byte[] buf = new byte[1];
					if(in.available() > 0) len = in.read(buf,0,1);
					else len=0;
					if(len>0)
					{
						switch((int)(buf[0]&0xFF))
						{
							case 0x01:
								if(loginStage>=2) break;
								loginStage=2;
								game.players[255].px=in.readInt();
								game.players[255].py=in.readInt();
								int tx = game.players[255].px>>6;
								int ty = game.players[255].py>>6;
								chunkRequest(tx,ty);
								chunkRequest(tx+1,ty);
								chunkRequest(tx,ty+1);
								chunkRequest(tx+1,ty+1);
								nick = readString();
								isOp=in.readUnsignedShort()==42;
								game.players[255].name = nick;
								System.out.println("Logged in!");
								break;
							case 0x11:
								isLoadingChunk=true;
								lcX = in.readInt();
								lcY = in.readInt();
								lcP = 0;
								System.out.println("getting chunk " + lcX + "," + lcY);
								synchronized(game.map)
								{
									loadChunkID = game.map.findNewChunkID(lcX,lcY);
								}
								if(loadChunkID < 0) isLoadingChunk = false; // haha! take that, silly servers
								chunkPacketsLeft=in.readInt();
								chunkSize=chunkPacketsLeft;
								cbuffer = new byte[chunkPacketsLeft];
								break;
							case 0x12:
								int tp = in.readUnsignedShort();
								if(isLoadingChunk)
								{
									in.readFully(cbuffer,lcP,tp);
									lcP+=tp;
									chunkPacketsLeft-=tp;
								}
								else
								{
									for(int i=0;i<tp;i++) in.readByte();
								}
								break;
							case 0x13:
								if(isLoadingChunk)
								{
									isLoadingChunk=false;
									GZIPInputStream gin = new GZIPInputStream(new ByteArrayInputStream(cbuffer,0,lcP));
									byte[] tmp1 = new byte[65536];
									int ttp = 0;
									int rp = 0;
									while(rp >= 0)
									{
										ttp+=rp;
										rp = gin.read(tmp1,ttp,65536-ttp);
									}
									synchronized(game.map)
									{
										game.map.chunks[loadChunkID].loadByteNet(tmp1);
									}
									game.netChange=true;
								}
								break;
							case 0x20:
								int t1 = in.readUnsignedByte();
								String tmp2 = readString();
								int px = in.readInt();
								int py = in.readInt();
								byte chr = in.readByte();
								byte col = in.readByte();
								game.players[t1] = new CraftrPlayer(px,py,chr,col,tmp2);
								game.players[t1].posChanged = true;
								game.players[t1].ncol = in.readByte();
								break;
							case 0x21:
								int t2 = in.readUnsignedByte();
								int dx1 = in.readByte();
								int dy1 = in.readByte();
								if(game.players[t2] != null)
								{
									game.players[t2].moveDelta(dx1,dy1);
								}
								break;
							case 0x22:
								game.players[in.readUnsignedByte()] = null;
								break;
							case 0x24:
								int ta1 = in.readUnsignedByte();
								int dx2 = in.readInt();
								int dy2 = in.readInt();
								if(game.players[ta1] != null)
								{
									game.players[ta1].move(dx2,dy2);
								}
								game.netChange=true;
								break;
							case 0x26:
								int ta25 = in.readUnsignedByte();
								if(game.players[ta25]!=null) game.players[ta25].name = readString();
								break;
							case 0x27:
								game.netThreadRequest = 1;
								break;
							case 0x2A:
							case 0x2B:
								int bx2c=in.readInt();
								int by2c=in.readInt();
								byte[] d2c;
								synchronized(game.map)
								{
									d2c = game.map.getBlock(bx2c,by2c).getBlockData();
								}
								int t2c = buf[0]&0x01;	
								int t22c = 0x80&(int)d2c[1];
								int t32c = -1;
								if(t22c!=0 && t2c==0) t32c=0;
								if(t22c==0 && t2c!=0) t32c=1;
								d2c[1] = (byte)((d2c[1]&0x7f) | (t2c<<7));
								synchronized(game.map)
								{
									game.map.setBlock(bx2c,by2c,d2c);
								}
								if(t32c>=0)
								{
									switch(d2c[0])
									{
										case 5:
											game.playSample(bx2c,by2c,t32c);
											break;
										case 6:
											game.playSample(bx2c,by2c,t32c+2);
											break;
										default:
											break;
									}
								}  
								break;
							case 0x2C:
							case 0x2D:
							case 0x2E:
							case 0x2F:
								int id2f = in.readUnsignedByte();
								int dir2f = buf[0]&0x03;
								int dx2f = CraftrMap.xMovement[dir2f];
								int dy2f = CraftrMap.yMovement[dir2f];
								if(game.players[id2f] != null)
								{
									game.players[id2f].moveDelta(dx2f,dy2f);
								}
								break;
							case 0x31:
							case 0x33:
								in.readUnsignedByte();
								int bx1 = in.readInt();
								int by1 = in.readInt();
								byte t3 = in.readByte();
								byte ch1 = in.readByte();
								byte co1 = in.readByte();
 								if(t3 == -1)
 								{
 									synchronized(game.map)
 									{
 										game.map.setPushable(bx1,by1,ch1,co1);
 									}
 								} else {
 									synchronized(game.map)
 									{
 										game.map.setBlock(bx1,by1,t3,(byte)0,ch1,co1);
 										if(buf[0]!=0x33) game.map.setPushable(bx1,by1,(byte)0,(byte)0);
 									}
 								}
								game.blockChange=true;
								break;
 							case 0x32:
								int pid = in.readUnsignedByte();
								int lolx = in.readInt();
								int loly = in.readInt();
								byte lolvx = in.readByte();
								byte lolvy = in.readByte();
								byte nchr = in.readByte();
								byte ncol = in.readByte();
								synchronized(game.map)
								{
									game.map.setPushable(lolx,loly,(byte)0,(byte)0);
									game.map.setPushable(lolx+lolvx,loly+lolvy,nchr,ncol);
								}
								if(game.players[pid] != null)
								{
									if(pid==255)
									{
										synchronized(out)
										{
											out.writeByte(0x28);
											out.writeInt(lolx);
											out.writeInt(loly);
											sendPacket();
										}
									}
									game.players[pid].move(lolx,loly);
								}
								game.blockChange=true;
								break;
							case 0x41:
								int ta41 = in.readUnsignedByte();
								String tmp3 = readString();
								game.gs.addChatMsg(tmp3,0);
								game.netChange = true;
								break;
							case 0x50:
								in.read(msgenc,0,32);
								game.netThreadRequest = 1;
								break;
							case 0x60: // sound GET
								int ta60x = in.readByte();
								int ta60y = in.readByte();
								int ta60v = in.readUnsignedByte();
								// best not to screw up meloders --GM
								game.audio.playNote(ta60x,ta60y,ta60v,1.0);
								break;
							case 0x70:
								int t70=in.readByte();
								isOp=t70==42?true:false;
								break;
							case 0xE1: // push me
								int e1x = in.readInt();
								int e1y = in.readInt();
								int e1xs = in.readShort();
								int e1ys = in.readShort();
								int e1dx = in.readByte();
								int e1dy = in.readByte();
								game.map.pushMultiple(e1x,e1y,e1xs,e1ys,e1dx,e1dy);
								break;
							case 0xF0:
								synchronized(out)
								{
									out.writeByte(0xF1);
									sendPacket();
								}
								break;
							case 0xF1:
								pingsWaiting--;
								break;
							case 0xF5:
								String tmp4 = readString();
								game.kickOut(tmp4);
								break;
						}
					}
				}
				frames++;
				if(frames%130==0) // every 2 seconds, less twempowawy measuwe
				{
					synchronized(out)
					{
						out.writeByte(0x28);
						out.writeInt(game.players[255].px);
						out.writeInt(game.players[255].py);
						sendPacket();
					}
				}
				if(frames%625==0) // every 10 seconds
				{
					synchronized(out)
					{
						out.writeByte(0xF0);
						sendPacket();
					}
					pingsWaiting++;
					if(pingsWaiting>10)
					{
						System.out.println("The server probably went down!");
						System.exit(1);
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal craftrNet Loop Error!");
			e.printStackTrace();
			sockClose();
			System.exit(1);
		}
	}
}
