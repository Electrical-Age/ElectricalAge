package mods.eln.node;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;

import org.lwjgl.opengl.GL11;

import mods.eln.item.SixNodeCacheItem;
import mods.eln.misc.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderEngine;
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
		// TODO Auto-generated method stub
	/*	try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//Minecraft.getMinecraft().mcProfiler.startSection("miaou !!!!!");
		SixNodeEntity tileEntity = (SixNodeEntity) entity;
		

		
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x+.5F, (float)y+.5F, (float)z+.5F);	
		if(tileEntity.sixNodeCacheMapId >= 0)
		{
			if(SixNodeCacheItem.map[tileEntity.sixNodeCacheMapId] != null)
				SixNodeCacheItem.map[tileEntity.sixNodeCacheMapId].draw();
		}
		
		int idx = 0;
		for(SixNodeElementRender render : tileEntity.elementRenderList)
		{
			if(render != null)
			{
				GL11.glPushMatrix();
				Direction.fromInt(idx).glRotateXnRef();
				GL11.glTranslatef(-0.5F, 0f, 0f);
				render.draw();
				GL11.glPopMatrix();
			}
			idx++;
		}
		GL11.glPopMatrix();
		//Minecraft.getMinecraft().mcProfiler.endSection();
		/*
		
		ItemStack i = Minecraft.getMinecraft().thePlayer.inventory.armorInventory[3];
		
		GL11.glDisable(GL_TEXTURE_2D);
		GL11.glPushMatrix();
		GL11.glTranslatef((float)x+.5F, (float)y+.5F, (float)z+.5F);
		GL11.glPointSize(10);
		GL11.glBegin(GL11.GL_POINTS);
		
		GL11.glTexCoord2f(0.0f,0.0f);
		GL11.glNormal3f(0.0f, 1.0f, 0.0f);
		
		
		

		for(Direction dir : Direction.values())
		{
			if(tileEntity.getSyncronizedSideEnable(dir))
			{
				GL11.glColor4d(1.0, 0.0, 0.0, 1.0);

				double[] vect = {0.0,0.0,0.0};
				dir.applyTo(vect, 0.45);
				GL11.glVertex3d(vect[0],vect[1],vect[2]);
			}
		}

		GL11.glEnd();
		GL11.glPopMatrix();
		GL11.glEnable(GL_TEXTURE_2D);		
		*/
	}
}
		