package common;

import java.lang.*;
import java.util.*;

public class CraftrPhysics
{
	private Set<CraftrBlockPos> blocksToCheck = new HashSet<CraftrBlockPos>();
	private Set<CraftrBlockPos> blocksToCheckOld = new HashSet<CraftrBlockPos>();
	private Set<CraftrBlock> blocksToSet = new HashSet<CraftrBlock>();
	private Set<CraftrBlock> blocksToSetOld = new HashSet<CraftrBlock>();
	private static final int[] pnandDir = {26,27,25,24};
	private static final int[] pnandDir2 = {27,26,24,25};
	private static final int[] extendDir = {16,17,31,30};
	private static final int[] extendDir2 = {17,16,30,31};
	private static final int[] xMovement = { -1, 1, 0, 0 };
	private static final int[] yMovement = { 0, 0, -1, 1 };
	private boolean isServer;
	
	private Date lastBUpdate = new Date();
	private boolean changeBullets = false;

	public CraftrPlayer[] players = new CraftrPlayer[256];
	public CraftrPhysics(boolean _isServer)
	{
		System.out.println("[MAP] [PHYSICS] Initializing...");
		isServer = _isServer;
		players = new CraftrPlayer[256];
	}

	public boolean isUpdated(int type)
	{
		return (type>=2 && type<=4) || type==6 || type==7 || type==10 || type==11 || type==12 || type==13 || type==15;
	}
	
	public boolean isSent(int type)
	{
		return !(type == 5 || type == 6);
	}

	public void tick(CraftrMap modifiedMap)
	{
		Set<CraftrBlockPos> tempb;
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
		for(CraftrBlockPos cbp:blocksToCheckOld)
		{
			runPhysics(cbp,modifiedMap);
		}
		changeBullets=false;
		synchronized(modifiedMap)
		{
			Set<CraftrBlock> temps;
			synchronized(blocksToSet)
			{
				temps = blocksToSet;
				blocksToSet = blocksToSetOld;
				blocksToSetOld = temps;
				blocksToSet.clear();
			}
			for(CraftrBlock cb:blocksToSetOld)
			{
				CraftrBlock cbo = modifiedMap.getBlock(cb.x,cb.y);
				if(cb.isPushable()) modifiedMap.setPushable(cb.x,cb.y,cb.getChar(),cb.getColor());
				else modifiedMap.setBlock(cb.x,cb.y,cb.getTypeWithVirtual(),cb.getParam(),modifiedMap.updateLook(cb),cb.getColor());
				if(cb.getBullet()!=cbo.getBullet())
				{
					modifiedMap.setBullet(cb.x,cb.y,(byte)cb.getBullet());
					if(isServer) modifiedMap.setBulletNet(cb.x,cb.y,(byte)cb.getBullet());
				}
				if(isServer && isSent(cb.getTypeWithVirtual()))
				{
					if(cb.isPushable()) modifiedMap.setBlockNet(cb.x,cb.y,(byte)cb.getTypeWithVirtual(),(byte)cb.getChar(),(byte)cb.getColor());
					else modifiedMap.setBlockNet(cb.x,cb.y,(byte)cb.getTypeWithVirtual(),(byte)modifiedMap.updateLook(cb),(byte)cb.getColor());
				}
			}
		}
		blocksToSetOld.clear();
	}
	
	public void addBlockToCheck(CraftrBlockPos cbp)
	{
		synchronized(blocksToCheck)
		{
			blocksToCheck.add(cbp);
		}
	}
	
	public void addBlockToSet(CraftrBlock cb)
	{
		blocksToSet.add(cb);
	}

	public void runPhysics(CraftrBlockPos cbp, CraftrMap map)
	{
		int x = cbp.getX();
		int y = cbp.getY();
		CraftrBlock blockO = map.getBlock(x,y);
		byte[] blockData = blockO.getBlockData();
		CraftrBlock[] surrBlockO = new CraftrBlock[4];
		byte[][] surrBlockPre = new byte[4][];
		int[][] surrBlockData = new int[4][CraftrBlock.getBDSize()];
		for(int i=0;i<4;i++)
		{
			surrBlockO[i]=map.getBlock(x+xMovement[i],y+yMovement[i]);
			surrBlockPre[i] = surrBlockO[i].getBlockData();
			for(int j=0;j<CraftrBlock.getBDSize();j++)
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
		if(changeBullets && blockData[6]!=0)
		{
			boolean bshot = false;
			for(int i=0;i<256;i++)
			{
				if(players[i]!=null && players[i].px==blockO.x && players[i].py==blockO.y)
				{
					map.se.kill(i);
					bshot = true;
				}
			}
			if(blockData[6]>0 && blockData[6]<=4 && !bshot)
			{
				if(surrBlockO[blockData[6]-1].isEmpty())
				{
					surrBlockO[blockData[6]-1].setBullet((byte)blockO.getBullet());
					addBlockToSet(surrBlockO[blockData[6]-1]);
					addBlockToCheck(new CraftrBlockPos(surrBlockO[blockData[6]-1].x,surrBlockO[blockData[6]-1].y));
					for(int i=0;i<4;i++)
					{
						int tbx = surrBlockO[blockData[6]-1].x+xMovement[i];
						int tby = surrBlockO[blockData[6]-1].y+yMovement[i];
						if(isUpdated(map.getBlock(tbx,tby).getType())) addBlockToCheck(new CraftrBlockPos(tbx,tby));
					}
				} else
				{
					for(int i=0;i<256;i++)
					{
						if(players[i]!=null && players[i].px==blockO.x+xMovement[blockData[6]-1] && players[i].py==blockO.y+yMovement[blockData[6]-1])
						{
							map.se.kill(i);
							bshot = true;
						}
					}
				}
			}
			if(surrBlockO[blockData[6]-1].getType()==14)
			{
				addBlockToSet(new CraftrBlock(surrBlockO[blockData[6]-1].x,surrBlockO[blockData[6]-1].y)); // this makes an empty block.
			}
			blockO.setBullet((byte)0);
			addBlockToSet(blockO);
			addBlockToCheck(new CraftrBlockPos(blockO.x,blockO.y));
		}
		else if(!changeBullets && blockData[6]!=0)
		{
			addBlockToCheck(new CraftrBlockPos(blockO.x,blockO.y));
		}
		byte oldd3 = blockData[3];
		byte oldd1 = blockData[1];
		int maxSignal = 0;
		int lowParam = blockData[1]&15;
		switch(blockData[0])
		{
			case 2:
			{
				int mSi=4;
				int oldmSi=((0xFF&(int)blockData[1])>>4)&7;
				for(int i=0;i<4;i++)
				{
					int ty = surrBlockData[i][0];
					int str = strength[i];
					if(oldmSi<4 && (oldmSi^1)==i) continue;
					if(str>maxSignal && str>lowParam) { maxSignal=str; mSi=i;}
				}
				if(maxSignal<=1)
				{
					if(lowParam>0) blockData[3]=(byte)(blockData[3]&7);
					if(oldd1!=((byte)(mSi<<4)))
					{
						addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)(mSi<<4),blockData[2],blockData[3]));
						addBlockToCheck(new CraftrBlockPos(x,y));
						for(int i=0;i<4;i++)
						{
							int ty = surrBlockData[i][0];
							if(isUpdated(ty)) { addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i])); }
						}
					}
				}
				else
				{
					if(lowParam==0) blockData[3]=(byte)((blockData[3]&7)|8);
					if(oldd1!=((byte)((maxSignal-1) | (mSi<<4))))
					{
						addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)((maxSignal-1) | (mSi<<4)),blockData[2],blockData[3]));
						addBlockToCheck(new CraftrBlockPos(x,y));
						for(int i=0;i<4;i++)
						{
							int ty = surrBlockData[i][0];
							if(isUpdated(ty)) { addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i])); }
						}
					}
				}
				if(oldd1!=blockData[1])
				{
				}
			} break;
			case 3:
			{
				int pnps = 3;
				if (blockData[2]>=24 && blockData[2]<28)
				{
					for(int i=0;i<4;i++)
					{
						if(pnandDir2[i]==blockData[2]) { pnps=i; break; }
					}
				} 
				int signals=0;
				for(int i=0;i<4;i++)
				{
					if(i==pnps) continue;
					int ty = surrBlockData[i][0];
					int str = strength[i];
					if((ty==1) || str>0) { signals++; }
				}
				if(signals==1 || signals==2)
				{
					if(lowParam==0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
					addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)15,blockData[2],blockData[3]));
					int ty = surrBlockData[pnps][0];
					blockData[1]=15;
					int str = strength[pnps];
					if(oldd1!=15 && isUpdated(ty)) { addBlockToCheck(new CraftrBlockPos(x+xMovement[pnps],y+yMovement[pnps])); }
				}
				else
				{
					if(lowParam>0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
					addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)0,blockData[2],blockData[3]));
					int ty = surrBlockData[pnps][0];
					blockData[1]=0;
					int str = strength[pnps];
					if(oldd1!=0 && isUpdated(ty)) { addBlockToCheck(new CraftrBlockPos(x+xMovement[pnps],y+yMovement[pnps])); }
				}
				if(oldd1!=blockData[1])
				{
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			case 4:
			{
				int newParam=0;
				int oldparam = 0xFF&(int)blockData[1];
				//int maxSignal=0;
				for(int i=0;i<4;i++)
				{
					// SETUP
					int t = surrBlockData[i][0];
					int str = strength[i];
					int rstr = strength[i^1];
					// PARAM CONFIGURATION
					boolean t1 = ((oldparam>>i)&1)>0; // was it sending in that direction?
					boolean t3 = ((oldparam>>(i^1))&1)>0; // was it sending opposite?
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
				if(oldd1!=newParam)
				{
					addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)newParam,blockData[2],blockData[3]));
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			case 5:
			{
				int co = blockData[1]&0x01;
				int on = (int)blockData[1]&0x80;
				if((on!=0) || (co!=0))
				{
					addBlockToCheck(new CraftrBlockPos(x,y));
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
					blockData[1] = (byte)(on|(co^1));
					addBlockToSet(new CraftrBlock(x,y,blockData[0],blockData[1],blockData[2],blockData[3]));
				}
			} break;
			case 6:
			{
				int signalz=0;
				int counter = (int)blockData[1]&0x7F;
				int prevon = 0x80&(int)blockData[1];
				if(counter>0) counter-=1;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { signalz++; }
				}	
				if(signalz>0)
				{
					if(prevon==0)
					{
						if(isServer)
							map.setPlayerNet(x,y,1);
						map.playSample(x,y,3);
					}
					addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)(counter|0x80),blockData[2],blockData[3]));
				}
				else
				{
					if(prevon!=0)
					{
						if(isServer)
							map.setPlayerNet(x,y,0);
						map.playSample(x,y,2);
					}
					addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)counter,blockData[2],blockData[3]));
				}
				//addBlockToCheck(new CraftrBlockPos(x,y));
				for(int i=0;i<4;i++)
				{
					int t = surrBlockData[i][0];
					int str = strength[i];
					if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
				}
			} break;
			case 7:
			{
				int sig=0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { sig++; }
				}
				if(sig>0 && (blockData[1]&1)==0)
				{
					map.playSound(x,y,(0xFF&(int)blockData[2])%248);
				}
				int np=sig>0?1:0;
				if((blockData[1]&1)!=np) addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)np,blockData[2],blockData[3]));
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
					if(co>1) addBlockToCheck(new CraftrBlockPos(x,y));
					else on=0;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],on|(co-1),blockData[2],blockData[3]));
				}
				else if(on==0 && si>0)
				{
					dc=true;
					on=0x80;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],0x82,blockData[2],blockData[3]));
					addBlockToCheck(new CraftrBlockPos(x,y));
				}
				else if(on>0 && si==0)
				{
					dc=true;
					on=0;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],0,blockData[2],blockData[3]));
					addBlockToCheck(new CraftrBlockPos(x,y));
				}
				if(dc)
				{
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
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
					addBlockToSet(new CraftrBlock(x,y,blockData[0],non,blockData[2],blockData[3]));
					if(non>0 && non<5)
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
					addBlockToSet(new CraftrBlock(x,y,blockData[0],non,blockData[2],blockData[3]));
					if(non>0 && non<5)
					{
						CraftrBlock newBlock = new CraftrBlock(x+xMovement[non-1],y+yMovement[non-1],surrBlockPre[non-1]);
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
					addBlockToSet(new CraftrBlock(x,y,blockData[0],non,blockData[2],blockData[3]));
					if(non>0 && non<5)
					{
						if(surrBlockData[(non-1)][6]==0 && surrBlockO[(non-1)].isEmpty())
						{
							surrBlockO[(non-1)].setBullet((byte)non);
							addBlockToSet(surrBlockO[(non-1)]);
							addBlockToCheck(new CraftrBlockPos(surrBlockO[(non-1)].x,surrBlockO[(non-1)].y));
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
					if(co>1) addBlockToCheck(new CraftrBlockPos(x,y));
					else on=0;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],on|(co-1),blockData[2],blockData[3]));
				}
				else if(on==0 && si>0)
				{
					dc=true;
					on=0x80;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],0x82,blockData[2],blockData[3]));
					addBlockToCheck(new CraftrBlockPos(x,y));
				}
				else if(on>0 && si==0)
				{
					dc=true;
					on=0;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],0,blockData[2],blockData[3]));
					addBlockToCheck(new CraftrBlockPos(x,y));
				}
				if(dc)
				{
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			case 15: // Extend
			{
				int pnps = 3;
				for(int i=0;i<4;i++)
				{
					if(extendDir2[i]==blockData[2]) { pnps=i; break; }
				}
				int signals=0;
				for(int i=0;i<4;i++)
				{
					if(i==pnps) continue;
					int ty = surrBlockData[i][0];
					int str = strength[i];
					if((ty==1) || str>0) { signals=1; break; }
				}
				if(blockData[1]!=(byte)0)
				{
					blockData[1]-=(byte)1;
					if(blockData[1]==(byte)0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
				}
				if(signals>0)
				{
					if(blockData[1]==(byte)0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
					blockData[1]+=(byte)2;
				}
				addBlockToSet(new CraftrBlock(x,y,blockData[0],blockData[1],blockData[2],blockData[3]));
				int ty = surrBlockData[pnps][0];
				if(isUpdated(ty)) addBlockToCheck(new CraftrBlockPos(x+xMovement[pnps],y+yMovement[pnps]));
				if(oldd1!=blockData[1])
				{
					addBlockToCheck(new CraftrBlockPos(x,y));
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
			} break;
			default:
				break;
		}
	}
}
