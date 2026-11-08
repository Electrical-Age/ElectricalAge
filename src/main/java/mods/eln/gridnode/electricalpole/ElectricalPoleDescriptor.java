package mods.eln.gridnode.electricalpole;

import mods.eln.gridnode.GridDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;

/**
 * A utility pole (Baughn 2026-11-08: one modular pole; see ElectricalPoleElement). Its model's transformer, its drop
 * cables and the foot's socket are drawn only with a transformer fitted; the streetlights and the stasis anchor come from
 * the polegear model. includeTransformer marks the old "Utility Pole w/DC-DC Converter", which now places a pole with a
 * transformer fitted.
 */
public class ElectricalPoleDescriptor extends GridDescriptor {
    public final float minimalLoadToHum = 0.2f;
    public final boolean includeTransformer;
    final Obj3D.Obj3DPart transformer, cables, foot;
    /** The polegear model's parts (null where the model is missing, as in tests without assets). */
    final Obj3D.Obj3DPart lamp, lens, anchor;

    public ElectricalPoleDescriptor(String name, Obj3D obj, Obj3D gear, String cableTexture, ElectricalCableDescriptor cableDescriptor,
                                    boolean includeTransformer) {
        super(name, obj, ElectricalPoleElement.class, null, cableTexture, cableDescriptor);
        this.includeTransformer = includeTransformer;
        transformer = obj.getPart("transformer");
        cables = obj.getPart("cables");
        foot = obj.getPart("foot");
        lamp = gear != null ? gear.getPart("lamp") : null;
        lens = gear != null ? gear.getPart("lens") : null;
        anchor = gear != null ? gear.getPart("anchor") : null;

        setDefaultIcon(name.replaceAll("/", ""));
        voltageLevelColor = VoltageLevelColor.Grid;
    }

    /** The pole turned to its angle, with what's fitted (the item: a transformer pole shows its transformer). */
    void draw(float angle, boolean withTransformer, boolean withAnchor) {
        mods.eln.gl.GL11.glPushMatrix();
        mods.eln.gl.GL11.glRotatef(angle, 0, 1, 0);
        for (Obj3D.Obj3DPart part : rotating_parts) part.draw();
        if (withTransformer) {
            if (transformer != null) transformer.draw();
            if (cables != null) cables.draw();
        }
        if (withAnchor && anchor != null) anchor.draw();
        mods.eln.gl.GL11.glPopMatrix();
        if (withTransformer && foot != null) foot.draw();
    }

    @Override
    public void draw(float idealRenderingAngle) {
        draw(idealRenderingAngle, includeTransformer, false);
    }
}
