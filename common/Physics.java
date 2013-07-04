package common;

import java.lang.*;
import java.util.*;

public class Physics
{
	private Set<BlockPos> blocksToCheck = new HashSet<BlockPos>();
	private Set<BlockPos> blocksToCheckOld = new HashSet<BlockPos>();
	private Set<BlockPos> blocksToClear = new HashSet<BlockPos>();
	private Set<BlockPos> blocksToClearOld = new HashSet<BlockPos>();
	private Set<Block> blocksToSet = new HashSet<Block>();
	private Set<Block> blocksToSetOld = new HashSet<Block>();
	private static final int[] pnandDir = {26,27,25,24};
	private static final int[] pnandDir2 = {27,26,24,25};
	private static final int[] extendDir = {16,17,31,30};
	private static final int[] extendDir2 = {17,16,30,31};
	private static final int[] pistonDir = {198,181,210,208};
	private static final int[] pistonDir2 = {181,198,208,210};
	private static final int[] xMovement = { -1, 1, 0, 0, 0 };
	private static final int[] yMovement = { 0, 0, -1, 1, 0 };
	private boolean isServer;
	private Random rand;
	
	private Date lastBUpdate = new Date();
	private boolean changeBullets = false;
	public Player[] players = new Player[256];
	public Physics(boolean _isServer)
	{
		System.out.println("[MAP] [PHYSICS] Initializing...");
		isServer = _isServer;
		players = new Player[256];
		rand = new Random();
	}

	public static boolean isUpdated(int type)
	{
		return (type>=2 && type<=4) || type==6 || type==7 || (type>=10 && type<=13) || type==15 || type==17 || type==20;
	}
	
	public static boolean isReloaded(int type)
	{
		return (type>=21 && type<=23);
	}

	public static boolean isSent(int type)
	{
		return !(type == 5 || type == 6);
	}

	public boolean shoot(int x, int y, int dir, WorldMap map)
	{
		if(dir>3 || dir<0) return false;
		int nx = x+xMovement[dir];
		int ny = y+yMovement[dir];
		Block nb = map.getBlock(nx,ny);
		if(nb.isEmpty())
		{
			nb.setBullet((byte)(dir+1));
			addBlockToSet(nb);
			addBlockToCheck(new BlockPos(nx,ny));
			return true;
		}
		else return false;
	}

	public void tick(WorldMap modifiedMap)
	{
		Set<BlockPos> tempb;
		synchronized(blocksToCheck)
		{
			tempb = blocksToCheck;
			blocksToCheck = blocksToCheckOld;
			blocksToCheckOld = tempb;
			blocksToCheck.clear();
		}
		if((System.currentTimeMillis()-lastBUpdate.getTime())>=100)
		{
			changeBullets=true;
			lastBUpdate = new Date();
		}
		for(BlockPos cbp:blocksToCheckOld)
		{
			runPhysics(cbp,modifiedMap);
		}
		changeBullets=false;
		synchronized(modifiedMap)
		{
			Set<BlockPos> tempc;
			synchronized(blocksToClear)
			{
				tempc = blocksToClear;
				blocksToClear = blocksToClearOld;
				blocksToClearOld = tempc;
				blocksToClear.clear();
			}
			for (BlockPos cbp:blocksToClearOld)
			{
				modifiedMap.clearBlock(cbp.getX(),cbp.getY());
				if(isServer) modifiedMap.clearBlockNet(cbp.getX(),cbp.getY());
			}
			Set<Block> temps;
			synchronized(blocksToSet)
			{
				temps = blocksToSet;
				blocksToSet = blocksToSetOld;
				blocksToSetOld = temps;
				blocksToSet.clear();
			}
			for(Block cb:blocksToSetOld)
			{
				Block cbo = modifiedMap.getBlock(cb.x,cb.y);
				if(cb.isPushable()) modifiedMap.setPushable(cb.x,cb.y,cb.getChar(),cb.getColor());
				else modifiedMap.setBlock(cb.x,cb.y,cb.getTypeWithVirtual(),cb.getParam(),modifiedMap.updateLook(cb),cb.getColor());
				if(cb.getBullet()!=cbo.getBullet())
				{
					modifiedMap.setBullet(cb.x,cb.y,(byte)cb.getBullet(),(byte)cb.getBulletParam());
					if(isServer) modifiedMap.setBulletNet(cb.x,cb.y,(byte)cb.getBullet());
				}
				if(isServer && isSent(cb.getTypeWithVirtual()))
				{
					if(cb.isPushable()) modifiedMap.setBlockNet(cb.x,cb.y,(byte)cb.getTypeWithVirtual(),(byte)cb.getChar(),(byte)cb.getColor());
					else {
						if(cbo.getBlockChar() == modifiedMap.updateLook(cb) && cbo.getColor() == cb.getColor()) continue;
						modifiedMap.setBlockNet(cb.x,cb.y,(byte)cb.getTypeWithVirtual(),(byte)modifiedMap.updateLook(cb),(byte)cb.getColor());
					}
				}
			}
		}
		blocksToSetOld.clear();
	}
	
	public void addBlockToCheck(BlockPos cbp)
	{
		synchronized(blocksToCheck)
		{
			blocksToCheck.add(cbp);
		}
	}

	public void addBlockToClear(BlockPos cbp)
	{
		synchronized(blocksToClear)
		{
			blocksToClear.add(cbp);
		}
	}
	
	public void addBlockToSet(Block cb)
	{
		synchronized(blocksToSet)
		{
			blocksToSet.add(cb);
		}
	}

	public void runPhysics(BlockPos cbp, WorldMap map)
	{
		int x = cbp.getX();
		int y = cbp.getY();
		Block blockO = map.getBlock(x,y);
		byte[] blockData = blockO.getBlockData();
		Block[] surrBlockO = new Block[4];
		byte[][] surrBlockPre = new byte[4][];
		int[][] surrBlockData = new int[4][Block.getBDSize()];
		for(int i=0;i<4;i++)
		{
			surrBlockO[i]=map.getBlock(x+xMovement[i],y+yMovement[i]);
			if(surrBlockO[i] == null)
			{
				surrBlockO[i] = new Block(x+xMovement[i],y+yMovement[i]);
			}
			surrBlockPre[i] = surrBlockO[i].getBlockData();
			for(int j=0;j<Block.getBDSize();j++)
			{
				surrBlockData[i][j]=0xFF&(int)surrBlockPre[i][j];
			}
		}
		// Strength and physics code
		int[] strength = new int[4];
		for(int i=0;i<4;i++)
		{
			switch(surrBlockData[i][0])
			{
				case 2:
					strength[i]=(surrBlockData[i][1]>>4)!=(i^1)?surrBlockData[i][1]&15:0;
					break;
				case 3:
					if(pnandDir[i]==surrBlockData[i][2]) strength[i]=surrBlockData[i][1]&15;
					break;
				case 4:
					if( ((surrBlockData[i][1]>>(i^1))&1)!=0 && (surrBlockData[i][1]>>4)>0 ) strength[i]=15;
					break;
				case 5:
				case 9:
				case 13:
					if(surrBlockData[i][1]>0) strength[i]=15;
					break;
				case 15:
					if(extendDir[i]==surrBlockData[i][2]) strength[i]=(surrBlockData[i][1]!=0)?15:0;
					break;
				default:
					strength[i]=0;
					break;
			}
		}
		// Bullet code
		if(changeBullets && blockData[6]>0 && blockData[6]<=4)
		{
			boolean bshot = false;
			for(int i=0;i<256;i++)
			{
				if(players[i]!=null && players[i].px==blockO.x && players[i].py==blockO.y)
				{
					synchronized(map)
					{
						map.kill(i);
					}
					bshot = true;
				}
			}
			if(blockData[6]>0 && blockData[6]<=4 && !bshot)
			{
				if(surrBlockO[blockData[6]-1].isEmpty())
				{
					surrBlockO[blockData[6]-1].setBullet((byte)blockO.getBullet());
					addBlockToSet(surrBlockO[blockData[6]-1]);
					addBlockToCheck(new BlockPos(surrBlockO[blockData[6]-1].x,surrBlockO[blockData[6]-1].y));
					for(int i=0;i<4;i++)
					{
						int tbx = surrBlockO[blockData[6]-1].x+xMovement[i];
						int tby = surrBlockO[blockData[6]-1].y+yMovement[i];
						if(isUpdated(map.getBlock(tbx,tby).getType())) addBlockToCheck(new BlockPos(tbx,tby));
					}
				} else
				{
					int tmpt = surrBlockO[blockData[6]-1].getType();
					if(tmpt == 23 || tmpt == 21) addBlockToClear(new BlockPos(x+xMovement[blockData[6]-1],y+yMovement[blockData[6]-1]));
					for(int i=0;i<256;i++)
					{
						if(players[i]!=null && players[i].px==blockO.x+xMovement[blockData[6]-1] && players[i].py==blockO.y+yMovement[blockData[6]-1])
						{
							synchronized(map)
							{
								map.kill(i);
							}
							bshot = true;
						}
					}
				}
			}
			if(surrBlockO[blockData[6]-1].getType()==14)
			{
				addBlockToClear(new BlockPos(surrBlockO[blockData[6]-1].x,surrBlockO[blockData[6]-1].y)); // this makes an empty block.
			}
			blockO.setBullet((byte)0);
			addBlockToSet(blockO);
			addBlockToCheck(new BlockPos(blockO.x,blockO.y));
		}
		else if(!changeBullets && blockData[6]!=0)
		{
			addBlockToCheck(new BlockPos(x,y));
		}
		byte oldColour = blockData[3];
		byte oldParam = blockData[1];
		int maxSignal = 0;
		int lowParam = blockData[1]&15;
		switch(blockData[0])
		{
			case 2:
			{
				int maxSignalDir=4;
				int oldMaxSignalDir=((0xFF&(int)blockData[1])>>4)&7;
				for(int i=0;i<4;i++)
				{
					int str = strength[i];
					if(surrBlockData[i][0] == 2 && ((surrBlockData[i][3]&7) != (blockData[3]&7))) continue; // Same color check
					if(oldMaxSignalDir<4 && (oldMaxSignalDir^1)==i) continue;
					if(str>maxSignal && str>lowParam) { maxSignal=str; maxSignalDir=i;}
				}
				if(maxSignal<=1)
				{
					if(lowParam>0) blockData[3]=(byte)(blockData[3]&7);
					if(oldColour!=((byte)(maxSignalDir<<4)))
					{
						addBlockToSet(new Block(x,y,blockData[0],(byte)(maxSignalDir<<4),blockData[2],blockData[3]));
						addBlockToCheck(new BlockPos(x,y));
						for(int i=0;i<4;i++)
						{
							int ty = surrBlockData[i][0];
							if(isUpdated(ty)) { addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i])); }
						}
					}
				}
				else
				{
					if(lowParam==0) blockData[3]=(byte)((blockData[3]&7)|8);
					if(oldColour!=((byte)((maxSignal-1) | (maxSignalDir<<4))))
					{
						addBlockToSet(new Block(x,y,blockData[0],(byte)((maxSignal-1) | (maxSignalDir<<4)),blockData[2],blockData[3]));
						addBlockToCheck(new BlockPos(x,y));
						for(int i=0;i<4;i++)
						{
							int ty = surrBlockData[i][0];
							if(isUpdated(ty)) { addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i])); }
						}
					}
				}
			} break;
			case 3:
			{
				int outputDirection = 3;
				if (blockData[2]>=24 && blockData[2]<28)
				{
					for(int i=0;i<4;i++)
					{
						if(pnandDir2[i]==blockData[2]) { outputDirection=i; break; }
					}
				}
				int signalCount=0;
				for(int i=0;i<4;i++)
				{
					if(i==outputDirection) continue;
					int type = surrBlockData[i][0];
					if((type==1) || strength[i]>0) { signalCount++; }
				}
				if(signalCount==1 || signalCount==2)
				{
					if(lowParam==0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
					addBlockToSet(new Block(x,y,blockData[0],(byte)15,blockData[2],blockData[3]));
					int type = surrBlockData[outputDirection][0];
					blockData[1]=15;
					if(oldColour!=15 && isUpdated(type)) {
						addBlockToCheck(new BlockPos(x+xMovement[outputDirection],y+yMovement[outputDirection]));
					}
				}
				else
				{
					if(lowParam>0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
					addBlockToSet(new Block(x,y,blockData[0],(byte)0,blockData[2],blockData[3]));
					int type = surrBlockData[outputDirection][0];
					blockData[1]=0;
					if(oldColour!=0 && isUpdated(type)) {
						addBlockToCheck(new BlockPos(x+xMovement[outputDirection],y+yMovement[outputDirection]));
					}
				}
				if(oldColour!=blockData[1])
				{
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			case 4:
			{
				int newParam=0;
				int oldParamInt = 0xFF&(int)blockData[1];
				for(int i=0;i<4;i++)
				{
					// SETUP
					int t = surrBlockData[i][0];
					int str = strength[i];
					int rstr = strength[i^1];
					// PARAM CONFIGURATION
					boolean t1 = ((oldParamInt>>i)&1)>0; // was it sending in that direction?
					boolean t3 = ((oldParamInt>>(i^1))&1)>0; // was it sending opposite?
					boolean t2 = false; // should it be sending in that direction?
					if(!t3 && rstr>1)
					{
						t2=true;
						if((surrBlockData[i^1][0]==2 && (surrBlockData[i^1][1]>>4)==i)) t2=false;
					}
					if(t2) { newParam|=1<<i; }
					else if (str>maxSignal && str>1) { maxSignal=str; }
				}
				if(maxSignal>1)
				{
					newParam |= ((maxSignal-1)<<4);
				}
				if(oldColour!=newParam)
				{
					addBlockToSet(new Block(x,y,blockData[0],(byte)newParam,blockData[2],blockData[3]));
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			case 5:
			{
				int co = blockData[1]&0x01;
				int on = (int)blockData[1]&0x80;
				if((on!=0) || (co!=0))
				{
					addBlockToCheck(new BlockPos(x,y));
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
					}
					blockData[1] = (byte)(on|(co^1));
					addBlockToSet(new Block(x,y,blockData[0],blockData[1],blockData[2],blockData[3]));
				}
			} break;
			case 6:
			{
				int signalCount=0;
				int counter = (int)blockData[1]&0x7F;
				int prevon = 0x80&(int)blockData[1];
				if(counter>0) counter-=1;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { signalCount++; }
				}
				if(signalCount>0)
				{
					if(prevon==0)
					{
						synchronized(map)
						{
							if(isServer) map.setPlayerNet(x,y,1);
							map.playSample(x,y,3);
						}
					}
					addBlockToSet(new Block(x,y,blockData[0],(byte)(counter|0x80),blockData[2],blockData[3]));
				}
				else
				{
					if(prevon!=0)
					{
						synchronized(map)
						{
							if(isServer) map.setPlayerNet(x,y,0);
							map.playSample(x,y,2);
						}
					}
					addBlockToSet(new Block(x,y,blockData[0],(byte)counter,blockData[2],blockData[3]));
				}
				//addBlockToCheck(new BlockPos(x,y));
				for(int i=0;i<4;i++)
				{
					int t = surrBlockData[i][0];
					int str = strength[i];
					if(isUpdated(t)) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
				}
			} break;
			case 7:
			{
				int signalCount=0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { signalCount++; }
				}
				if(signalCount>0 && (blockData[1]&1)==0)
				{
					synchronized(map) { map.playSound(x,y,(0xFF&(int)blockData[2])%248); }
				}
				int np=signalCount>0?1:0;
				if((blockData[1]&1)!=np) addBlockToSet(new Block(x,y,blockData[0],(byte)np,blockData[2],blockData[3]));
			} break;
			case 9: // Pensor
			{
				int co = blockData[1]&0x7F;
				int on = (int)blockData[1]&0x80;
				int si = 0;
				boolean dc = false;
				for(int i=0;i<4;i++)
				{
					if(((surrBlockData[i][5]&0x0f)!=0) && ( ((surrBlockData[i][5]&0x0F)==(blockData[3]&0x0F)) || (blockData[3]&0x0F)==0 )) si++;
				}
				if(co>0)
				{
					dc=true;
					if(co>1) addBlockToCheck(new BlockPos(x,y));
					else on=0;
					addBlockToSet(new Block(x,y,blockData[0],on|(co-1),blockData[2],blockData[3]));
				}
				else if(on==0 && si>0)
				{
					dc=true;
					on=0x80;
					addBlockToSet(new Block(x,y,blockData[0],0x82,blockData[2],blockData[3]));
					addBlockToCheck(new BlockPos(x,y));
				}
				else if(on>0 && si==0)
				{
					dc=true;
					on=0;
					addBlockToSet(new Block(x,y,blockData[0],0,blockData[2],blockData[3]));
					addBlockToCheck(new BlockPos(x,y));
				}
				if(dc)
				{
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			case 10: // Finally, Pumulty
			{
				int on = blockData[1]&0x07;
				int non = 0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { non = (i^1)+1; break; } 
				}
				if(non!=on)
				{
					addBlockToSet(new Block(x,y,blockData[0],non,blockData[2],blockData[3]));
					if(non>0 && non<5)
					{
						synchronized(map)
						{
							if((blockData[3]&0x0F)!=0)
							{
								map.tryPushM(x,y,xMovement[non-1],yMovement[non-1],blockData[2],(byte)(blockData[3]&0x0F));
							}
							else if (surrBlockData[(non-1)][5]!=0)
							{
								map.setPushable(x+xMovement[non-1],y+yMovement[non-1],(byte)0,(byte)0);
								if(isServer)
									map.setPushableNet(x+xMovement[non-1],y+yMovement[non-1],(byte)0,(byte)0);
							}
						}
					}
				}
			} break;
			case 11: // Bmodder
			{
				int on = blockData[1]&0x07;
				int non = 0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { non = (i^1)+1; break; } 
				}
				if(non!=on)
				{
					addBlockToSet(new Block(x,y,blockData[0],non,blockData[2],blockData[3]));
					if(non>0 && non<5)
					{
						Block newBlock = new Block(x+xMovement[non-1],y+yMovement[non-1],surrBlockPre[non-1]);
						if(blockData[2]!=0) newBlock.setChar(0xFF&(int)blockData[2]);
						if(blockData[3]!=0) newBlock.setColor(0xFF&(int)blockData[3]);
						addBlockToSet(newBlock);
					}
				}
			} break;
			case 12: // Cannona
			{
				int on = blockData[1]&0x07;
				int non = 0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { non = (i^1)+1; break; } 
				}
				if(non!=on)
				{
					addBlockToSet(new Block(x,y,blockData[0],non,blockData[2],blockData[3]));
					if(non>0 && non<5)
					{
						if(surrBlockData[(non-1)][6]==0 && surrBlockO[(non-1)].isEmpty())
						{
							surrBlockO[(non-1)].setBullet((byte)non);
							addBlockToSet(surrBlockO[(non-1)]);
							addBlockToCheck(new BlockPos(surrBlockO[(non-1)].x,surrBlockO[(non-1)].y));
						}
					}
				}
			} break;
			case 13: // Bullsor
			{
				int co = blockData[1]&0x7F;
				int on = (int)blockData[1]&0x80;
				int si = 0;
				boolean dc = false;
				for(int i=0;i<4;i++)
				{
					if(surrBlockData[i][6]!=0) si++;
				}
				if(co>0)
				{
					dc=true;
					if(co>1) addBlockToCheck(new BlockPos(x,y));
					else on=0;
					addBlockToSet(new Block(x,y,blockData[0],on|(co-1),blockData[2],blockData[3]));
				}
				else if(on==0 && si>0)
				{
					dc=true;
					on=0x80;
					addBlockToSet(new Block(x,y,blockData[0],0x82,blockData[2],blockData[3]));
					addBlockToCheck(new BlockPos(x,y));
				}
				else if(on>0 && si==0)
				{
					dc=true;
					on=0;
					addBlockToSet(new Block(x,y,blockData[0],0,blockData[2],blockData[3]));
					addBlockToCheck(new BlockPos(x,y));
				}
				if(dc)
				{
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			case 15: // Extend
			{
				int outputDirection = 3;
				for(int i=0;i<4;i++)
				{
					if(extendDir2[i]==blockData[2]) { outputDirection=i; break; }
				}
				int signalCount=0;
				for(int i=0;i<4;i++)
				{
					if(i==outputDirection) continue;
					int ty = surrBlockData[i][0];
					int str = strength[i];
					if((ty==1) || str>0) { signalCount=1; break; }
				}
				if(blockData[1]!=(byte)0)
				{
					blockData[1]-=(byte)1;
					if(blockData[1]==(byte)0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
				}
				if(signalCount>0)
				{
					if(blockData[1]==(byte)0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
					blockData[1]+=(byte)2;
				}
				addBlockToSet(new Block(x,y,blockData[0],blockData[1],blockData[2],blockData[3]));
				int ty = surrBlockData[outputDirection][0];
				if(isUpdated(ty)) addBlockToCheck(new BlockPos(x+xMovement[outputDirection],y+yMovement[outputDirection]));
				if(oldColour!=blockData[1])
				{
					addBlockToCheck(new BlockPos(x,y));
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			case 17: // Pusher
			{
				int signalCount=0;
				int oldSignalCount=(int)blockData[1]&0x01;
				int val = (int)blockData[1]>>1;
				int st = 0;
				//System.out.println("" + (int)blockData[3]);
				if(((int)blockData[3]) == 46) st=1;
				int i = 0;
				for(;i<4;i++)
				{
					if(strength[i]>0) { signalCount++; i=i^1; break;}
				}
				if(signalCount>0 && oldSignalCount==0)
				{
					int activ=0;
					if (map.piston(x,y,xMovement[i],yMovement[i],false))
						activ=1;
					if(map.getBlock(x+xMovement[i],y+yMovement[i]).isEmpty())
					{
						activ=1;
						addBlockToSet(new Block(x+xMovement[i],y+yMovement[i],16,(byte)0,(byte)pistonDir[i],(byte)7));
					}
					addBlockToSet(new Block(x,y,blockData[0],(byte)(activ | (i<<1)),blockData[2],blockData[3]));
				}
				else if(signalCount==0 && oldSignalCount>0 && val<4)
				{
					i=val;
					if(st>0) map.piston(x+xMovement[i],y+yMovement[i],xMovement[i],yMovement[i],true);
					Block ph = map.getBlock(x+xMovement[i],y+yMovement[i]);
					if(ph.getType()==16 && ph.getChar()==pistonDir[i]) addBlockToClear(new BlockPos(x+xMovement[val],y+yMovement[val]));
					addBlockToSet(new Block(x,y,blockData[0],(byte)((4<<1)),blockData[2],blockData[3]));
				}
				else if(signalCount==0 && oldSignalCount==0 && val<4 && surrBlockData[val][0]==16)
					addBlockToClear(new BlockPos(x+xMovement[val],y+yMovement[val]));
				for(i=0;i<4;i++)
				{
					int t = surrBlockData[i][0];
					int str = strength[i];
					if(isUpdated(t) && t!=17) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
				}
			} break;
			case 20: { // Dupe
				int signalCount=0;
				int oldSignalCount=(int)blockData[1]&0x01;
				int chr = (int)blockData[2]&0xFF;
				int i = 0;
				int j = 1;
				for(;i<4;i++)
				{
					if(strength[i]>0) { signalCount++;}
					if(chr==pnandDir[i]) { j = i; }
				}
				i = j;
				if(signalCount>0 && oldSignalCount==0)
				{
					byte[] tbl = map.getBlock(x+xMovement[i],y+yMovement[i]).getBlockData();
					i=i^1;
					addBlockToSet(new Block(x+xMovement[i],y+yMovement[i],tbl));
					addBlockToSet(new Block(x,y,blockData[0],(byte)1,blockData[2],blockData[3]));
				}
				else if(signalCount==0 && oldSignalCount>0)
				{
					addBlockToSet(new Block(x,y,blockData[0],(byte)0,blockData[2],blockData[3]));
				}
				for(i=0;i<4;i++)
				{
					int t = surrBlockData[i][0];
					int str = strength[i];
					if(isUpdated(t) && t!=20) addBlockToCheck(new BlockPos(x+xMovement[i],y+yMovement[i]));
				}
			} break;
			default:
				break;
		}
	}
}
