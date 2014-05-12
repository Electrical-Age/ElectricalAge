package mods.eln.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.SideOnly;



public class ReplicatorRender extends RenderLiving
{
    //private static final ResourceLocation Your_Texture = new ResourceLocation("eln:textures/blocks/copperore.png");  //refers to:assets/yourmod/textures/entity/yourtexture.png
	private static final ResourceLocation Your_Texture = new ResourceLocation("eln:textures/entity/replicator.png");  //refers to:assets/yourmod/textures/entity/yourtexture.png
	
    public ReplicatorRender(ModelBase par1ModelBase, float par2)
    {
        super(par1ModelBase, par2);
    }


	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		// TODO Auto-generated method stub
		return Your_Texture;
	}
}