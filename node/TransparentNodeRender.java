package mods.eln.node;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderEngine;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;

import com.jcraft.jorbis.Block;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

public class TransparentNodeRender extends TileEntitySpecialRenderer
{
	@Override
	public void renderTileEntityAt(TileEntity entity, double x, double y,
			double z, float var8) {
		// TODO Auto-generated method stub
		//System.out.println("delta T : " + var8);
		TransparentNodeEntity tileEntity = (TransparentNodeEntity) entity;
		if(tileEntity.elementRender == null) return;
		GL11.glPushMatrix();
			GL11.glTranslatef((float)x+.5F, (float)y+.5F, (float)z+.5F);
			tileEntity.elementRender.front.glRotateXnRef();
			tileEntity.elementRender.draw();
		GL11.glPopMatrix();

	}
}
		