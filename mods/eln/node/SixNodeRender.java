package mods.eln.node;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;

import org.lwjgl.opengl.GL11;

import mods.eln.item.SixNodeCacheItem;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import com.jcraft.jorbis.Block;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

public class SixNodeRender extends TileEntitySpecialRenderer
{
	@Override
	public void renderTileEntityAt(TileEntity entity, double x, double y,
			double z, float var8) {
		Minecraft.getMinecraft().mcProfiler.startSection("SixNode");

		SixNodeEntity tileEntity = (SixNodeEntity) entity;
		

		
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x+.5F, (float)y+.5F, (float)z+.5F);	
		if(tileEntity.sixNodeCacheMapId >= 0)
		{
			if(SixNodeCacheItem.map[tileEntity.sixNodeCacheMapId] != null)
			{
				Utils.glDefaultColor();
				SixNodeCacheItem.map[tileEntity.sixNodeCacheMapId].draw();
			}
		}
		
		int idx = 0;
		for(SixNodeElementRender render : tileEntity.elementRenderList)
		{
			if(render != null)
			{
				Utils.glDefaultColor();
				GL11.glPushMatrix();
				Direction.fromInt(idx).glRotateXnRef();
				GL11.glTranslatef(-0.5F, 0f, 0f);
				render.draw();
				GL11.glPopMatrix();
			}
			idx++;
		}
		GL11.glPopMatrix();
		Utils.glDefaultColor();
		Minecraft.getMinecraft().mcProfiler.endSection();

	}
}
		