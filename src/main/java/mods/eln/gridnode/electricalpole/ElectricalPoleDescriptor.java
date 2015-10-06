package mods.eln.gridnode.electricalpole;

import mods.eln.gridnode.GridDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;

/**
 * Created by svein on 07/08/15.
 */
public class ElectricalPoleDescriptor extends GridDescriptor {
    public final float minimalLoadToHum = 0.2f;
    final boolean includeTransformer;
    public SoundCommand highLoadSound = new SoundCommand("eln:Transformer", 1.6f);

    private Obj3D.Obj3DPart[] transformerParts;

    public ElectricalPoleDescriptor(String name, Obj3D obj, String cableTexture, ElectricalCableDescriptor cableDescriptor, boolean includeTransformer) {
        super(name, obj, ElectricalPoleElement.class, ElectricalPoleRender.class, cableTexture, cableDescriptor);
        this.includeTransformer = includeTransformer;
        transformerParts = new Obj3D.Obj3DPart[]{
                obj.getPart("transformer"),
                obj.getPart("wire"),
                obj.getPart("cables"),
        };
    }

    @Override
    public void draw() {
        super.draw();
        if (includeTransformer) {
            for (Obj3D.Obj3DPart transformerPart : transformerParts) {
                transformerPart.draw();
            }
        }
    }
}
