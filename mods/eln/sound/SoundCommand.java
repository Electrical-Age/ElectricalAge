package mods.eln.sound;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import mods.eln.misc.Coordonate;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class SoundCommand {


	public SoundCommand() {

	}

	public SoundCommand(String track) {
		this.track = track;
		mediumRange();
	}
	public SoundCommand(String track,double trackLength) {
		this.track = track;
		this.trackLength = trackLength;
		mediumRange();
	}
	
	public SoundCommand(SoundTrack s) {
		track = s.track;
		volume = s.volume;
		pitch = s.pitch;
		rangeNominal = s.rangeNominal;
		rangeMax = s.rangeMax;
		blockFactor = s.blockFactor;
		uuid = (ArrayList<Integer>) s.uuid.clone();
	}

	World world;
	double x, y, z;
	String track;
	double trackLength;
	float volume = 1, pitch = 1;
	float rangeNominal, rangeMax, blockFactor;
	ArrayList<Integer> uuid = new ArrayList<Integer>();

	public SoundCommand copy(){
		SoundCommand c = new SoundCommand();
		c.world = world;
		c.x = x;
		c.y = y;
		c.z = z;
		c.track = track;
		c.trackLength = trackLength;
		c.volume = volume;
		c.pitch = pitch;
		c.rangeNominal = rangeNominal;
		c.rangeMax = rangeMax;
		c.blockFactor = blockFactor;
		c.uuid = (ArrayList<Integer>) uuid.clone();
		return c;
	}

	
	public void play() {
		if (world.isRemote)
			SoundClient.play(this);
		else
			SoundServer.play(this);
	}

	public void set(Coordonate c) {
		world = c.world();
		x = c.x + 0.5;
		y = c.y + 0.5;
		z = c.z + 0.5;
	}
	public SoundCommand set(TileEntity c) {
		world = c.getWorldObj();
		x = c.xCoord + 0.5;
		y = c.yCoord + 0.5;
		z = c.zCoord + 0.5;
		mediumRange();
		return this;
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

	public SoundCommand mediumRange() {
		rangeNominal = 4;
		rangeMax = 16;
		blockFactor = 1;
		return this;
	}

	public SoundCommand smallRange() {
		rangeNominal = 2;
		rangeMax = 8;
		blockFactor = 3;
		return this;
	}

	public SoundCommand longRange() {
		rangeNominal = 8;
		rangeMax = 48;
		blockFactor = 0.5f;
		return this;
	}

	public SoundCommand mulVolume(float volume, float pitch) {
		this.volume *= volume;
		this.pitch *= pitch;
		return this;
	}

	
	public SoundCommand addUuid(int uuid){
		this.uuid.add(uuid);
		return this;
	}
	public static SoundCommand fromStream(DataInputStream stream, World w) throws IOException {
		SoundCommand p = new SoundCommand();
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
		p.uuid = new ArrayList<Integer>();
		for(int idx = stream.readByte();idx != 0;idx--){
			p.addUuid(stream.readInt());
		}
		return p;
	}

	public void writeTo(DataOutputStream stream) throws IOException {
		stream.writeInt((int) (x * 8));
		stream.writeInt((int) (y * 8));
		stream.writeInt((int) (z * 8));

		stream.writeUTF(track);
		stream.writeFloat(volume);
		stream.writeFloat(pitch);
		stream.writeFloat(rangeNominal);
		stream.writeFloat(rangeMax);
		stream.writeFloat(blockFactor);
		stream.writeByte(uuid.size());
		for(Integer i : uuid){
			stream.writeInt(i);
		}
	}

	public SoundCommand mulVolume(double volume) {
		this.volume *= volume;
		return this;
	}
}
