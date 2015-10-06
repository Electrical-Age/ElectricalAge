package mods.eln.gridnode.transformer;

import mods.eln.gridnode.GridDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.sound.SoundCommand;

/**
 * Created by svein on 07/08/15.
 */
public class GridTransformerDescriptor extends GridDescriptor {
    public final float minimalLoadToHum = 0.2f;
    public SoundCommand highLoadSound = new SoundCommand("eln:Transformer", 1.6f);

    public GridTransformerDescriptor(String name, Obj3D obj, String cableTexture, ElectricalCableDescriptor cableDescriptor) {
        super(name, obj, GridTransformerElement.class, GridTransformerRender.class, cableTexture, cableDescriptor);
    }

    @Override
    public boolean rotationIsFixed() {
        return true;
    }
}

