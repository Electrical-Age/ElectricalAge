package mods.eln.client.render;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import mods.eln.gl.GlFrame;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EA's own entity pipelines (P3): the vanilla entity shader with 1.12's fixed-function choices. Shading from vertex
 * normals (vanilla's no-cull entity pipelines shade per face), GL_LIGHTING off = no directional lighting
 * (NO_CARDINAL_LIGHTING), blending off / alpha / additive (halos: SRC_ALPHA, ONE, no depth write), culling on or off.
 * Alpha-test cutoff 0.1 as 1.12's glAlphaFunc(GREATER, 0.1).
 */
public final class ElnPipelines {
    private ElnPipelines() {
    }

    static final RenderPipeline[] PIPES = new RenderPipeline[12];
    public static final List<RenderPipeline> ALL = new ArrayList<>();

    static int index(boolean cull, boolean lit, int blend) {
        return (cull ? 6 : 0) + (lit ? 3 : 0) + blend;
    }

    static {
        for (int c = 0; c < 2; c++)
            for (int l = 0; l < 2; l++)
                for (int b = 0; b < 3; b++) {
                    boolean cull = c == 1, lit = l == 1;
                    RenderPipeline.Builder p = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                        .withLocation(Identifier.fromNamespaceAndPath("eln", "pipeline/node_" + (cull ? "cull" : "nocull")
                            + (lit ? "_lit" : "_unlit") + "_" + new String[]{"opaque", "alpha", "additive"}[b]))
                        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                        .withCull(cull);
                    if (!lit) p.withShaderDefine("NO_CARDINAL_LIGHTING");
                    switch (b) {
                        case 0 -> p.withColorTargetState(ColorTargetState.DEFAULT);
                        case 1 -> p.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
                        default -> p.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false));
                    }
                    RenderPipeline built = p.build();
                    PIPES[index(cull, lit, b)] = built;
                    ALL.add(built);
                }
    }

    private static final Map<Long, Map<Identifier, RenderType>> TYPES = new HashMap<>();

    /** The RenderType for a batch key (memoised per pipeline and texture). */
    public static RenderType type(GlFrame.Key key, Identifier texture) {
        int blend = switch (key.blend()) {
            case NONE -> 0;
            case ALPHA, MULTIPLY -> 1;
            default -> 2;
        };
        int i = index(key.cull(), key.lit(), blend);
        return TYPES.computeIfAbsent((long) i, k -> new HashMap<>()).computeIfAbsent(texture, t -> {
            RenderSetup.RenderSetupBuilder s = RenderSetup.builder(PIPES[i]).withTexture("Sampler0", t).useLightmap().useOverlay();
            if (blend == 0) s.affectsCrumbling();
            else {
                // "Improved Transparency" draws blended types through order-independent transparency: they need OIT
                // pipelines (vanilla's entity sets; unlit and additive parts use the emissive one)
                s.sortOnUpload();
                if (blend == 1 && key.lit()) s.setOitPipelines(key.cull() ? RenderPipelines.OIT_ENTITY_CULL : RenderPipelines.OIT_ENTITY);
                else s.setOitPipelines(RenderPipelines.OIT_ENTITY_EMISSIVE);
            }
            return RenderType.create("eln_node_" + i, s.createRenderSetup());
        });
    }
}
