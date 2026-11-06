package mods.eln.robotics.computer;

import mods.eln.Eln;
import mods.eln.api.Brownout;
import mods.eln.api.CablePosition;
import mods.eln.api.MachineSpec;
import mods.eln.api.RelativeFace;
import mods.eln.api.Tier;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

/**
 * The robotics computer (Baughn 2026-11-05): an RV32 machine with 8 chip slots of 1 MB (RAM or ROM; it starts at
 * 0x400 in slot 1's chip) and two floppy drives, running off the server thread (hw.Runner). 50 V on the back; the
 * RS485 bus pin on the right flank's floor edge, at its right end (left and right facing the flank), as on the servo
 * drive. Its draw follows how busy it is. Model: mods.eln.cad.models.ComputerParts.
 */
public class ComputerDescriptor extends TransparentNodeDescriptor {
    /** Idle (asleep or waiting) and flat-out draw, W. */
    public static final double IDLE_WATTS = 2, RATED_WATTS = 60;
    final Obj3D obj;

    public ComputerDescriptor(String name, String model) {
        super(name, ComputerElement.class, ComputerRender.class);
        obj = Eln.obj.getObj(model);
        voltageLevelColor = mods.eln.sixnode.electricalcable.ElectricalCableDescriptor.voltageLevelColorOf(
            mods.eln.registry.ElnDeviceRegistry.lowVoltageCableDescriptor);
    }

    /**
     * A 50 V consumer that stops on a brownout: the computer then pauses with its RAM kept, and goes on when the line
     * comes back (a sag shouldn't cost a program its state).
     */
    public static MachineSpec spec() {
        return MachineSpec.consumer(Tier.V48).ratedWatts(RATED_WATTS).idleWatts(IDLE_WATTS).onBrownout(Brownout.STOP)
            .power(RelativeFace.BACK)
            .bus(RelativeFace.RIGHT, CablePosition.FLOOR_RIGHT)
            .build();
    }

    void draw(byte status) {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
        var lamp = obj.getPart("lamp");
        if (lamp == null) return;
        // a lamp glows: drawn unlit while on, so it shows at night (AdventurAgent's check)
        boolean on = status == ComputerElement.RUNNING || status == ComputerElement.SLEEPING || status == ComputerElement.HALTED;
        switch (status) {
            case ComputerElement.RUNNING -> mods.eln.gl.GL11.glColor4f(0.35f, 1f, 0.35f, 1f);
            case ComputerElement.SLEEPING -> mods.eln.gl.GL11.glColor4f(0.15f, 0.6f, 0.15f, 1f);
            case ComputerElement.HALTED -> mods.eln.gl.GL11.glColor4f(1f, 0.15f, 0.1f, 1f);
            default -> mods.eln.gl.GL11.glColor4f(0.25f, 0.25f, 0.25f, 1f);
        }
        if (on) mods.eln.misc.UtilsClient.disableLight();
        lamp.draw();
        if (on) mods.eln.misc.UtilsClient.enableLight();
        mods.eln.gl.GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(ComputerElement.OFF);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("A programmable computer: 8 chip slots\n(a ROM in slot 1, RAM after it)\nand two floppy drives.\n50V power on the back.\nRS485 bus pin on the right flank's\nfloor edge, at its right end.").split("\n"));
    }
}
