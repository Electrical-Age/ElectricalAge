package mods.eln.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class ReplicatorRender extends RenderLiving {

    private final static ResourceLocation textureLoc = new ResourceLocation("eln", "textures/entity/replicator.png");

    public ReplicatorRender(ModelBase par1ModelBase, float par2) {
        super(par1ModelBase, par2);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return ReplicatorRender.textureLoc;
    }
}
