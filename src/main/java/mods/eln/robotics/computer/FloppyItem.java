package mods.eln.robotics.computer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * A 1.44 MB floppy disk. The item holds only its disk id (given the first time it goes into a computer), and its
 * write-protect tab; the image is a file in the world (ComputerStore), so contents never travel to clients. Its label
 * is the item's name (rename it on an anvil). Right-click flips the tab.
 */
public class FloppyItem extends Item {
    static final String DISK = "disk", PROTECT = "protect";

    public FloppyItem(Properties p) {
        super(p.stacksTo(1));
    }

    public static UUID disk(ItemStack s) {
        String v = s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getStringOr(DISK, "");
        try {
            return v.isEmpty() ? null : UUID.fromString(v);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void setDisk(ItemStack s, UUID id) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putString(DISK, id.toString()));
    }

    public static boolean writeProtected(ItemStack s) {
        return s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr(PROTECT, false);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack s = player.getItemInHand(hand);
        boolean p = !writeProtected(s);
        if (!level.isClientSide()) {
            CustomData.update(DataComponents.CUSTOM_DATA, s, t -> t.putBoolean(PROTECT, p));
            player.sendOverlayMessage(Component.literal(p ? "Write-protected" : "Writable"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        out.accept(Component.literal(disk(stack) == null ? "Blank" : writeProtected(stack) ? "Write-protected" : "Writable")
            .withStyle(net.minecraft.ChatFormatting.GRAY));
        out.accept(Component.literal("Right-click to flip the write-protect tab; rename it on an anvil to label it")
            .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
