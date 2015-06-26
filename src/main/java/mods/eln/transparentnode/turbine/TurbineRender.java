package mods.eln.transparentnode.turbine;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.cable.CableRenderType;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.LRDUMask;
import mods.eln.misc.Utils;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementInventory;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import org.lwjgl.opengl.GL11;

import java.io.DataInputStream;


public class TurbineRender extends TransparentNodeElementRender {

    TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(1, 64, this);

    TurbineDescriptor descriptor;

    private CableRenderType connectionType;

    public TurbineRender(TransparentNodeEntity tileEntity, TransparentNodeDescriptor descriptor) {
        super(tileEntity, descriptor);
        this.descriptor = (TurbineDescriptor) descriptor;
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
            if (eConn.get(lrdu) == false) continue;
            if (lrdu != front.down().getLRDUGoingTo(front) && lrdu.inverse() != front.down().getLRDUGoingTo(front))
                continue;
            maskTemp.set(1 << lrdu.toInt());
            CableRender.drawCable(descriptor.eRender, maskTemp, connectionType);
        }
    }

    boolean cableRefresh;


    @Override
    public CableRenderDescriptor getCableRender(Direction side, LRDU lrdu) {
        if (lrdu == lrdu.Down) {
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
    }


    LRDUMask eConn = new LRDUMask(), maskTemp = new LRDUMask();
}
