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
 * A pipe part's slider screen. A throttle valve's (Baughn 2026-10-26: a manual valve with a slider): how far open,
 * 0-100%. A safety valve's (Baughn 2026-10-27): its set pressure, SafetyValvePart.MIN_BAR..MAX_BAR on a log scale.
 * Opened by the server (Eln.packetValveScreen); the slider sends each value back as the voxel node's part packet
 * (VoxelNode.PART_PACKET): 0..1 open, or bar.
 */
public class ThrottleValveScreen extends Screen {
    final BlockPos pos;
    final int index, kind;
    /** The slider's position (0..1). */
    double setting;

    ThrottleValveScreen(BlockPos pos, int index, int kind, double value) {
        super(Component.literal(kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY ? "Safety Valve" : "Throttle Valve"));
        this.pos = pos;
        this.index = index;
        this.kind = kind;
        this.setting = kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY ? toSlider(value) : value;
    }

    static final double LO = mods.eln.node.voxel.SafetyValvePart.MIN_BAR, HI = mods.eln.node.voxel.SafetyValvePart.MAX_BAR;

    static double toSlider(double bar) {
        return Math.max(0, Math.min(1, Math.log(bar / LO) / Math.log(HI / LO)));
    }

    /** A safety valve's set pressure at a slider position: log scale, to half a bar under 20 bar, whole bars over. */
    static double toBar(double v) {
        double b = LO * Math.pow(HI / LO, v);
        return b < 20 ? Math.round(b * 2) / 2.0 : Math.round(b);
    }

    static void open(DataInputStream in, Level level) throws IOException {
        BlockPos pos = new BlockPos(in.readInt(), in.readInt(), in.readInt());
        int index = in.readByte();
        double value = in.readFloat();
        int kind = in.readByte();
        Minecraft.getInstance().gui.setScreen(new ThrottleValveScreen(pos, index, kind, value));
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
                send(kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY ? toBar(value) : value);
            }
        });
    }

    Component label(double v) {
        if (kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY) return Component.literal(String.format("Lifts at: %.1f bar", toBar(v)));
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
        g.centeredText(font, Component.literal(kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY
            ? "Opens over this pressure (absolute), wide open 10% above" : "Shut at 0%, fully open at 100%"), width / 2, height / 2 + 16, 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
