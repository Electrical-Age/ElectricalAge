package mods.eln.transparentnode.turbine;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.misc.*;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;
import net.minecraft.client.audio.ISound;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;
import java.io.IOException;


public class TurbineRender extends TransparentNodeElementRender {
    private final TurbineDescriptor descriptor;

    private CableRenderType connectionType;
    private final SlewLimiter factorLimiter = new SlewLimiter(0.2f);

    private boolean cableRefresh;
    private final LRDUMask eConn = new LRDUMask();
    private final LRDUMask maskTemp = new LRDUMask();

    public TurbineRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (TurbineDescriptor) descriptor;
        addLoopedSound(new LoopedSound(this.descriptor.soundFile, coordonate(), ISound.AttenuationType.LINEAR) {
            @Override
            public float getVolume() {
                return 0.1f * factorLimiter.getPosition();
            }

            @Override
            public float getPitch() {
                return 0.9f + 0.2f * factorLimiter.getPosition();
            }
        });
    }

    @Override
    public void draw() {
        GL11.glPushMatrix();
        front.glRotateXnRef();
        GL11.glScalef(1.0f, 1.0f, 1.0f);
        descriptor.draw();
        GL11.glPopMatrix();

        if (cableRefresh) {
            cableRefresh = false;
            connectionType = CableRender.connectionType(tileEntity, eConn, front.down());
        }

        glCableTransforme(front.down());
        descriptor.eRender.bindCableTexture();

        for (LRDU lrdu : LRDU.values()) {
            Utils.setGlColorFromDye(connectionType.otherdry[lrdu.toInt()]);
            if (!eConn.get(lrdu)) continue;
            if (lrdu != front.down().getLRDUGoingTo(front) && lrdu.inverse() != front.down().getLRDUGoingTo(front))
                continue;
            maskTemp.set(1 << lrdu.toInt());
            CableRender.drawCable(descriptor.eRender, maskTemp, connectionType);
        }
    }

    @Override
    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        if (lrdu == LRDU.Down) {
            if (side == front) return descriptor.eRender;
            if (side == front.back()) return descriptor.eRender;
        }
        return null;
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        eConn.deserialize(stream);
        cableRefresh = true;
        try {
            float deltaT = stream.readFloat();
            if (deltaT >= 40) {
                factorLimiter.setTarget((float) (deltaT / TurbineRender.this.descriptor.nominalDeltaT));
            } else {
                factorLimiter.setTarget(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void refresh(float deltaT) {
        factorLimiter.step(deltaT);
        super.refresh(deltaT);
    }
}
