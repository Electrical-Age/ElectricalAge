package mods.eln.misc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

public class LRDUMask {
	public int mask;
	public LRDUMask()
	{
		mask = 0;
	}
	public LRDUMask(int mask)
	{
		this.mask = mask;
	}
	public static LRDUMask[] array = {new LRDUMask(1),new LRDUMask(2),new LRDUMask(4),new LRDUMask(8)};
	public boolean left(){ return (mask & 1) != 0;}
	public boolean right(){ return (mask & 2) != 0;}
	public boolean down(){ return (mask & 4) != 0;}
	public boolean up(){ return (mask & 8) != 0;}
	
	public void set(int mask)
	{
		this.mask = mask;
	}
	public void set(LRDU lrdu,boolean value)
	{
		if(value)
		{
			this.mask |= (1<<lrdu.dir);	
		}
		else
		{
			this.mask &= ~(1<<lrdu.dir);
		}
	}
	public boolean get(LRDU lrdu)
	{
		return (mask & (1<<lrdu.dir)) != 0;
	}
	
	public void serialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		try {
			stream.writeByte(mask);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void deserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		try {
			set(stream.readByte());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			set(0);
		}
		
	}
}
