package mods.eln.node.transparent;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class TransparentNodeRender extends TileEntitySpecialRenderer<TransparentNodeEntity> {
    @Override
    public void render(TransparentNodeEntity entity, double x, double y, double z, float var8, int destroyStage, float alpha) {

        //Utils.println("delta T : " + var8);
        TransparentNodeEntity tileEntity = (TransparentNodeEntity) entity;
        if (tileEntity.elementRender == null) return;
        //Utils.glDefaultColor();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT
            | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT); // EA draws raw GL11; keep GlStateManager's cache true (no texture bit: EA binds via TextureManager)
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + .5F, (float) y + .5F, (float) z + .5F);
        //tileEntity.elementRender.front.glRotateXnRef();
        tileEntity.elementRender.draw();
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        //Utils.glDefaultColor();

    }


}
