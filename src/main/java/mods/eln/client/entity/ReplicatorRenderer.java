package mods.eln.client.entity;

import mods.eln.entity.ReplicatorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** 1.12 ReplicatorRender: the model with eln:textures/entity/replicator.png. */
public class ReplicatorRenderer extends MobRenderer<ReplicatorEntity, LivingEntityRenderState, ReplicatorModel> {
    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("eln", "textures/entity/replicator.png");

    public ReplicatorRenderer(EntityRendererProvider.Context context) {
        super(context, new ReplicatorModel(context.bakeLayer(ReplicatorModel.LAYER)), 0.3f);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}
