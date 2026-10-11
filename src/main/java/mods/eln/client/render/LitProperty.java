package mods.eln.client.render;

import com.mojang.serialization.MapCodec;
import mods.eln.generic.ElnItem;
import mods.eln.item.electricalitem.ElectricalLampItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** R5: item model condition `eln:lit`: a flashlight that is on (1.12 ElectricalLampItem drew its "on" icon). */
public record LitProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<LitProperty> MAP_CODEC = MapCodec.unit(new LitProperty());

    @Override
    public boolean get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext context) {
        return stack.getItem() instanceof ElnItem i && i.descriptor instanceof ElectricalLampItem lamp && lamp.isLit(stack);
    }

    @Override
    public MapCodec<LitProperty> type() {
        return MAP_CODEC;
    }
}
