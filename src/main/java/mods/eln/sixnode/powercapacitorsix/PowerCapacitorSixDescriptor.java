package mods.eln.sixnode.powercapacitorsix;

import mods.eln.gl.GL11;


import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.misc.series.ISerie;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.powerprimitives.PowerCapacitorCore;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;

public class PowerCapacitorSixDescriptor extends SixNodeDescriptor implements PowerCapacitorCore.Spec {

    private Obj3D obj;

    private Obj3DPart CapacitorCore;
    private Obj3DPart CapacitorCables;
    private Obj3DPart Base;

    ISerie serie;
    public double dischargeTao;

    public PowerCapacitorSixDescriptor(String name,
                                       Obj3D obj,
                                       ISerie serie,
                                       double dischargeTao) {
        super(name, PowerCapacitorSixElement.class, null);
        this.serie = serie;
        this.dischargeTao = dischargeTao;
        this.obj = obj;
        if (obj != null) {
            CapacitorCables = obj.getPart("CapacitorCables");
            CapacitorCore = obj.getPart("CapacitorCore");
            Base = obj.getPart("Base");
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public ISerie capacitanceSerie() {
        return serie;
    }

    /** 1 uF with an empty slot (the transparent node's is 0 F). */
    @Override
    public double emptyCapacitance() {
        return 1e-6;
    }

    @Override
    public double dischargeTao() {
        return dischargeTao;
    }

    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    void draw() {
        if (null != Base) Base.draw();
        if (null != CapacitorCables) CapacitorCables.draw();
        if (null != CapacitorCore) CapacitorCore.draw();
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
        draw();

    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).left();
    }

    /** Small: placed anywhere on a face of a voxel block, in line on a cable, its leads shortened (Baughn, batch 3). */
    @Override
    public boolean placeAnywhere() {
        return true;
    }

    @Override
    public void drawPlaced(Direction side, LRDU front) {
        LRDU turn = modelTurn(side, front);
        if (turn != null) turn.glRotateOnX();
        drawShort();
    }

    @Override
    public LRDU modelTurn(Direction side, LRDU front) {
        return front.getNextClockwise();   // (its render turns it a quarter more)
    }

    @Override
    protected float[] bodyBounds() {
        return inlineBounds(2, new Obj3DPart[]{CapacitorCore}, Base, CapacitorCore);
    }

    /** Off centre: the core, its base and leads shortened to its pins. */
    void drawShort() {
        drawStretched(2, SHORT_LEADS, Base, CapacitorCables);
        if (null != CapacitorCore) CapacitorCore.draw();
    }
}
