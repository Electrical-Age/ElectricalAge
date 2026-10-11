package mods.eln.transparentnode.powerinductor;



import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.misc.Utils;
import mods.eln.Eln;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.series.ISerie;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sim.mna.misc.MnaConst;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class PowerInductorDescriptor extends TransparentNodeDescriptor {

    private Obj3D obj;

    public PowerInductorDescriptor(
        String name,
        Obj3D obj,
        ISerie serie

    ) {
        super(name, PowerInductorElement.class, null);
        this.serie = serie;
        this.obj = obj;
        if (obj != null) {

        }

    }

    ISerie serie;

    public double getlValue(int cableCount) {
        if (cableCount == 0) return 0;
        return serie.getValue(cableCount - 1);
    }

    public double getlValue(Container inventory) {
        ItemStack core = inventory.getItem(PowerInductorContainer.cableId);
        if (Utils.isEmpty(core))
            return getlValue(0);
        else
            return getlValue(core.getCount());
    }

    public double getRsValue(Container inventory) {
        ItemStack core = inventory.getItem(PowerInductorContainer.coreId);

        if (Utils.isEmpty(core)) return MnaConst.highImpedance;
        FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

        double coreFactor = coreDescriptor.cableMultiplicator;

        return ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs * coreFactor;
    }

    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        //Data.addEnergy(newItemStack());
    }

    void draw() {

    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return false; // its draw() is empty (an invisible block in 1.12 too): the flat icon everywhere
    }

    @Override
    public void drawItem(boolean entity) {
        draw();
    }
}
