package mods.eln.oil;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/** The Tank Loading Station as an item: what it does, in three lines (lang keys item.eln.tank_loading_station.tip1..3). */
public class TankStationItem extends BlockItem {
    public TankStationItem(Block block, Item.Properties p) {
        super(block, p);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        for (int i = 1; i <= 3; i++) out.accept(Component.translatable("item.eln.tank_loading_station.tip" + i).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
