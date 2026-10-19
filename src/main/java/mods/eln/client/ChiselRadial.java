package mods.eln.client;

import mods.eln.client.gui.RadialMenu;
import mods.eln.network.ElnNet;
import mods.eln.node.voxel.ChiselBrush;
import mods.eln.node.voxel.ChiselItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The chisel's brush menu (Baughn: the wrench key with the chisel in hand opens a radial menu): the inner ring is the
 * size (1, 2, 4, 8), the outer ring the shape (cube, flat, tunnel), the centre the brush so far. On commit a changed
 * brush goes to the server as packetChiselBrush (ChiselItem.applyBrush), which sets it on the held chisel.
 */
final class ChiselRadial {
    private ChiselRadial() {
    }

    static RadialMenu create(ItemStack chisel, KeyMapping key, boolean held) {
        int size0 = ChiselItem.size(chisel);
        ChiselBrush.Shape shape0 = ChiselItem.shape(chisel);
        int[] size = {size0};
        ChiselBrush.Shape[] shape = {shape0};
        List<RadialMenu.Segment> sizes = new ArrayList<>(), shapes = new ArrayList<>();
        for (int s : ChiselBrush.SIZES)
            sizes.add(new RadialMenu.Segment(Component.literal(String.valueOf(s)), () -> size[0] == s, () -> size[0] = s));
        for (var sh : ChiselBrush.Shape.values())
            shapes.add(new RadialMenu.Segment(ChiselItem.shapeName(sh), () -> shape[0] == sh, () -> shape[0] = sh));
        return new RadialMenu(Component.translatable("gui.eln.chisel.brush"), List.of(sizes, shapes),
            () -> Component.literal(size[0] + ", ").append(ChiselItem.shapeName(shape[0])), key, held, () -> {
                if (size[0] == size0 && shape[0] == shape0) return;
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bos.writeBytes(ChiselItem.brushPacket(size[0], shape[0]));
                ElnNet.sendToServer(bos);
            });
    }
}
