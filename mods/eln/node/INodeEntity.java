package mods.eln.node;

import java.io.DataInputStream;

public interface INodeEntity {
	String getNodeUuid();
	void serverPublishUnserialize(DataInputStream stream);
	void serverPacketUnserialize(DataInputStream stream);
}
