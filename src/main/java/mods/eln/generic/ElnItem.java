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

    /** The descriptor is a mining tool (overrides getStrVsBlock: electrical pickaxe / axe). */
    private final boolean tool;

    public ElnItem(Properties properties, GenericItemUsingDamageDescriptor descriptor) {
        super(properties);
        this.descriptor = descriptor;
        boolean t;
        try {
            t = descriptor.getClass().getMethod("getStrVsBlock", ItemStack.class, net.minecraft.world.level.block.Block.class)
                .getDeclaringClass() != GenericItemUsingDamageDescriptor.class;
        } catch (NoSuchMethodException e) {
            t = false;
        }
        tool = t;
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

    // ---- mining (1.12 GenericItemUsingDamage). 1.12 gave every EA item dig speed 0.2 and harvest rights on every
    // block; 26.x: only the tools (descriptors that override getStrVsBlock) do, other EA items mine like a hand. ----

    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return tool ? descriptor.getStrVsBlock(stack, state.getBlock()) : super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        return tool || super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state,
                             net.minecraft.core.BlockPos pos, net.minecraft.world.entity.LivingEntity owner) {
        if (level.isClientSide()) return false;
        return descriptor.onBlockDestroyed(stack, level, state.getBlock(), pos.getX(), pos.getY(), pos.getZ(), owner);
    }

    @Override
    public boolean onDroppedByPlayer(ItemStack item, Player player) {
        return descriptor.onDroppedByPlayer(item, player);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, net.minecraft.world.entity.LivingEntity entity, InteractionHand hand) {
        return descriptor.onEntitySwing(entity, stack);
    }

    /** EA items keep state in their custom data (energy, counters): changing it must not replay the equip animation. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    // ---- R5: the energy of electrical items as the vanilla item bar (1.12 drew its own red->yellow bar in the GUI)

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return descriptor instanceof mods.eln.item.electricalinterface.IItemEnergyBattery || descriptor.itemBar(stack) >= 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double f = descriptor.itemBar(stack);
        return f >= 0 ? (int) Math.round(13 * Math.min(1, f)) : energyBarWidth(descriptor, stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        double f = descriptor.itemBar(stack);
        if (f >= 0) return net.minecraft.util.Mth.hsvToRgb((float) Math.min(1, f) / 3f, 1f, 1f);
        return energyBarColor(descriptor, stack);
    }

    static double energyFraction(Object d, ItemStack stack) {
        if (!(d instanceof mods.eln.item.electricalinterface.IItemEnergyBattery b)) return 0;
        double max = b.getEnergyMax(stack);
        return max <= 0 ? 0 : Math.max(0, Math.min(1, b.getEnergy(stack) / max));
    }

    public static int energyBarWidth(Object d, ItemStack stack) {
        return (int) Math.round(13 * energyFraction(d, stack));
    }

    /** 1.12 drawEnergyBare: red when empty to yellow when full. */
    public static int energyBarColor(Object d, ItemStack stack) {
        return 0xFF0000 | ((int) Math.round(255 * energyFraction(d, stack)) << 8);
    }
}
