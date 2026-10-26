package mods.eln.client;

import mods.eln.Eln;
import mods.eln.misc.Dimensions;
import mods.eln.misc.Utils;
import mods.eln.node.INodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Client side of EA's byte protocol (server -> client), 1.12 ClientPacketHandler. Node publishes and node packets are
 * handled; sounds (R4), local GUIs (P4b), uuid kills and shared config come later.
 */
public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void packetRx(byte[] data) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            Level level = mc.level;
            if (level == null) return;
            DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data));
            try {
                switch (stream.readByte()) {
                    case Eln.packetNodeSingleSerialized -> forNode(stream, level, true);
                    case Eln.packetForClientNode -> forNode(stream, level, false);
                    case Eln.packetPlaySound -> {
                        if (stream.readInt() == Dimensions.id(level))
                            mods.eln.sound.SoundClient.play(mods.eln.sound.SoundCommand.fromStream(stream, level));
                    }
                    case Eln.packetKitPreview -> mods.eln.client.render.KitOutline.read(stream);
                    case Eln.packetDevicePreview -> mods.eln.client.render.DevicePreviewRender.read(stream);
                    case Eln.packetValveScreen -> mods.eln.client.ThrottleValveScreen.open(stream, level);
                    default -> {
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /** publish = the node's publish data (serverPublishUnserialize), else a node packet (serverPacketUnserialize). */
    static void forNode(DataInputStream stream, Level level, boolean publish) throws IOException {
        int x = stream.readInt(), y = stream.readInt(), z = stream.readInt(), dim = stream.readInt();
        if (Dimensions.id(level) != dim) return;
        BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
        if (!(be instanceof INodeEntity node)) {
            Utils.println("No node found for " + x + " " + y + " " + z);
            return;
        }
        if (!node.getNodeUuid().equals(stream.readUTF())) {
            Utils.println("Wrong node UUID warning");
            return;
        }
        if (publish) node.serverPublishUnserialize(stream);
        else node.serverPacketUnserialize(stream);
    }
}
