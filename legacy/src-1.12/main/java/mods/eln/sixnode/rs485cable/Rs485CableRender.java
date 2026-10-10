package mods.eln.sixnode.rs485cable;

import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.UtilsClient;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import org.lwjgl.opengl.GL11;

public class Rs485CableRender extends SixNodeElementRender {

    Rs485CableDescriptor descriptor;

    public Rs485CableRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
        super(tileEntity, side, descriptor);
        this.descriptor = (Rs485CableDescriptor) descriptor;
    }

    @Override
    public void draw() {
        UtilsClient.bindTexture(descriptor.render.cableTexture);
        glListCall();

        GL11.glColor3f(1f, 1f, 1f);
    }

    @Override
    public void glListDraw() {
        CableRender.drawCable(descriptor.render, connectedSide, CableRender.connectionType(this, side));
        CableRender.drawNode(descriptor.render, connectedSide, CableRender.connectionType(this, side));
    }

    @Override
    public boolean glListEnable() {
        return true;
    }

    @Override
    public CableRenderDescriptor getCableRender(LRDU lrdu) {
        return descriptor.render;
    }
}
