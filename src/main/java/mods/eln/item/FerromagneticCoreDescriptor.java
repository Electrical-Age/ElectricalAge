package mods.eln.item;

import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sim.ElectricalLoad;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class FerromagneticCoreDescriptor extends GenericItemUsingDamageDescriptorUpgrade {

    public double cableMultiplicator;
    /**
     * The most volts a turn of a winding on it may carry before it saturates (Baughn 2026-11-06: converters like real
     * transformers): 230 V needs 58 turns on a cheap core, 15 on an optimal one.
     */
    public double voltsPerTurn = 16;
    /** How much winding it holds: turns x mm² summed over both windings (Wires stage 4; set per core). */
    public double window = 800;
    public Obj3DPart feroPart;
    Obj3D obj;

    public FerromagneticCoreDescriptor(String name, Obj3D obj, double cableMultiplicator) {
        super(name);
        this.obj = obj;
        if (obj != null) {
            feroPart = obj.getPart("fero");
        }
        this.cableMultiplicator = cableMultiplicator;
    }

    public void applyTo(ElectricalLoad load) {
        load.setRs(load.getRs() * cableMultiplicator);
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Cable loss factor: %1$", cableMultiplicator));
        list.add(tr("Up to %1$ V per turn", mods.eln.misc.Utils.plotValue(voltsPerTurn)));
        list.add(tr("Holds %1$ mm² turns of winding", (int) window));
    }
}
