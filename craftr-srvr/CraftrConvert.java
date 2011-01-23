public final class CraftrConvert
{
	public CraftrConvert()
	{
	
	}
	
	public static final byte[] shortArray(short i)
	{
		byte[] out = new byte[2];
		out[0] = (byte)(i&255);
		out[1] = (byte)((i>>8)&255);
		return out;
	}
	
	public static final byte[] intArray(short i)
	{
		byte[] out = new byte[4];
		out[0] = (byte)(i&255);
		out[1] = (byte)((i>>8)&255);
		out[2] = (byte)((i>>16)&255);
		out[3] = (byte)((i>>24)&255);
		return out;
	}
	
	public static final short arrShort(byte[] i)
	{
		return (short)(i[0] | (i[1]<<8));
	}
	
	public static final int arrInt(byte[] i)
	{
		return i[0] | (i[1]<<8) | (i[2]<<16) + (i[3]<<24);
	}

}