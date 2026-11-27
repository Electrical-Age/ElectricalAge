package mods.eln.robotics.computer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A chip for one of the computer's 8 slots (Baughn: each takes a 1 MB RAM or ROM chip). */
public class ChipItem extends Item {
    public enum Kind {RAM, JANET, BOOT}

    public final Kind kind;

    public ChipItem(Kind kind, Properties p) {
        super(p.stacksTo(16));
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        String s = switch (kind) {
            case RAM -> "1 MB computer RAM (Janet requires 2 MB: slots 2–3).";
            case JANET -> "Janet Lisp ROM with terminal REPL (slot 1).";
            case BOOT -> "Floppy bootloader ROM: boots drive A sector 0 into slot 2 RAM (slot 1).";
        };
        out.accept(Component.literal(s).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
