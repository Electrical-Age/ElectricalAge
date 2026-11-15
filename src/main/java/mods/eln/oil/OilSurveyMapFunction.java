package mods.eln.oil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Loot function eln:oil_survey_map (todo #470): makes a filled map centred on an oil lake picked at random among those
 * min_distance..max_distance blocks from where the loot is made (OilSurvey.pick; vanilla's exploration_map takes the
 * nearest structure and loads chunks to check it). For villager trades (given_item_modifier) and chest loot. The item
 * comes back unchanged (no map id) when there is no lake, as exploration_map does.
 */
public class OilSurveyMapFunction extends LootItemConditionalFunction {
    public static final MapCodec<OilSurveyMapFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i).and(i.group(
        MapDecorationType.CODEC.optionalFieldOf("decoration", MapDecorationTypes.RED_X).forGetter(f -> f.decoration),
        Codec.BYTE.optionalFieldOf("zoom", (byte) 2).forGetter(f -> f.zoom),
        Codec.INT.optionalFieldOf("min_distance", 600).forGetter(f -> f.minDistance),
        Codec.INT.optionalFieldOf("max_distance", 3000).forGetter(f -> f.maxDistance)
    )).apply(i, OilSurveyMapFunction::new));

    private final Holder<MapDecorationType> decoration;
    private final byte zoom;
    private final int minDistance, maxDistance;

    private OilSurveyMapFunction(Optional<Holder<LootItemCondition>> condition, Holder<MapDecorationType> decoration, byte zoom, int minDistance, int maxDistance) {
        super(condition);
        this.decoration = decoration;
        this.zoom = zoom;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    @Override
    public MapCodec<OilSurveyMapFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.ORIGIN);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (stack.isEmpty()) return stack;
        Vec3 at = context.getOptional(LootContextParams.ORIGIN);
        if (at == null) return stack;
        ServerLevel level = context.getLevel();
        BlockPos lake = OilSurvey.pick(level, BlockPos.containing(at), minDistance, maxDistance, context.getRandom());
        if (lake == null) return stack;
        fill(level, stack, lake, zoom, decoration);
        return stack;
    }

    /** The stack made a map of the land over a lake (zoom 2: 512 blocks across), its centre marked, its position in the lore. */
    public static void fill(ServerLevel level, ItemStack stack, BlockPos lake, byte zoom, Holder<MapDecorationType> decoration) {
        MapItem.applyNewSavedData(level, stack, lake.getX(), lake.getZ(), zoom, true, true);
        MapItem.renderBiomePreviewMap(level, stack);
        MapItemSavedData.addTargetDecoration(stack, lake, "+", decoration);
        stack.set(DataComponents.LORE, new ItemLore(List.of(
            Component.translatableWithFallback("item.eln.oil_survey_map.lore", "Oil under X %s, Z %s", lake.getX(), lake.getZ()))));
    }
}
