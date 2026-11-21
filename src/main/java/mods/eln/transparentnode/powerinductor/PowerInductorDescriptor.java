package mods.eln.transparentnode.powerinductor;



import mods.eln.misc.Obj3D;
import mods.eln.misc.series.ISerie;
import mods.eln.powerprimitives.PowerInductorCore;
import mods.eln.node.transparent.TransparentNodeDescriptor;

public class PowerInductorDescriptor extends TransparentNodeDescriptor implements PowerInductorCore.Spec {

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

    @Override
    public ISerie inductanceSerie() {
        return serie;
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
