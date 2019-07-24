package mods.eln.node.six;

import mods.eln.misc.Direction;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import org.lwjgl.opengl.GL11;

public class SixNodeRender extends TileEntitySpecialRenderer {

    @Override
    public void renderTileEntityFast(TileEntity te, double x, double y, double z, float partialTicks, int destroyStage, float partial, BufferBuilder buffer) {
        Minecraft.getMinecraft().profiler.startSection("SixNode");
        SixNodeEntity tileEntity = (SixNodeEntity) te;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + .5F, (float) y + .5F, (float) z + .5F);
        /*if(tileEntity.sixNodeCacheMapId >= 0)
		{
			if(SixNodeCacheItem.map[tileEntity.sixNodeCacheMapId] != null)
			{
				UtilsClient.glDefaultColor();
				SixNodeCacheItem.map[tileEntity.sixNodeCacheMapId].draw(entity.getWorld(),entity.x,entity.y,entity.z);
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
        //Utils.glDefaultColor();
        Minecraft.getMinecraft().profiler.endSection();
    }
}
