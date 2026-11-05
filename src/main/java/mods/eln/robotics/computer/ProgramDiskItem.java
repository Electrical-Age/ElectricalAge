package mods.eln.robotics.computer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * A floppy that comes with a program (rom/disks/NAME in the repository, built into resources): the first time it goes
 * into a computer it gets a disk of its own holding a copy of that image, and from then on it is an ordinary floppy.
 */
public class ProgramDiskItem extends FloppyItem {
    public final String preset;
    final String what;

    public ProgramDiskItem(String preset, String what, Properties p) {
        super(p);
        this.preset = preset;
        this.what = what;
    }

    /** The image it starts with, or null if the jar lacks it. */
    public byte[] image() {
        try (var in = ProgramDiskItem.class.getResourceAsStream("/eln/computer/disks/" + preset + ".img")) {
            return in == null ? null : in.readAllBytes();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> out, net.minecraft.world.item.TooltipFlag flag) {
        out.accept(Component.literal(what).withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, out, flag);
    }
}
