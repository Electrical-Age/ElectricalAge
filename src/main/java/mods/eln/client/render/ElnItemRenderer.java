package mods.eln.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.IDescriptorItem;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.misc.UtilsClient;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * R5: a device item drawn as its 3D model (1.12 renderItem outside the GUI), item model type `eln:device` with
 * `context` "entity" (ground, item frame, head) or "hand". The pose comes from the base model's `display` block
 * (eln:item/device_six / device_transparent); the device draws centred on the block centre, through the GL11 shim
 * like the placed blocks.
 */
public class ElnItemRenderer implements SpecialModelRenderer<ElnItemRenderer.Arg> {
    /** The device (block descriptor) or the item descriptor with its stack (prints, live items). */
    public record Arg(Object descriptor, ItemStack stack) {
    }

    final boolean entity;

    ElnItemRenderer(boolean entity) {
        this.entity = entity;
    }

    @Override
    public void submit(@Nullable Arg a, PoseStack poseStack, SubmitNodeCollector collector,
                       int light, int overlay, boolean foil, int outline) {
        if (a == null) return;
        GlFrame frame = GlSubmit.open(poseStack);
        try {
            UtilsClient.glDefaultColor();
            if (a.descriptor() instanceof GenericItemBlockUsingDamageDescriptor d) {
                GL11.glTranslatef(.5f, .5f, .5f);
                d.drawItem(entity);
            } else if (a.descriptor() instanceof mods.eln.generic.GenericItemUsingDamageDescriptor d) {
                if (d.itemDrawParent() != null) GL11.glTranslatef(.5f, .5f, .5f); // model items: centred like devices
                d.drawItem(a.stack(), entity);
            }
        } catch (RuntimeException e) {
            ElnRenderErrors.report("item " + a.stack().getItem(), e);
        } finally {
            GlFrame.close();
        }
        GlSubmit.submit(frame, poseStack, collector, light);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int i = 0; i < 8; i++) output.accept(new Vector3f(i & 1, (i >> 1) & 1, (i >> 2) & 1));
    }

    @Override
    public @Nullable Arg extractArgument(ItemStack stack) {
        if (!(stack.getItem() instanceof IDescriptorItem i)) return null;
        Object d = i.getEaDescriptor();
        if (d instanceof GenericItemBlockUsingDamageDescriptor b && b.hasItemDraw()
            || d instanceof mods.eln.generic.GenericItemUsingDamageDescriptor g && g.hasItemDraw()) return new Arg(d, stack);
        return null;
    }

    public record Unbaked(String context) implements SpecialModelRenderer.Unbaked<Arg> {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("context").forGetter(Unbaked::context)).apply(i, Unbaked::new));

        @Override
        public SpecialModelRenderer<Arg> bake(SpecialModelRenderer.BakingContext context) {
            return new ElnItemRenderer("entity".equals(this.context));
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
