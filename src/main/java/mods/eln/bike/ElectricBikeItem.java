package mods.eln.bike;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;

/** The electric bike as an item: used on a block, it stands there on its kickstand, facing the way you face. */
public class ElectricBikeItem extends Item {
    public ElectricBikeItem(Item.Properties p) {
        super(p);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos();
        double y = ctx.getClickedFace() == Direction.UP ? ctx.getClickLocation().y : pos.relative(ctx.getClickedFace()).getY();
        BlockPos at = ctx.getClickedFace() == Direction.UP ? pos : pos.relative(ctx.getClickedFace());
        ElectricBike bike = mods.eln.registry.ElnRegistry.ELECTRIC_BIKE_ENTITY.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (bike == null) return InteractionResult.FAIL;
        float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : ctx.getHorizontalDirection().toYRot();
        bike.snapTo(at.getX() + 0.5, y, at.getZ() + 0.5, yaw, 0);
        if (!level.noCollision(bike, bike.getBoundingBox())) return InteractionResult.FAIL;
        bike.setCustomName(ctx.getItemInHand().get(net.minecraft.core.component.DataComponents.CUSTOM_NAME));
        bike.setColors(BikeColors.of(ctx.getItemInHand()));   // (its paint, from when it was picked up)
        level.addFreshEntity(bike);
        level.gameEvent(ctx.getPlayer(), net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, at);
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    /** Baughn: "Not a motorcycle". */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("item.eln.electric_bike.tooltip").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
