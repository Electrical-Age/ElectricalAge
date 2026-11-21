package mods.eln.sixnode.powercapacitorsix;

import mods.eln.gl.GL11;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.series.ISerie;
import mods.eln.powerprimitives.PowerCapacitorCore;
import mods.eln.powerprimitives.PowerPrimitiveSixDescriptor;

public class PowerCapacitorSixDescriptor extends PowerPrimitiveSixDescriptor implements PowerCapacitorCore.Spec {

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
        super(name, PowerCapacitorSixElement.class);
        this.serie = serie;
        this.dischargeTao = dischargeTao;
        this.obj = obj;
        if (obj != null) {
            CapacitorCables = obj.getPart("CapacitorCables");
            CapacitorCore = obj.getPart("CapacitorCore");
            Base = obj.getPart("Base");
        }
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

    @Override
    protected void draw() {
        if (null != Base) Base.draw();
        if (null != CapacitorCables) CapacitorCables.draw();
        if (null != CapacitorCore) CapacitorCore.draw();
    }

    @Override
    protected void turnToFront(LRDU front) {
        GL11.glRotatef(90, 1, 0, 0);
        front.glRotateOnX();
    }

    @Override
    public LRDU modelTurn(Direction side, LRDU front) {
        return front.getNextClockwise();   // (its render turns it a quarter more)
    }

    @Override
    protected float[] bodyBounds() {
        return inlineBounds(2, new Obj3DPart[]{CapacitorCore}, Base, CapacitorCore);
    }

    @Override
    protected void drawShort() {
        drawStretched(2, SHORT_LEADS, Base, CapacitorCables);
        if (null != CapacitorCore) CapacitorCore.draw();
    }
}
