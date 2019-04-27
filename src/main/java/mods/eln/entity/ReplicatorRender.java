package mods.eln.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class ReplicatorRender extends RenderLiving {

    private static final ResourceLocation res = new ResourceLocation("eln:textures/entity/replicator.png");

    public ReplicatorRender(RenderManager rendermanagerIn, ModelBase modelbaseIn, float shadowsizeIn) {
        super(rendermanagerIn, modelbaseIn, shadowsizeIn);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return res;
    }
}
