package mods.eln.client;

import mods.eln.generic.ElnItem;
import mods.eln.item.xray.XRayFilter;
import mods.eln.item.xray.XRayRayMarch;
import mods.eln.item.xray.XRayScannerDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The picture of the X-ray scanner the local player holds (switched on): the whole grid every other client tick
 * (not per frame: 1.12's lag), through a block cache per scan. XRayScannerDescriptor.drawItem shows it by stack id.
 */
final class XRayScan {
    private XRayScan() {
    }

    static long id;
    static float[] picture;
    static int tick;
    static List<ItemStack> filters;
    static Map<Block, Integer> slots = Map.of();

    static void install() {
        XRayScannerDescriptor.pictures = i -> i != 0 && i == id ? picture : null;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            picture = null;
            return;
        }
        ItemStack s = held(mc.player.getMainHandItem());
        if (s == null) s = held(mc.player.getOffhandItem());
        if (s == null) {
            picture = null;
            id = 0;
            return;
        }
        if (++tick % 2 != 0 && picture != null) return;
        XRayScannerDescriptor d = (XRayScannerDescriptor) ((ElnItem) s.getItem()).descriptor;
        List<ItemStack> f = XRayFilter.filters(s);
        if (filters == null || !ItemStack.listMatches(filters, f)) {
            filters = f;
            slots = XRayFilter.blockSlots(f);
        }
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getViewVector(1f);
        Vec3 up = mc.player.getUpVector(1f);
        var level = mc.level;
        Map<Long, Integer> cache = new HashMap<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        XRayRayMarch.Lookup lookup = (x, y, z) -> cache.computeIfAbsent(BlockPos.asLong(x, y, z), k -> {
            BlockState st = level.getBlockState(p.set(x, y, z));
            if (st.isAir()) return XRayRayMarch.AIR;
            Integer slot = slots.get(st.getBlock());
            if (slot != null) return slot;
            return st.canOcclude() ? XRayRayMarch.SOLID : XRayRayMarch.OTHER;
        });
        float[] out = new float[XRayScannerDescriptor.RES_W * XRayScannerDescriptor.RES_H * 3];
        XRayRayMarch.scan(lookup, new double[]{eye.x, eye.y, eye.z}, new double[]{look.x, look.y, look.z},
            new double[]{up.x, up.y, up.z}, d.fov, d.range, XRayScannerDescriptor.RES_W, XRayScannerDescriptor.RES_H, out);
        picture = out;
        long newId = XRayScannerDescriptor.id(s);
        if (newId != id) mods.eln.Eln.LOG.info("X-ray scanner {}: scanning, {} ore block types in its filters", newId, slots.size());
        id = newId;
    }

    static ItemStack held(ItemStack s) {
        return s.getItem() instanceof ElnItem i && i.descriptor instanceof XRayScannerDescriptor d
            && d.getState(s) == XRayScannerDescriptor.State.Run ? s : null;
    }
}
