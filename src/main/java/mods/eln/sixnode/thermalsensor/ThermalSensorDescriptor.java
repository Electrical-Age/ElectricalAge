package mods.eln.sixnode.thermalsensor;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ThermalSensorDescriptor extends SixNodeDescriptor {

    public boolean temperatureOnly;

    Obj3D obj;
    Obj3DPart main;
    Obj3DPart adapter;

    public ThermalSensorDescriptor(String name,
                                   Obj3D obj,
                                   boolean temperatureOnly) {
        super(name, ThermalSensorElement.class, null);
        this.temperatureOnly = temperatureOnly;
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            adapter = obj.getPart("adapter");
        }
        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(temperatureOnly
            ? tr("Outputs a 0–5 V signal for thermal cable temperature.")
            : tr("Outputs a 0–5 V signal for thermal cable temperature or heat flow."));
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addThermal(newItemStack());
        Data.addSignal(newItemStack());
    }
    void draw(boolean renderAdapter) {
        if (main != null) main.draw();
        if (renderAdapter && adapter != null) adapter.draw();
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }

    /** Small: placed anywhere on a face of a voxel block, at its drawn size (Baughn). */
    @Override
    public boolean placeAnywhere() {
        return true;
    }

    @Override
    public void drawPlaced(mods.eln.misc.Direction side, mods.eln.misc.LRDU front) {
        front.glRotateOnX();
        draw(temperatureOnly);
    }

    @Override
    protected float[] bodyBounds() {
        return boundsOf(main, adapter);
    }
}
