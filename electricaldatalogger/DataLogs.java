package mods.eln.electricaldatalogger;

import org.lwjgl.opengl.GL11;

import mods.eln.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;


public class DataLogs implements INBTTReady{
	public DataLogs(int sizeMax) {
		log = new byte[sizeMax];
		this.sizeMax = sizeMax;
		size = 0;
	}
	byte[] log;
	int sizeMax,size;
	
	void write(byte data)
	{
		int idx;
		if(size != sizeMax)
		{
			size++;
		}
		if(size != sizeMax)
			idx = size;
		else
			idx = size - 1;
		
		while(idx > 0)
		{
			log[idx] = log[idx - 1];
			idx--;
		}
		log[0] = data;
	}
	
	void reset()
	{
		size = 0;
	}
	int size()
	{
		return size;
	}
	byte read(int idx)
	{
		return log[idx];
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		byte[] cpy = nbt.getByteArray(str + "log");
		for(int idx = 0;idx < cpy.length;idx++)
		{
			write(cpy[cpy.length - 1 - idx]);
		}
		size = cpy.length;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setByteArray(str + "log",copyLog());
	}
	
	public byte[] copyLog()
	{
		byte[] cpy = new byte[size];
		for(int idx = 0;idx < size;idx++)
		{
			cpy[idx] = read(idx);
		}
		return cpy;
	}
	
	
	@Override
	public String toString() {
		String str = "";
		for(int idx = 0;idx< size;idx++)
		{
			str += ((int)read(idx) + 128) + " ";
		}

		return str;
	}
	
	void draw()
	{
		draw(log,size);
	}
	
	static void draw(byte []value,int size)
	{

		if(size < 2) return;
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		float dx = 1f/(size - 1);
		GL11.glBegin(GL11.GL_LINE_STRIP);
			for(int idx = 0;idx < size;idx++)
			{
				GL11.glVertex2f(1f - dx * idx,1f - ((int)value[idx] + 128) / 255f);
			}
		GL11.glEnd();
		GL11.glBegin(GL11.GL_LINE_STRIP);
			GL11.glVertex2f(0f,0f);
			GL11.glVertex2f(0f,1f);
			GL11.glVertex2f(1f,1f);
		GL11.glEnd();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}
}
/*

package mods.eln.electricaldatalogger;

import org.lwjgl.opengl.GL11;

import mods.eln.INBTTReady;
import net.minecraft.nbt.NBTTagCompound;


public class DataLogs implements INBTTReady{
	public DataLogs(int sizeMax) {
		log = new byte[sizeMax];
		this.sizeMax = sizeMax;
		lastest = sizeMax - 1;
		oldest = 0;
		size = 0;
	}
	byte[] log;
	int sizeMax,lastest,oldest,size;
	
	void write(byte data)
	{
		lastest++;
		if(lastest == sizeMax) lastest = 0;
		log[lastest] = data;
		if(size == sizeMax)
		{ 
			oldest++;
			if(oldest == sizeMax) oldest = 0;
		}
		else
		{
			size++;
		}
	}
	
	void reset()
	{
		lastest = sizeMax - 1;
		oldest = 0;
		size = 0;
	}
	int size()
	{
		return size;
	}
	byte read(int idx)
	{
		if(idx >= sizeMax) return 0;
		if(lastest - idx >= 0)
			return log[lastest - idx];
		else
			return log[sizeMax + (lastest - idx)];
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt, String str) {
		byte[] cpy = nbt.getByteArray(str + "log");
		for(int idx = 0;idx < cpy.length;idx++)
		{
			write(cpy[cpy.length - 1 - idx]);
		}
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt, String str) {
		nbt.setByteArray(str + "log", copyLog());
	}
	
	
	public byte[] copyLog()
	{
		byte[] cpy = new byte[size];
		for(int idx = 0;idx < size;idx++)
		{
			cpy[idx] = read(idx);
		}
		return cpy;
	}
	
	
	@Override
	public String toString() {
		String str = "";
		for(int idx = 0;idx< size;idx++)
		{
			str += ((int)read(idx) + 128) + " ";
		}

		return str;
	}
	
	void draw()
	{
		if(size < 2) return;
		GL11.glLineWidth(1f);
		GL11.glColor4f(1f, 0f, 0f, 1f);
		GL11.glBegin(GL11.GL_LINE_STRIP);
		
		float dx = 1f/(size - 1);
		for(int idx = 0;idx < size;idx++)
		{
			GL11.glVertex2f(1f - dx * idx,1f - (read(idx) + 128) / 255f);
		}
		GL11.glEnd();
	}
}*/