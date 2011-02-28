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
	private static final int[] xMovement = { -1, 1, 0, 0 };
	private static final int[] yMovement = { 0, 0, -1, 1 };

	public CraftrPhysics()
	{
	}

	public boolean isUpdated(int type)
	{
		if((type>=2 && type<=4) || type==6 || type==7 || type==10 || type==11) return true;
		return false;
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
		for(CraftrBlockPos cbp:blocksToCheckOld)
		{
			runPhysics(cbp,modifiedMap);
		}
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
			if(cb.isPushable()) modifiedMap.setPushable(cb.x,cb.y,cb.getChar(),cb.getColor());
			else modifiedMap.setBlock(cb.x,cb.y,cb.getTypeWithVirtual(),cb.getParam(),modifiedMap.updateLook(cb),cb.getBlockColor());
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
		byte[] blockData = map.getBlock(x,y).getBlockData();
		byte[][] surrBlockPre = new byte[4][];
		int[][] surrBlockData = new int[4][6];
		for(int i=0;i<4;i++)
		{
			surrBlockPre[i]=map.getBlock(x+xMovement[i],y+yMovement[i]).getBlockData();
			for(int j=0;j<6;j++)
			{
				surrBlockData[i][j]=0xFF&(int)surrBlockPre[i][j];
			}
		}
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
					if(surrBlockData[i][1]>0) strength[i]=15;
					break;
				default:
					strength[i]=0;
					break;
			}
		}
		byte oldd3 = blockData[3];
		byte oldd1 = blockData[1];
		int maxSignal = 0;
		int ss = blockData[1]&15;
		switch(blockData[0])
		{
			case 2:
				int mSi=4;
				int oldmSi=((0xFF&(int)blockData[1])>>4)&7;
				for(int i=0;i<4;i++)
				{
					int ty = surrBlockData[i][0];
					int str = strength[i];
					if(oldmSi<4 && (oldmSi^1)==i) continue;
					if(str>maxSignal && str>ss) { maxSignal=str; mSi=i;}
				}
				if(maxSignal<=1)
				{
					if(ss>0) blockData[3]=(byte)(blockData[3]&7);
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
					if(ss==0) blockData[3]=(byte)((blockData[3]&7)|8);
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
				break;
			case 3:
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
					if(ss==0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
					addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)15,blockData[2],blockData[3]));
					int ty = surrBlockData[pnps][0];
					blockData[1]=15;
					int str = strength[pnps];
					if(oldd1!=15 && isUpdated(ty)) { addBlockToCheck(new CraftrBlockPos(x+xMovement[pnps],y+yMovement[pnps])); }
				}
				else
				{
					if(ss>0) blockData[3]=(byte)(((blockData[3]>>4)&15)|((blockData[3]<<4)&240));
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
				break;
			case 4:
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
				break;
			case 5:
				int co5 = blockData[1]&0x7F;
				int on = (int)blockData[1]&0x80;
				if(co5>0)
				{
					if(co5>1) addBlockToCheck(new CraftrBlockPos(x,y));
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
					blockData[1] = (byte)(on|(co5-1));
					addBlockToSet(new CraftrBlock(x,y,blockData[0],on|(co5-1),blockData[2],blockData[3]));
				}
				break;
			case 6:
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
					if(prevon==0) map.playSample(x,y,3);
					addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)(counter|0x80),blockData[2],blockData[3]));
				}
				else
				{
					if(prevon!=0) map.playSample(x,y,2);
					addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)counter,blockData[2],blockData[3]));
				}
				//addBlockToCheck(new CraftrBlockPos(x,y));
				for(int i=0;i<4;i++)
				{
					int t = surrBlockData[i][0];
					int str = strength[i];
					if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
				}
				break;
			case 7:
				int sig7=0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { sig7++; }
				}
				if(sig7>0 && (blockData[1]&1)==0)
				{
					map.playSound(x,y,(0xFF&(int)blockData[2])%248);
				}
				int np=sig7>0?1:0;
				if((blockData[1]&1)!=np) addBlockToSet(new CraftrBlock(x,y,blockData[0],(byte)np,blockData[2],blockData[3]));
				break;
			case 9: // Pensor
				int co8 = blockData[1]&0x7F;
				int on8 = (int)blockData[1]&0x80;
				int si8 = 0;
				boolean dc8 = false;
				for(int i=0;i<4;i++)
				{
					if(((surrBlockData[i][5]&0x0f)!=0) && ( ((surrBlockData[i][5]&0x0F)==(blockData[3]&0x0F)) || (blockData[3]&0x0F)==0 )) si8++;
				}
				if(co8>0)
				{
					dc8=true;
					if(co8>1) addBlockToCheck(new CraftrBlockPos(x,y));
					else on8=0;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],on8|(co8-1),blockData[2],blockData[3]));
				}
				else if(on8==0 && si8>0)
				{
					dc8=true;
					on8=0x80;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],0x84,blockData[2],blockData[3]));
					addBlockToCheck(new CraftrBlockPos(x,y));
				}
				else if(on8>0 && si8==0)
				{
					dc8=true;
					on8=0;
					addBlockToSet(new CraftrBlock(x,y,blockData[0],0,blockData[2],blockData[3]));
					addBlockToCheck(new CraftrBlockPos(x,y));
				}
				if(dc8)
				{
					for(int i=0;i<4;i++)
					{
						int t = surrBlockData[i][0];
						int str = strength[i];
						if(isUpdated(t)) addBlockToCheck(new CraftrBlockPos(x+xMovement[i],y+yMovement[i]));
					}
				}
				break;
			case 10: // Finally, Pumulty
				int on10 = blockData[1]&0x07;
				int non10 = 0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { non10 = (i^1)+1; break; } 
				}
				if(non10!=on10)
				{
					addBlockToSet(new CraftrBlock(x,y,blockData[0],non10,blockData[2],blockData[3]));
					if(non10>0 && non10<5)
					{
						if((blockData[3]&0x0F)!=0)
						{
							map.tryPushM(x,y,xMovement[non10-1],yMovement[non10-1],blockData[2],(byte)(blockData[3]&0x0F));
						}
						else if (surrBlockData[(non10-1)][5]!=0) map.setPushable(x+xMovement[non10-1],y+yMovement[non10-1],(byte)0,(byte)0);
					}
				}
				break;
			case 11: // Bmodder
				int on11 = blockData[1]&0x07;
				int non11 = 0;
				for(int i=0;i<4;i++)
				{
					if(strength[i]>0) { non11 = (i^1)+1; break; } 
				}
				if(non11!=on11)
				{
					addBlockToSet(new CraftrBlock(x,y,blockData[0],non11,blockData[2],blockData[3]));
					if(non11>0 && non11<5)
					{
						CraftrBlock newBlock = new CraftrBlock(x+xMovement[non11-1],y+yMovement[non11-1],surrBlockPre[non11-1]);
						if(blockData[2]!=0) newBlock.setChar(0xFF&(int)blockData[2]);
						if(blockData[3]!=0) newBlock.setColor(0xFF&(int)blockData[3]);
						addBlockToSet(newBlock);
					}
				}
				break;
			default:
				break;
		}
	}
}
