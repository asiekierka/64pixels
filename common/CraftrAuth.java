package common;

import java.util.*;
public class CraftrAuth
{
	private byte[] serverKey = new byte[32];
	private String clientKey;
	private byte[] cka;
	private byte[] msg;

	public CraftrAuth(String ck)
	{
		setClientKey(ck);
		genServerKey();
		genMessage();
	}
	
	public void setClientKey(String ck)
	{
		clientKey=ck;
		cka = ck.getBytes();
	}
	
	public void setMessage(String mg)
	{
		msg=mg.getBytes();
	}
	
	public void genMessage()
	{
		msg = new byte[32];
		for (int i=0;i<32;i++)
		{
			msg[i]=(byte)(rand.nextInt(96)+32);
		}
	}

	public String getClientKey()
	{
		return clientKey;
	}
	
	public byte[] getServerKey()
	{
		return serverKey;
	}
	private Random rand = new Random();
	public void setSeed(long seed)
	{
		rand.setSeed(seed);
	}
	public void genServerKey()
	{
		serverKey=new byte[32];
		rand.nextBytes(serverKey);
		for(int i=0;i<32;i++)
		{
			while(serverKey[i]==0 || serverKey[i]==cka[i%cka.length])
			{
				serverKey[i]=(byte)rand.nextInt(256);
			}
		}
	}
	
	public void genData()
	{
		
	}
	public byte[] encrypt()
	{
		// SAX principle!
		byte[] enc = msg.clone();
		for(int i=0;i<enc.length;i++)
		{
			int tmp = 0xFF&(int)msg[i];
			// S is for Scramble
			for(int j=0;j<8;j++)
			{
				int t2 = serverKey[(i+j)%serverKey.length];
				int t3 = serverKey[i%serverKey.length]&7;
				t2=(t2>>t3)&1;
				if(t2==1)
				{
					tmp=(tmp^((serverKey[i%serverKey.length]*324689)&255));
				}
			}
			// A is for Add
			tmp=(tmp+(0xFF&(int)serverKey[i%serverKey.length]))&255;
			// X is for XOR
			tmp=tmp^(0xFF&(int)cka[i%cka.length]);
			enc[i]=(byte)tmp;
			// A is for Add (client time, this one)
			tmp=(tmp+(0xFF&(int)cka[i%cka.length]))&255;
		}
		return enc;
	}
	public byte[] decryptClient(byte[] enc)
	{
		byte[] enc2 = new byte[enc.length];
		for(int i=0;i<enc2.length;i++)
		{
			int tmp = 0xFF&(int)enc[i];
			// un-ADD
			int t1 = 0xFF&(int)cka[i%cka.length];
			if(tmp<t1) tmp+=256;
			tmp-=t1;
			// XOR is the client part!
			tmp=tmp^(0xFF&(int)cka[i%cka.length]);
			enc2[i]=(byte)tmp;
		}
		return enc2;
	}
	public byte[] decryptServer(byte[] dec)
	{
		byte[] dec2=new byte[dec.length];
		for(int i=0;i<dec2.length;i++)
		{
			int tmp = 0xFF&(int)dec[i];
			// un-ADD
			int t1 = 0xFF&(int)serverKey[i%serverKey.length];
			if(tmp<t1) tmp+=256;
			tmp-=t1;
			// un=SCRAMBLE
			for(int j=0;j<8;j++)
			{
				int t2 = serverKey[(i+j)%serverKey.length];
				int t3 = serverKey[i%serverKey.length]&7;
				t2=(t2>>t3)&1;
				if(t2==1)
				{
					tmp=(tmp^((serverKey[i%serverKey.length]*324689)&255));
				}
			}
			dec2[i]=(byte)tmp;
		}
		return dec2;
	}
	
	public boolean testDecrypt(byte[] dec)
	{
		byte[] dec2 = decryptServer(dec);
		int diff = 0;
		for(int i=0;i<dec2.length;i++)
		{
			if(dec2[i]!=msg[i])
			{
				System.out.println("byte " + i + " differs, " + dec2[i] + " " + msg[i]);
				diff++;
			}
		}
		return (diff==0);
	}
	public static void main(String[] args)
	{
		CraftrAuth ta = new CraftrAuth("iAmt4eHArd00reMAN");
		//ta.genServerKey();
		//ta.genMessage();
		byte[] test = ta.encrypt();
		byte[] test2 = ta.decryptServer(ta.decryptClient(test));
		byte[] test3 = ta.getServerKey();
		for(int i=0;i<16;i++) { System.out.print(Integer.toHexString(0xFF&(int)test2[i]) + " "); }
		System.out.println("");
		for(int i=0;i<test3.length;i++) { System.out.print(Integer.toHexString(0xFF&(int)test3[i]) + " "); }
		System.out.println("");
		for(int i=0;i<16;i++) { System.out.print(Integer.toHexString(0xFF&(int)test[i]) + " "); }
		System.out.println("");
		System.out.println("RESULT: " + ta.testDecrypt(ta.decryptClient(test)));
	}
	
}
