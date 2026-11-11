package mods.eln.oil;

import mods.eln.Eln;
import mods.eln.misc.Obj3D;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Pipe Mixer (Baughn 2026-11-11): a signal-controlled pipe T that works as two steerable check valves, not a
 * metering valve. Inlet A on its left flank, inlet B on its right, the outlet out of its back; a signal on its front
 * (0-5 V) opens A by s and B by 1 - s (no signal: half each). Each inlet is a check valve: nothing flows back into it.
 * What it mixes follows the pressures on both inlets and against its outlet, so a set mix takes pressure math or
 * feedback from the outlet's contents. Model: mods.eln.cad.models.PipeMixer.
 */
public class PipeMixerDescriptor extends TransparentNodeDescriptor {
    final Obj3D obj;

    public PipeMixerDescriptor(String name, String model) {
        super(name, PipeMixerElement.class, PipeMixerRender.class);
        this.obj = Eln.obj.getObj(model);
        cableTerminals = mods.eln.cable.CableTerminals.FRONT;
    }

    void draw() {
        if (obj == null) return;
        var m = obj.getPart("main");
        if (m != null) m.draw();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addInformation(ItemStack stack, net.minecraft.world.entity.player.Player player, List list, boolean adv) {
        super.addInformation(stack, player, list, adv);
        list.add("Two inlets (left A, right B), one outlet (back): two check valves steered by one signal.");
        list.add("A signal on its front opens A by it and B by the rest (no signal: half each).");
        list.add("The mix follows the pressures: set it by calculation, or by watching what comes out.");
    }
}
