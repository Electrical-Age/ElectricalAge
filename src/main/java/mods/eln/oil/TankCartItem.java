package mods.eln.oil;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A Tank Minecart as an item: placed on a rail as any minecart is (MinecartItem). Its tooltip is the tank item's: with
 * contents (TankContents, which only a command gives it: a broken cart drops a minecart and its tank) only those and
 * the pressure (Baughn 2026-11-25), else the tank's rating and where it is filled.
 */
public class TankCartItem extends MinecartItem {
    final boolean alloy;

    public TankCartItem(EntityType<? extends AbstractMinecart> type, boolean alloy, Item.Properties p) {
        super(type, p);
        this.alloy = alloy;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        TankContents c = stack.get(mods.eln.registry.ElnRegistry.TANK_CONTENTS.get());
        if (c != null) {
            for (String l : c.lines()) out.accept(Component.literal(l));
            return;
        }
        var d = mods.eln.Eln.transparentNodeItem.getDescriptor(alloy ? TankDescriptor.ALLOY_ID : TankDescriptor.IRON_ID);
        if (d instanceof TankDescriptor t) out.accept(Component.literal(t.ratingLine()));
        out.accept(Component.literal(mods.eln.i18n.I18N.tr("Fill and empty it at a Tank Loading Station.")));
    }
}
