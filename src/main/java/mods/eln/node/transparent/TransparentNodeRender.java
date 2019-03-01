package mods.eln.node.transparent;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class TransparentNodeRender extends TileEntitySpecialRenderer {
    @Override
    public void render(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        //Utils.println("delta T : " + var8);
        TransparentNodeEntity tileEntity = (TransparentNodeEntity) te;
        if (tileEntity.elementRender == null) return;
        //Utils.glDefaultColor();
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + .5F, (float) y + .5F, (float) z + .5F);
        //tileEntity.elementRender.front.glRotateXnRef();
        tileEntity.elementRender.draw();
        GL11.glPopMatrix();
        //Utils.glDefaultColor();

    }


}
