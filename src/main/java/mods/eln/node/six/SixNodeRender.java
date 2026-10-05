package mods.eln.node.six;

import mods.eln.misc.Direction;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

/** Plain TESR (not FastTESR: EA draws immediate-mode GL11 / display lists). Bound in ClientProxy.registerRenderers. */
public class SixNodeRender extends TileEntitySpecialRenderer<SixNodeEntity> {
    @Override
    public void render(SixNodeEntity entity, double x, double y, double z, float var8, int destroyStage, float alpha) {
        Minecraft.getMinecraft().profiler.startSection("SixNode");

        SixNodeEntity tileEntity = (SixNodeEntity) entity;


        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT
            | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT); // EA draws raw GL11; keep GlStateManager's cache true (no texture bit: EA binds via TextureManager)
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + .5F, (float) y + .5F, (float) z + .5F);
        /*if(tileEntity.sixNodeCacheMapId >= 0)
		{
			if(SixNodeCacheItem.map[tileEntity.sixNodeCacheMapId] != null)
			{
				UtilsClient.glDefaultColor();
				SixNodeCacheItem.map[tileEntity.sixNodeCacheMapId].draw(entity.getWorld(),entity.getPos().getX(),entity.getPos().getY(),entity.getPos().getZ());
			}
		}*/

        int idx = 0;
        for (SixNodeElementRender render : tileEntity.elementRenderList) {
            if (render != null) {
                UtilsClient.glDefaultColor();
                GL11.glPushMatrix();
                Direction.fromInt(idx).glRotateXnRef();
                GL11.glTranslatef(-0.5F, 0f, 0f);
                render.draw();
                GL11.glPopMatrix();
            }
            idx++;
        }
        GL11.glPopMatrix();
        GL11.glPopAttrib();
        //Utils.glDefaultColor();
        Minecraft.getMinecraft().profiler.endSection();

    }


}
