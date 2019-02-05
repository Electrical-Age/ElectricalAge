package mods.eln.gridnode.electricalpole;

import mods.eln.Eln;
import mods.eln.cable.CableRenderType;
import mods.eln.gridnode.GridRender;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.SlewLimiter;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;
import net.minecraft.client.audio.ISound;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Created by svein on 07/08/15.
 */
public class ElectricalPoleRender extends GridRender {

    CableRenderType cableRenderType;
    LRDUMask eConn = new LRDUMask();

    private final ElectricalPoleDescriptor descriptor;
    private final SlewLimiter load = new SlewLimiter(0.5f);

    public ElectricalPoleRender(TransparentNodeEntity entity, final TransparentNodeDescriptor descriptor) {
        super(entity, descriptor);
        this.descriptor = (ElectricalPoleDescriptor) descriptor;

        if (this.descriptor.includeTransformer) {
            addLoopedSound(new LoopedSound("eln:Transformer", coordinate(), ISound.AttenuationType.LINEAR) {
                @Override
                public float getVolume() {
                    if (load.getPosition() > ElectricalPoleRender.this.descriptor.minimalLoadToHum)
                        return 0.05f * (load.getPosition() - ElectricalPoleRender.this.descriptor.minimalLoadToHum) /
                            (1 - ElectricalPoleRender.this.descriptor.minimalLoadToHum);
                    else
                        return 0f;
                }
            });
        }
    }

    @Override
    public void draw() {
        super.draw();
        cableRenderType = drawCable(front.down(), Eln.instance.stdCableRender3200V, eConn, cableRenderType);
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        eConn.deserialize(stream);
        cableRenderType = null;
        try {
            load.setTarget(stream.readFloat());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void refresh(float deltaT) {
        super.refresh(deltaT);
        load.step(deltaT);
    }
}
