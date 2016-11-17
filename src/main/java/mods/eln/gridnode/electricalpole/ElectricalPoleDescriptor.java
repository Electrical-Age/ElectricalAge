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
    public SoundCommand highLoadSound = new SoundCommand("eln:Transformer", 1.6f);
    public final boolean includeTransformer;

    public ElectricalPoleDescriptor(String name, Obj3D obj, String cableTexture, ElectricalCableDescriptor cableDescriptor, boolean includeTransformer) {
        super(name, obj, ElectricalPoleElement.class, ElectricalPoleRender.class, cableTexture, cableDescriptor);
        this.includeTransformer = includeTransformer;
        if (includeTransformer) {
            rotating_parts.add(obj.getPart("transformer"));
            rotating_parts.add(obj.getPart("cables"));
            static_parts.add(obj.getPart("foot"));
        }
    }
}
