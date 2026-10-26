package mods.eln.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A throttle valve's screen (Baughn 2026-10-26: a manual valve with a slider): how far open, 0-100%. Opened by the
 * server (Eln.packetValveScreen); the slider sends each setting back as the voxel node's part packet
 * (VoxelNode.PART_PACKET).
 */
public class ThrottleValveScreen extends Screen {
    final BlockPos pos;
    final int index;
    double setting;

    ThrottleValveScreen(BlockPos pos, int index, double setting) {
        super(Component.literal("Throttle Valve"));
        this.pos = pos;
        this.index = index;
        this.setting = setting;
    }

    static void open(DataInputStream in, Level level) throws IOException {
        BlockPos pos = new BlockPos(in.readInt(), in.readInt(), in.readInt());
        int index = in.readByte();
        double setting = in.readFloat();
        Minecraft.getInstance().gui.setScreen(new ThrottleValveScreen(pos, index, setting));
    }

    @Override
    protected void init() {
        int w = 200;
        addRenderableWidget(new AbstractSliderButton(width / 2 - w / 2, height / 2 - 10, w, 20, label(setting), setting) {
            @Override
            protected void updateMessage() {
                setMessage(label(value));
            }

            @Override
            protected void applyValue() {
                setting = value;
                send(value);
            }
        });
    }

    static Component label(double v) {
        return Component.literal(String.format("Open: %.0f%%", v * 100));
    }

    void send(double v) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getBlockEntity(pos) instanceof mods.eln.node.NodeBlockEntity be)) return;
        try {
            var bos = new ByteArrayOutputStream();
            var out = new DataOutputStream(bos);
            be.preparePacketForServer(out);
            out.writeByte(mods.eln.node.voxel.VoxelNode.PART_PACKET);
            out.writeByte(index);
            out.writeFloat((float) v);
            be.sendPacketToServer(bos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractRenderState(g, mouseX, mouseY, partial);
        g.centeredText(font, title, width / 2, height / 2 - 30, 0xFFFFFFFF);
        g.centeredText(font, Component.literal("Shut at 0%, fully open at 100%"), width / 2, height / 2 + 16, 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
