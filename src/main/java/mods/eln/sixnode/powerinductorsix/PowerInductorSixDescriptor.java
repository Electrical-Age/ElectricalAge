package mods.eln.sixnode.powerinductorsix;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.series.ISerie;
import mods.eln.powerprimitives.PowerInductorCore;
import mods.eln.powerprimitives.PowerPrimitiveSixDescriptor;

public class PowerInductorSixDescriptor extends PowerPrimitiveSixDescriptor implements PowerInductorCore.Spec {

    private Obj3D obj;
    Obj3DPart InductorBaseExtention, InductorCables, InductorCore, Base;

    ISerie serie;

    public PowerInductorSixDescriptor(String name,
                                      Obj3D obj,
                                      ISerie serie) {
        super(name, PowerInductorSixElement.class);
        this.serie = serie;
        this.obj = obj;
        if (obj != null) {
            InductorBaseExtention = obj.getPart("InductorBaseExtention");
            InductorCables = obj.getPart("InductorCables");
            InductorCore = obj.getPart("InductorCore");
            Base = obj.getPart("Base");
        }
    }

    @Override
    public ISerie inductanceSerie() {
        return serie;
    }

    @Override
    protected void draw() {
        if (null != Base) Base.draw();
        if (null != InductorBaseExtention) InductorBaseExtention.draw();
        if (null != InductorCables) InductorCables.draw();
        if (null != InductorCore) InductorCore.draw();
    }

    @Override
    protected void turnToFront(LRDU front) {
        front.left().glRotateOnX();
    }

    @Override
    public LRDU modelTurn(Direction side, LRDU front) {
        return front.left();
    }

    @Override
    protected float[] bodyBounds() {
        return inlineBounds(2, new Obj3DPart[]{InductorCore}, Base, InductorCore, InductorBaseExtention);
    }

    @Override
    protected void drawShort() {
        drawStretched(2, SHORT_LEADS, Base, InductorCables);
        if (null != InductorBaseExtention) InductorBaseExtention.draw();
        if (null != InductorCore) InductorCore.draw();
    }
}
