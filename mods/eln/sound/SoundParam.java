package mods.eln.sound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.misc.Coordonate;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class SoundParam {

	public SoundParam(String track, TileEntity c) {
		this.track = track;
		world = c.getWorldObj();
		x = c.xCoord + 0.5;
		y = c.yCoord + 0.5;
		z = c.zCoord + 0.5;
		mediumRange();
	}

	public SoundParam() {

	}

	public SoundParam(String track, Coordonate c) {
		this.track = track;
		set(c);
		mediumRange();
	}

	public SoundParam(String track, Coordonate c, Range range) {
		this.track = track;
		set(c);
		applyRange(range);
	}

	World world;
	double x, y, z;
	String track;
	float volume = 1, pitch = 1;
	float rangeNominal, rangeMax, blockFactor;

	public void set(Coordonate c) {
		world = c.world();
		x = c.x + 0.5;
		y = c.y + 0.5;
		z = c.z + 0.5;
	}

	void applyRange(Range range) {
		switch (range) {
		case Small:
			smallRange();
			break;
		case Far:
			longRange();
			break;
		case Mid:
		default:
			mediumRange();
			break;
		}
	}

	enum Range {
		Small, Mid, Far
	};

	public SoundParam mediumRange() {
		rangeNominal = 4;
		rangeMax = 16;
		blockFactor = 1;
		return this;
	}

	public SoundParam smallRange() {
		rangeNominal = 2;
		rangeMax = 8;
		blockFactor = 3;
		return this;
	}

	public SoundParam longRange() {
		rangeNominal = 8;
		rangeMax = 48;
		blockFactor = 0.5f;
		return this;
	}

	public SoundParam setVolume(float volume, float pitch) {
		this.volume = volume;
		this.pitch = pitch;
		return this;
	}

	public static SoundParam fromStream(DataInputStream stream,World w) throws IOException {
		SoundParam p = new SoundParam();
		p.world = w;
		
		p.x = stream.readInt() / 8.0;
		p.y = stream.readInt() / 8.0;
		p.z = stream.readInt() / 8.0;
		p.track = stream.readUTF();
		p.volume = stream.readFloat();
		p.pitch = stream.readFloat();
		p.rangeNominal = stream.readFloat();
		p.rangeMax = stream.readFloat();
		p.blockFactor = stream.readFloat();
		return p;
	}

	public void writeTo(DataOutputStream stream) throws IOException {
        stream.writeInt((int)(x*8));
        stream.writeInt((int)(y*8));
        stream.writeInt((int)(z*8));
        
       
        stream.writeUTF(track);
        stream.writeFloat(volume);
        stream.writeFloat(pitch);
        stream.writeFloat(rangeNominal);
        stream.writeFloat(rangeMax);
        stream.writeFloat(blockFactor);	
	}
}
