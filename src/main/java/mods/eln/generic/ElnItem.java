package mods.eln.generic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The item of one plain EA descriptor (1.12: a sub-item of SharedItem / SharedItemStackOne). Behaviour is delegated
 * to the descriptor's 1.12 hooks; models, tooltips and names come with P3.
 */
public class ElnItem extends Item implements IDescriptorItem {
    public final GenericItemUsingDamageDescriptor descriptor;

    public ElnItem(Properties properties, GenericItemUsingDamageDescriptor descriptor) {
        super(properties);
        this.descriptor = descriptor;
    }

    @Override
    public Object getEaDescriptor() {
        return descriptor;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(descriptor.getName(stack));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        ItemStack r = descriptor.onItemRightClick(s, level, player);
        if (r != s) player.setItemInHand(hand, r);
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        net.minecraft.core.BlockPos p = ctx.getClickedPos();
        boolean done = descriptor.onItemUse(ctx.getItemInHand(), ctx.getPlayer(), ctx.getLevel(), p.getX(), p.getY(), p.getZ(),
            ctx.getClickedFace().get3DDataValue(), (float) (ctx.getClickLocation().x - p.getX()),
            (float) (ctx.getClickLocation().y - p.getY()), (float) (ctx.getClickLocation().z - p.getZ()));
        return done ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        descriptor.onUpdate(stack, level, entity, slot == null ? -1 : slot.getIndex(), slot == EquipmentSlot.MAINHAND);
    }
}
