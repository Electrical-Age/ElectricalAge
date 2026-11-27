package mods.eln.transparentnode.powercapacitor;


import mods.eln.misc.Obj3D;
import mods.eln.misc.series.ISerie;
import mods.eln.node.transparent.FrontDrawnRender;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.powerprimitives.PowerCapacitorCore;

public class PowerCapacitorDescriptor extends TransparentNodeDescriptor implements PowerCapacitorCore.Spec, FrontDrawnRender.Drawn {

    private Obj3D obj;

    public PowerCapacitorDescriptor(
        String name,
        Obj3D obj,
        ISerie serie,
        double dischargeTao

    ) {
        super(name, PowerCapacitorElement.class, null);
        this.serie = serie;
        this.dischargeTao = dischargeTao;
        this.obj = obj;
        if (obj != null) {

        }

    }

    ISerie serie;
    public double dischargeTao;

    @Override
    public ISerie capacitanceSerie() {
        return serie;
    }

    /** 0 F with an empty slot (the six-node's is 1 uF): its discharge resistor is then dischargeTao / 0, infinite. */
    @Override
    public double emptyCapacitance() {
        return 0;
    }

    @Override
    public double dischargeTao() {
        return dischargeTao;
    }

    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        //Data.addEnergy(newItemStack());
    }

    @Override
    public void draw() {   // (empty: an invisible block in 1.12 too)

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

    @Override
    public void addInformation(net.minecraft.world.item.ItemStack itemStack, net.minecraft.world.entity.player.Player entityPlayer, java.util.List<String> list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(mods.eln.i18n.I18N.tr("Configurable power capacitor block (fit dielectrics to set capacitance)."));
    }
}
