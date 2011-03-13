package server;
import common.*;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;

public class CraftrClient implements Runnable
{
	public Socket socket;
	public DataOutputStream out;
	public ByteArrayOutputStream out2;
	private DataInputStream in;
	public int x, y, loginStage;
	public byte chr, col;
	public String nick;
	public CraftrMap map;
	public int id, version, dc;
	public boolean isRequestingChunk;
	public int rcX, rcY;
	public int rcP;
	public GZIPOutputStream rcin;
	public CraftrServer serv;
	public CraftrNetSender ns;
	public int ncol=0;
	public int health;
	public boolean op = false;
	public long frames = 0;
	public int pingsWaiting = 0;
	private CraftrAuth auth;
	public boolean passWait=false;
	public CraftrCopier cc;
	public boolean isCopying = false;
	public boolean isPasting = false;
	public int copyStage = 0;
	public int cx,cy;

	public CraftrClient(Socket s, CraftrMap m, int iz, CraftrServer se)
	{
		try
		{
			socket = s;
			serv = se;
			socket.setTcpNoDelay(true);
			map = m;
			id = iz;
			dc = 0;
			nick = "anonymous"; // ah yeah, default values
			chr = 2;
			col = 31;
			x = 0;
			y = 0;
			loginStage = 0;
			in = new DataInputStream(socket.getInputStream());
			out2 = new ByteArrayOutputStream(2048);
			out = new DataOutputStream(out2);
			ns = new CraftrNetSender(socket.getOutputStream());
			Thread tns = new Thread(ns);
			tns.start();
			cc=new CraftrCopier();
			System.out.println("New user with ID " + id + " connected!");
		}
		catch(Exception e)
		{
			System.out.println("Fatal CraftrServer client thread init error!");
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

	public void playSound(int tx,int ty, int val)
	{
		int ax=x-tx;
		int ay=y-ty;
		if(ax>=-128 && ax<128 && ay>=-128 && ay<128)
		{
			synchronized(out)
			{
				try
				{
					out.writeByte(0x60);
					out.writeByte((byte)ax);
					out.writeByte((byte)ay);
					out.writeByte((byte)val);
				}
				catch(Exception e){};
				sendPacket();
			}
		}
	}
	
	public void changeNickname(String newn)
	{
		try
		{
			nick=newn;
			synchronized(out)
			{
				out.writeByte(0x26);
				out.writeByte(255);
				writeString(newn);
				sendPacket();
				out.writeByte(0x26);
				out.writeByte((byte)id);
				writeString(newn);
				serv.sendOthers(id,getPacket());
			}
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal changeNickname error!");
		}
	}
	
	public void sendChatMsgSelf(String m)
	{
		try
		{
			String m2 = m;
			synchronized(out)
			{
				while(m2.length()>40)
				{
					out.writeByte(0x41);
					out.writeByte((byte)id);
					writeString(m2.substring(0,40));
					m2=m2.substring(40);
				}
				out.writeByte(0x41);
				out.writeByte((byte)id);
				writeString(m2);
				sendPacket();
			}
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal sendChatMsgSelf error!");
		}
	}
	
	public void sendChatMsgAll(String m)
	{
		try
		{
			synchronized(out)
			{
				String m2=m;
				while(m2.length()>40)
				{
					out.writeByte(0x41);
					out.writeByte((byte)id);
					writeString(m2.substring(0,40));
					m2=m2.substring(40);
				}
				out.writeByte(0x41);
				out.writeByte((byte)id);
				writeString(m2);
				serv.sendAll(getPacket());
			}
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal sendChatMsgAll error!");
		}
	}
	
	public void kick()
	{
		kick("Kicked!");
	}
	
	public void kick(String msg)
	{
		try
		{
			synchronized(out)
			{
				out.writeByte(0xF5);
				writeString(msg);
				sendPacket();
			}
			disconnect();
			System.out.println("User " + id + " was kicked! (" + msg + ")");
		}
		catch(Exception e)
		{
			System.out.println("Fatal CraftrKick error!");
			disconnect();
		}
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
	
	public void teleport(int tx, int ty)
	{
		try
		{
			x=tx;
			y=ty;
			synchronized(out)
			{
				out.writeByte(0x24);
				out.writeByte(255);
				out.writeInt(x);
				out.writeInt(y);
				sendPacket();
				out.writeByte(0x24);
				out.writeByte((byte)id);
				out.writeInt(x);
				out.writeInt(y);
				serv.sendOthers(id,getPacket());
			}
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal teleport error!");
			e.printStackTrace();
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
				disconnect();
			}
		} 
	}
	
	public void sendPacket()
	{
		sendPacket(getPacket());
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
	
	public byte[] getPacket()
	{
		byte[] t = out2.toByteArray();
		out2.reset();
		return t;
	}

	public void sendOpPacket(int val)
	{
		try
		{
			synchronized(out)
			{
				out.writeByte(0x28);
				out.writeShort((short)val*42);
				sendPacket();
			}
		}
		catch(Exception e){}
	}
	public int abs(int a)
	{
		if(a<0) return -a; else return a;
	}
	public void disconnect()
	{
		System.out.println("User " + id + " has disconnected!");
		dc = 1;
		try
		{
			serv.map.setPlayer(x,y,0);
			synchronized(out)
			{
				out.writeByte(0x22);
				out.writeByte((byte)id);
				serv.sendOthers(id,getPacket());
			}
			sendChatMsgAll(nick + " has left.");
		}
		catch(Exception e)
		{
			System.out.println("Non-fatal CraftrServer client thread DC packet send error!");
			e.printStackTrace();
		}
	}

	// This game is a TEMPORARY MEASURE!!! -asie
	public void run()
	{
		// Inner loop.
		try
		{
			while(dc == 0)
			{
				int len = 1;
				int packets = 0;
				while(len>0 && dc==0)
				{
					byte[] buf = new byte[1];
					if(!socket.isConnected() || !socket.isBound() || !ns.isRunning) disconnect();
					else if(in.available() > 0) len = in.read(buf,0,1);
					else len = 0;
					if(len > 0 && packets < 32)
					{
						packets++;
						switch((int)(buf[0]&0xFF))
						{
							case 0x0F:
									if(loginStage>=1) break;
									loginStage = 1;
									nick = readString();
									readString();
									in.readByte();
									in.readByte();
									version = in.readInt();
									chr = in.readByte();
									col = in.readByte();
									x=serv.spawnX;
									y=serv.spawnY;
									if(serv.anonMode) nick = "User"+id;
									System.out.println("User " + id + " (IP " + socket.getInetAddress().getHostAddress() + ") connected!");
									if(nick.length()>20)
									{
										kick("Invalid nickname!");
									}
									else if(version!=CraftrVersion.getProtocolVersion())
									{
										kick("Invalid protocol! Needs 0.1 or higher.");
									}
									else if(serv.isBanned(socket.getInetAddress().getHostAddress()))
									{
										kick("You're banned!");
									}
									else
									{
										if(serv.isOp(socket.getInetAddress().getHostAddress()))
										{
											op=true;
											System.out.println("User " + id + " is an Op!");
										}
										synchronized(out)
										{
											out.writeByte(0x01);
											out.writeInt(x);
											out.writeInt(y);
											writeString(nick);
											out.writeShort(op?42:0);

											sendPacket();
										}
										if(serv.passOn)
										{
											auth = new CraftrAuth(serv.pass);
											byte[] ae = auth.encrypt();
											synchronized(out)
											{
												out.writeByte(0x50);
												out.write(ae,0,32);
												sendPacket();
											}
											passWait=true;
										}
										sendChatMsgAll(nick + " has joined.");
										health = 7;
										serv.map.physics.players[id] = new CraftrPlayer(x,y,chr,col,nick);
										serv.map.physics.players[id].health = health;
										serv.map.setPlayer(x,y,1);
										synchronized(out)
										{
											out.writeByte(0x20);
											out.writeByte(id);
											writeString(nick);
											out.writeInt(x);
											out.writeInt(y);
											out.writeByte(chr);
											out.writeByte(col);
											out.writeByte(ncol); 
											serv.sendOthers(id,getPacket());
										}
										for(int pli=0;pli<255;pli++)
										{
											if(pli != id && serv.clients[pli] != null && serv.clients[pli].dc == 0)
											{
												synchronized(out)
												{
													out.writeByte(0x20);
													out.writeByte(serv.clients[pli].id);
													writeString(serv.clients[pli].nick);
													out.writeInt(serv.clients[pli].x);
													out.writeInt(serv.clients[pli].y);
													out.writeByte(serv.clients[pli].chr);
													out.writeByte(serv.clients[pli].col);
													out.writeByte(serv.clients[pli].ncol);
													sendPacket();
												}
											}	
										}
									}
								break;
							case 0x10:
								rcX = in.readInt();
								rcY = in.readInt();
								System.out.println("[ID " + id + "] sending chunk " + rcX + "," + rcY);
								byte[] t = new byte[16384];
								int z10;
								synchronized(map.chunks)
								{
									t = map.grabChunk(rcX,rcY).saveByteNet();
									z10 = map.findChunkID(rcX,rcY);
								}
								ByteArrayOutputStream bain = new ByteArrayOutputStream();
								rcin = new GZIPOutputStream(bain);
								rcin.write(t,0,t.length);
								rcin.finish();
								rcin.close();
								byte[] t2 = bain.toByteArray();
								int pl = t2.length;
								int pp = t2.length;
								synchronized(out)
								{
									out.writeByte(0x11);
									out.writeInt(rcX);
									out.writeInt(rcY);
									out.writeInt(t2.length);
									sendPacket();
								}
								while(pl>0)
								{
									int pls = 640;
									if(pl<pls) pls=pl;
									synchronized(out)
									{
										out.writeByte(0x12);
										out.writeShort(pls);
										out.write(t2,pp-pl,pls);
										sendPacket();
									}
									pl -= pls;
								}
								synchronized(map.chunks) { if(map.chunks[z10].xpos == rcX && map.chunks[z10].ypos == rcY) map.chunks[z10].isUsed=false; }
								synchronized(out)
								{
									out.writeByte(0x13);
									sendPacket();
								}
								break;
							case 0x23:
								byte[] ta = new byte[4];
								ta[0] = 0x21;
								ta[1] = (byte)id;
								ta[2] = in.readByte();
								ta[3] = in.readByte();
								if(passWait) break;
								if(abs(ta[2])>4 || abs(ta[3])>4)
								{
									kick("Invalid movement!");
								}
								serv.sendOthers(id,ta,4);
								serv.map.setPlayer(x,y,0);
								x+=ta[2];
								y+=ta[3];
								serv.map.setPlayer(x,y,1);
								break;
							case 0x25:
								serv.map.setPlayer(x,y,0);
								serv.map.setPlayer(serv.spawnX,serv.spawnY,1);
								teleport(serv.spawnX,serv.spawnY);
								break;
							case 0x26:
								break;
							case 0x28:
								int x29 = in.readInt();
								int y29 = in.readInt();
								if(passWait) break;
								if(abs(x29-x)<=16 || abs(y29-y)<=16)
								{
									x=x29;
									y=y29;
									synchronized(out)
									{
										out.writeByte(0x24);
										out.writeByte(id);
										out.writeInt(x29);
										out.writeInt(y29);
										serv.sendOthers(id,getPacket());
									}
								}
								break;
							case 0x2A:
								disconnect();
								break;
							case 0x2C:
							case 0x2D:
							case 0x2E:
							case 0x2F:
								int dir2f = buf[0]&0x03;
								int dx2f = map.xMovement[dir2f];
								int dy2f = map.yMovement[dir2f];
								byte[] ta2f = new byte[4];
								ta2f[0] = buf[0];
								ta2f[1] = (byte)id;
								if(passWait) break;
								serv.sendOthers(id,ta2f,2);
								serv.map.setPlayer(x,y,0);
								x+=dx2f;
								y+=dy2f;
								serv.map.setPlayer(x,y,1);
								break;
							case 0x30:
								int ax = in.readInt();
								int ay = in.readInt();
								byte at = in.readByte();
								byte ach = in.readByte();
								byte aco = in.readByte();
								if(passWait) break;
								byte[] zc = map.getBlock(ax,ay).getBlockData();
								if(op && (isCopying || isPasting))
								{
									out.writeByte(0x31);
									out.writeByte((byte)id);
									out.writeInt(ax);
									out.writeInt(ay);
									out.writeByte(zc[0]);
									out.writeByte(zc[2]);
									out.writeByte(zc[3]);
									sendPacket();
									if(isCopying)
									{
										if(copyStage == 0)
										{
											sendChatMsgSelf("Click on the other corner.");
											cx=ax;
											cy=ay;
											copyStage++;
										}
										else if(copyStage == 1)
										{
											isCopying=false;
											int tcx = ax;
											int tcy = ay;
											int ts = 0;
											if(tcx<cx) { ts=tcx;tcx=cx;cx=ts; }
											if(tcy<cy) { ts=tcy;tcy=cy;cy=ts; }
											int tcxs = abs(tcx-cx)+1;
											int tcys = abs(tcy-cy)+1;
											if(tcxs>0 && tcxs<=128 && tcys>0 && tcys<=128)
											{
												cc.copy(map,cx,cy,tcxs,tcys);
												sendChatMsgSelf("Copied.");
											} else sendChatMsgSelf("Copy error: Invalid size (" + tcxs + ", " + tcys + ")");
										}
									}
									else if(isPasting)
									{
										cc.paste(map,ax,ay);
										sendChatMsgSelf("Pasted.");
										System.out.println("[ID " + id + "] Pasted at " + ax + "," + ay + "!");
										isPasting=false;
									}
								}
								else
								{
									if((int)(at&0xFF) > CraftrMap.maxType && at != -1)
									{
										kick("Invalid block type!");
									}
									//else if(abs(ax-x)>24 || abs(ay-y)>24)
									//{
									//	kick("Invalid block placement position!");
									//}
									byte[] t33 = new byte[4];
									t33[0] = at;
									t33[3] = aco;
									t33[2] = ach;
									while(serv.map.maplock) { try{ Thread.sleep(1); } catch(Exception e) {} }
									serv.map.modlock=true;
									//while(serv.map.bslock) { Thread.sleep(1); }
	 								synchronized(serv.map)
	 								{
	 									if(at == -1)
	 									{
	 										serv.map.setPushable(ax,ay,ach,aco);
	 									} else {
	 										serv.map.setBlock(ax,ay,t33[0],t33[1],t33[2],t33[3]);
	 										serv.map.setPushable(ax,ay,(byte)0,(byte)0);
	 									}
	 								}
									serv.map.physics.addBlockToCheck(new CraftrBlockPos(ax,ay));
									for(int i=0;i<4;i++)
									{
										serv.map.physics.addBlockToCheck(new CraftrBlockPos(ax+serv.map.xMovement[i],ay+serv.map.yMovement[i]));
									}
									synchronized(out)
									{
										out.writeByte(0x31);
										out.writeByte((byte)id);
										out.writeInt(ax);
										out.writeInt(ay);
										out.writeByte(t33[0]);
										out.writeByte(t33[2]);
										out.writeByte(t33[3]);
										serv.sendOthers(id,getPacket());
	 									if(at != -1 && zc[5] != 0)
	 									{
	 										out.writeByte(0x31);
	 										out.writeByte((byte)id);
	 										out.writeInt(ax);
	 										out.writeInt(ay);
	 										out.writeByte(-1);
	 										out.writeByte(0);
	 										out.writeByte(0);
	 										serv.sendOthers(id,getPacket());
	 									}
									}
									serv.map.modlock=false;
								}
								break;
							case 0x40:
								String al = readString();
								System.out.println("<" + nick + "> " + al);
								String alt = serv.parseMessage(al,id);
								if(alt.equals("$N") && !al.equals("")) sendChatMsgAll("<" + nick + "> " + al);
								else if (!alt.equals("")) sendChatMsgSelf(alt);
								break;
							case 0x51:
								byte[] t51 = new byte[32];
								in.read(t51,0,32);
								if(!auth.testDecrypt(t51))
								{
									kick("Incorrect password!");
								}
								else passWait=false;
 								break;
							case 0x70:
								int xb = in.readInt();
								int yb = in.readInt();
								byte bt = in.readByte();
								serv.map.setBullet(xb,yb,bt);
								serv.map.setBulletNet(xb,yb,bt);
								serv.map.physics.addBlockToCheck(new CraftrBlockPos(xb,yb));
								break;
 							case 0xE0:
 								{
 									int lolx = this.x;
 									int loly = this.y;
 									int lolvx = in.readByte();
 									int lolvy = in.readByte();

 									if((lolvx != 0 && lolvy != 0) || (lolvx == 0 && lolvy == 0))
 										kick("Invalid touch distance!");
 									else if(lolvx < -1 || lolvx > 1 || lolvy < -1 || lolvy > 1)
 										kick("Invalid touch distance!");
 									else {
 										byte[] dq;
 										boolean pa;
 										synchronized(serv.map)
 										{
 											dq = serv.map.getBlock(lolx+lolvx,loly+lolvy).getBlockData();
 											pa = serv.map.pushAttempt(lolx+lolvx,loly+lolvy,lolvx,lolvy);
 										}
 										if(pa)
 										{
 											serv.map.setPlayer(x,y,0);
 											x = lolx+lolvx;
 											y = loly+lolvy;
 											synchronized(out)
 											{
 												out.writeByte(0x32);
 												out.writeByte(255);
 												out.writeInt(lolx+lolvx);
 												out.writeInt(loly+lolvy);
 												out.writeByte((byte)lolvx);
 												out.writeByte((byte)lolvy);
 												out.writeByte(dq[4]);
 												out.writeByte(dq[5]);
 												sendPacket();
 												out.writeByte(0x32);
 												out.writeByte(id);
 												out.writeInt(lolx+lolvx);
 												out.writeInt(loly+lolvy);
 												out.writeByte((byte)lolvx);
 												out.writeByte((byte)lolvy);
 												out.writeByte(dq[4]);
 												out.writeByte(dq[5]);
 												serv.sendOthers(id,getPacket());
 												serv.map.setPlayer(this.x,this.y,1);
 												serv.map.setPlayer(this.x+lolvx,this.y+lolvy,1);
 											}
 										}
 									}
 								}
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
							default:
								System.out.println("Unknown packet " + (int)(buf[0]&0xFF) + "!");
								break; // Ignore.
						}
					}
				}
				Thread.sleep(5);
				frames++;
				if(frames%625==0) // every 10 seconds
				{
					synchronized(out)
					{
						out.writeByte(0xF0);
						sendPacket();
					}
					pingsWaiting++;
					if(pingsWaiting>20) disconnect();
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Fatal CraftrServer client thread loop error!");
			e.printStackTrace();
			dc = 1;
		}
	}
	// for people who didn't get the earlier comment, it's an inside joke of sorts
}
