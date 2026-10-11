package mods.eln.sixnode.resistor;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;


import mods.eln.misc.Utils;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.misc.series.ISerie;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Created by svein on 05/08/15.
 */
public class ResistorDescriptor extends SixNodeDescriptor {

    public final boolean isRheostat;
    public double thermalCoolLimit = -100;
    public double thermalWarmLimit = Eln.cableWarmLimit;
    public double thermalMaximalPowerDissipated = 1000;
    public double thermalNominalHeatTime = 120;
    public double thermalConductivityTao = Eln.cableThermalConductionTao;
    public double tempCoef;
    Obj3D.Obj3DPart ResistorBaseExtension, ResistorCore, ResistorTrack, ResistorWiper, Base, Cables;
    ISerie series;
    private Obj3D obj;


    public ResistorDescriptor(String name,
                              Obj3D obj,
                              ISerie series,
                              double tempCoef,
                              boolean isRheostat) {
        super(name, ResistorElement.class, null);
        this.obj = obj;
        this.series = series;
        this.tempCoef = tempCoef;
        this.isRheostat = isRheostat;
        if (obj != null) {
            ResistorBaseExtension = obj.getPart("ResistorBaseExtention");
            ResistorCore = obj.getPart("ResistorCore");
            ResistorTrack = obj.getPart("ResistorTrack");
            ResistorWiper = obj.getPart("ResistorWiper");
            Base = obj.getPart("Base");
            Cables = obj.getPart("CapacitorCables");
        }
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public double getRsValue(Container inventory) {
        ItemStack core = inventory.getItem(ResistorElement.coreSlotId);

        if (Utils.isEmpty(core)) return series.getValue(0);
        return series.getValue(core.getCount());
    }

    @Override
    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    void draw(float wiperPos) {
        //UtilsClient.disableCulling();
        //UtilsClient.disableTexture();
        //GL11.glRotatef(90, 1, 0, 0);


        if (null != Base) Base.draw();
        if (null != ResistorBaseExtension) ResistorBaseExtension.draw();
        if (null != ResistorCore) ResistorCore.draw();
        if (null != Cables) Cables.draw();

        if (isRheostat) {
            final float wiperSpread = 0.238f;
            wiperPos = (wiperPos - 0.5f) * wiperSpread * 2;
            ResistorTrack.draw();
            GL11.glTranslatef(0, 0, wiperPos);
            ResistorWiper.draw();
            // GL11.glTranslatef(-wiperPos, 0, 0);
        }
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glTranslatef(0.0f, 0.0f, -0.2f);
        GL11.glScalef(1.25f, 1.25f, 1.25f);
        GL11.glRotatef(-90.f, 0.f, 1.f, 0.f);
        draw(0);

    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
