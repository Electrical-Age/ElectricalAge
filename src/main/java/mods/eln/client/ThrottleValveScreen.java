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
 * 0-100%. A safety valve's (Baughn 2026-10-27): its set pressure, SafetyValvePart.MIN_BAR..MAX_BAR on a log scale. A
 * pressure valve's (Baughn 2026-10-29): its set point (ValvePart.MIN_BAR..MAX_BAR, log), its mode (relief: holds the
 * pressure before it; reducing: holds the pressure after it) and which way its arrow points.
 * Opened by the server (Eln.packetValveScreen); the slider sends each value back as the voxel node's part packet
 * (VoxelNode.PART_PACKET): 0..1 open, or bar.
 */
public class ThrottleValveScreen extends Screen {
    final BlockPos pos;
    final int index, kind;
    /** The slider's position (0..1). */
    double setting;
    /** A pressure valve's mode (reducing?) and arrow (towards -axis?), as the server sent them; flipped on the buttons. */
    boolean reducing, backwards;

    ThrottleValveScreen(BlockPos pos, int index, int kind, double value) {
        super(Component.literal(kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY ? "Safety Valve"
            : kind == mods.eln.node.voxel.VoxelNode.SCREEN_PRESSURE ? "Pressure Valve" : "Throttle Valve"));
        this.pos = pos;
        this.index = index;
        this.kind = kind;
        if (kind == mods.eln.node.voxel.VoxelNode.SCREEN_PRESSURE) {
            backwards = value >= 2000;
            if (backwards) value -= 2000;
            reducing = value >= 1000;
            if (reducing) value -= 1000;
            this.setting = toSliderP(value);
        } else this.setting = kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY ? toSlider(value) : value;
    }

    static final double LO = mods.eln.node.voxel.SafetyValvePart.MIN_BAR, HI = mods.eln.node.voxel.SafetyValvePart.MAX_BAR;

    static final double PLO = mods.eln.node.voxel.ValvePart.MIN_BAR, PHI = mods.eln.node.voxel.ValvePart.MAX_BAR;

    static double toSliderP(double bar) {
        return Math.max(0, Math.min(1, Math.log(Math.max(PLO, bar) / PLO) / Math.log(PHI / PLO)));
    }

    /** A pressure valve's set point at a slider position: log scale, to 0.05 bar under 2, half a bar under 20, whole bars over. */
    static double toBarP(double v) {
        double b = PLO * Math.pow(PHI / PLO, v);
        return b < 2 ? Math.round(b * 20) / 20.0 : b < 20 ? Math.round(b * 2) / 2.0 : Math.round(b);
    }

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
                send(kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY ? toBar(value) : kind == mods.eln.node.voxel.VoxelNode.SCREEN_PRESSURE ? toBarP(value) : value);
            }
        });
        if (kind == mods.eln.node.voxel.VoxelNode.SCREEN_PRESSURE) {
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(modeLabel(), b -> {
                reducing = !reducing;
                send(mods.eln.node.voxel.VoxelNode.TOGGLE_PRESSURE_MODE);
                b.setMessage(modeLabel());
            }).bounds(width / 2 - w / 2, height / 2 + 30, w, 20).build());
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(Component.literal("Turn its arrow round"), b -> {
                backwards = !backwards;
                send(mods.eln.node.voxel.VoxelNode.FLIP_PRESSURE_ARROW);
            }).bounds(width / 2 - w / 2, height / 2 + 54, w, 20).build());
        }
    }

    Component modeLabel() {
        return Component.literal(reducing ? "Reducing: holds the pressure after it" : "Relief: holds the pressure before it");
    }

    Component label(double v) {
        if (kind == mods.eln.node.voxel.VoxelNode.SCREEN_SAFETY) return Component.literal(String.format("Lifts at: %.1f bar", toBar(v)));
        if (kind == mods.eln.node.voxel.VoxelNode.SCREEN_PRESSURE) return Component.literal(String.format("Set point: %.2f bar", toBarP(v)));
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
            ? "Opens over this pressure (absolute), wide open 10% above"
            : kind == mods.eln.node.voxel.VoxelNode.SCREEN_PRESSURE ? "Absolute; one way, along its arrow; a signal cable in its block sets it instead (0-50 V: 0-150 bar)"
            : "Shut at 0%, fully open at 100%"), width / 2, height / 2 + 16, 0xFFA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
