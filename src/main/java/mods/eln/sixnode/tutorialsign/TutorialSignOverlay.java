package mods.eln.sixnode.tutorialsign;

import org.lwjgl.opengl.GL11;

import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeBlock;
import mods.eln.node.six.SixNodeElementRender;
import mods.eln.node.six.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class TutorialSignOverlay {
	public TutorialSignOverlay() {
		int i = 0;
		i++;
	}
	
	TutorialSignRender oldRender = null;
	@SubscribeEvent
	public void render(RenderGameOverlayEvent.Text event) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityClientPlayerMP player = mc.thePlayer;
		
		if(oldRender != null){
			oldRender.lightInterpol.setTarget(0);
			oldRender = null;
		}
		
		int px = MathHelper.floor_double(player.posX),py = MathHelper.floor_double(player.posY),pz = MathHelper.floor_double(player.posZ);
		int r = 1;
		World w = player.worldObj;

		TutorialSignRender best = null;
		double bestDistance = 10000;
		
		for(int x = px-r;x <= px+r;x++){
			for(int y = py-r;y <= py+r;y++){
				for(int z = pz-r;z <= pz+r;z++){
					if(w.getBlock(x, y, z) instanceof SixNodeBlock){
						TileEntity e = w.getTileEntity(x, y, z);
						if(e instanceof SixNodeEntity){
							SixNodeEntity sne = (SixNodeEntity)e;
							for(SixNodeElementRender render : sne.elementRenderList){
								if(render instanceof TutorialSignRender){
									double d = Utils.getLength(player.posX, player.posY, player.posZ, x+0.5, y+0.5, z+0.5);
									if(d < bestDistance){
										bestDistance = d;
										best = (TutorialSignRender)render;
										break;
									}
								}
							}
						}
					}
				}		
			}
		}
		
		
		if(best != null){
			oldRender = best;
			oldRender.lightInterpol.setTarget(1f);
			GL11.glPushMatrix();
			GL11.glScalef(0.5f, 0.5f, 0.5f);
			int y = 0;
			for(String str : best.texts){
				Minecraft.getMinecraft().fontRenderer.drawString(str,10/* event.resolution.getScaledWidth()/2-50*/, 10+y, 0xFFFFFF);
				y+=10;
			}
			GL11.glPopMatrix();
		}
		
		
	}
}
