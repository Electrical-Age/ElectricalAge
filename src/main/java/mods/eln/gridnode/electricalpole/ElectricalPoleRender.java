package mods.eln.gridnode.electricalpole;

import mods.eln.Eln;
import mods.eln.cable.CableRenderType;
import mods.eln.gl.GL11;
import mods.eln.gridnode.GridRender;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.SlewLimiter;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeEntity;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * A utility pole: the pole turned to its angle with what's fitted (transformer, anchor), the streetlights square to the
 * world (each on its side, its lens bright while lit), the low side's cable at the foot, and the transformer's hum by
 * its load.
 */
public class ElectricalPoleRender extends GridRender {

    CableRenderType cableRenderType;
    LRDUMask eConn = new LRDUMask();

    private final ElectricalPoleDescriptor descriptor;
    private final SlewLimiter load = new SlewLimiter(0.5f);
    /** What the server says is fitted: the transformer's low tier (0: none), the lamps, lit, the anchor. */
    int transformerTier;
    int lampMask;
    boolean lit, anchor;
    int lineTier = 4;
    float lineU, lineP;
    private float angle;

    public ElectricalPoleRender(TransparentNodeEntity entity, final TransparentNodeDescriptor descriptor) {
        super(entity, descriptor);
        this.descriptor = (ElectricalPoleDescriptor) descriptor;
        addLoopedSound(new mods.eln.sound.LoopedSound("eln:Transformer", coordonate(), mods.eln.sound.LoopedSound.Attenuation.LINEAR) {
            @Override
            public float getVolume() {
                float min = ElectricalPoleRender.this.descriptor.minimalLoadToHum;
                if (transformerTier != 0 && load.getPosition() > min) return 0.05f * (load.getPosition() - min) / (1 - min);
                return 0f;
            }
        });
    }

    /** Streetlight k (0 north .. 3 west): the lamp model points to -x (west), so turn it to its side. */
    static final float[] LAMP_YAW = {-90, 180, 90, 0};

    @Override
    public void draw() {
        drawPoleAndCatenaries();
        for (int k = 0; k < 4; k++) {
            if ((lampMask & (1 << k)) == 0 || descriptor.lamp == null) continue;
            GL11.glPushMatrix();
            GL11.glRotatef(LAMP_YAW[k], 0, 1, 0);
            GL11.glTranslatef(0, 4.0f, 0);   // (above the cross-arm and its insulators, clear of the spans: Baughn)
            descriptor.lamp.draw();
            if (descriptor.lens != null) {
                if (lit) mods.eln.misc.UtilsClient.disableLight();
                descriptor.lens.draw();
                if (lit) mods.eln.misc.UtilsClient.enableLight();
            }
            GL11.glPopMatrix();
        }
        if (transformerTier != 0) cableRenderType = drawCable(front.down(), Eln.stdCableRender3200V, eConn, cableRenderType);
    }

    void drawPoleAndCatenaries() {
        descriptor.draw(angle, transformerTier != 0, anchor);
        drawCatenaries();
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        angle = idealRenderingAngle();
        eConn.deserialize(stream);
        cableRenderType = null;
        try {
            load.setTarget(stream.readFloat());
            transformerTier = stream.readByte();
            int b = stream.readByte();
            lampMask = b & 15;
            lit = (b & 16) != 0;
            anchor = (b & 32) != 0;
            lineTier = stream.readByte();
            lineU = stream.readFloat();
            lineP = stream.readFloat();
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
