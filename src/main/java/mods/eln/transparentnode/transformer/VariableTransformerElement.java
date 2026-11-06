package mods.eln.transparentnode.transformer;

import mods.eln.Eln;
import mods.eln.i18n.I18N;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.nbt.NbtElectricalGateInput;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * The Variable DC-DC Converter (Baughn 2026-11-06): a signal on its front taps the secondary at round(signal x turns),
 * one turn at least. It is always isolated (two sources coupled by TransformerInterSystemProcess), where a ratio is a
 * number set each step, so re-tapping rebuilds no matrix; it re-taps at most every quarter second.
 */
public class VariableTransformerElement extends TransformerElement {
    public final NbtElectricalGateInput tap = new NbtElectricalGateInput("tap");
    /** The secondary turns in use. */
    int tapTurns = 1;
    private int sinceTap;

    public VariableTransformerElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
        super(transparentNode, descriptor);
        isIsolator = true;
        electricalLoadList.add(tap);
        slowProcessList.add(dt -> retap());
    }

    @Override
    boolean canToggleIsolation() {
        return false;
    }

    @Override
    int secondaryTurns() {
        int all = super.secondaryTurns();
        return all == 0 ? 0 : Math.max(1, Math.min(all, tapTurns));
    }

    /** The tap the signal asks for (1..all turns). */
    int wantedTap() {
        int all = super.secondaryTurns();
        return Math.max(1, (int) Math.round(tap.getNormalized() * all));
    }

    void retap() {
        if (sinceTap < 5) sinceTap++;
        int want = wantedTap();
        if (want == tapTurns || sinceTap < 5) return;
        tapTurns = want;
        sinceTap = 0;
        ItemStack p = inventory.getItem(TransformerContainer.primaryCableSlotId);
        if (!Utils.isEmpty(p)) {
            interSystemProcess.setRatio(1.0 * secondaryTurns() / p.getCount());
            transformer.setRatio(1.0 * secondaryTurns() / p.getCount());
        }
        needPublish();
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        if (lrdu == LRDU.Down && side == front) return tap;
        return super.getElectricalLoad(side, lrdu);
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        if (lrdu == LRDU.Down && side == front) return NodeBase.maskElectricalInputGate;
        if (lrdu == LRDU.Down && side == front.back()) return 0;
        return super.getConnectionMask(side, lrdu);
    }

    @Override
    public void readFromNBT(net.minecraft.nbt.CompoundTag nbt) {
        super.readFromNBT(nbt);
        tapTurns = Math.max(1, nbt.getIntOr("tapTurns", 1));
    }

    @Override
    public void writeToNBT(net.minecraft.nbt.CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putInt("tapTurns", tapTurns);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.put(I18N.tr("Tap"), secondaryTurns() + " / " + super.secondaryTurns() + " " + I18N.tr("turns"));
        info.put(I18N.tr("Signal"), Utils.plotVolt("", tap.getU()));
        return info;
    }
}
