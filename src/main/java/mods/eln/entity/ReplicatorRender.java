package mods.eln.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class ReplicatorRender extends RenderLiving {

    private static final ResourceLocation res = new ResourceLocation("eln:textures/entity/replicator.png");

    public ReplicatorRender(ModelBase par1ModelBase, float par2) {
        super(par1ModelBase, par2);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return res;
    }

	/*@Override
    public void doRender(Entity par1Entity, double par2, double par4, double par6, float par8, float par9) {
		//UtilsClient.disableDepthTest();
		//GL11.glColor3f(1, 0, 0);
		super.doRender(par1Entity, par2, par4, par6, par8, par9);
		//GL11.glColor3f(1, 1, 1);
		//UtilsClient.enableDepthTest();
	}*/
}
