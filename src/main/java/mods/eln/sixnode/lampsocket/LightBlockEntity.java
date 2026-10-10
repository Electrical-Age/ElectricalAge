package mods.eln.sixnode.lampsocket;

import mods.eln.compat.BlockMeta;
import mods.eln.compat.WorldCompat;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.registry.ElnRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * The light of lamps, the turret and the teleporter (1.12 LightBlockEntity): each source adds a (level, timeout in
 * ticks) handle; every tick the block shows the brightest live handle and counts the timeouts down; without handles
 * the block removes itself. Not saved (as in 1.12): sources refresh their handles while they run.
 */
public class LightBlockEntity extends BlockEntity {

    ArrayList<LightHandle> lightList = new ArrayList<LightHandle>();

    public static final ArrayList<LightBlockObserver> observers = new ArrayList<LightBlockObserver>();

    static void addObserver(LightBlockObserver observer) {
        observers.add(observer);
    }

    static void removeObserver(LightBlockObserver observer) {
        observers.remove(observer);
    }

    public interface LightBlockObserver {
        void lightBlockDestructor(Coordonate coord);
    }

    static class LightHandle {
        byte value;
        int timeout;

        public LightHandle(byte value, int timeout) {
            this.value = value;
            this.timeout = timeout;
        }
    }

    public LightBlockEntity(BlockPos pos, BlockState state) {
        super(ElnRegistry.LIGHT_ENTITY.get(), pos, state);
    }

    void addLight(int light, int timeout) {
        lightList.add(new LightHandle((byte) light, timeout));
    }

    /** Server tick (1.12 ITickable.update). */
    void update() {
        Level world = getLevel();
        BlockPos pos = getBlockPos();
        if (lightList.isEmpty()) {
            world.removeBlock(pos, false);
            Utils.println("Destroy light at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " ");
            return;
        }

        int light = 0;
        Iterator<LightHandle> iterator = lightList.iterator();

        while (iterator.hasNext()) {
            LightHandle l = iterator.next();
            if (light < l.value) light = l.value;

            l.timeout--;
            if (l.timeout <= 0) {
                iterator.remove();
            }
        }

        BlockState state = world.getBlockState(pos);
        if (state.hasProperty(BlockMeta.META) && light != state.getValue(BlockMeta.META)) {
            // 26.x keeps the block entity when only a property changes (1.12 needed shouldRefresh for that)
            world.setBlock(pos, state.setValue(BlockMeta.META, light), 2);
        }
    }

    public static void addLight(Level w, int x, int y, int z, int light, int timeout) {
        Block block = WorldCompat.getBlock(w, x, y, z);
        if (block != ElnDeviceRegistry.lightBlock) {
            if (block != Blocks.AIR) return;
            WorldCompat.setBlock(w, x, y, z, ElnDeviceRegistry.lightBlock, light, 2);
        }

        BlockEntity t = WorldCompat.getTileEntity(w, x, y, z);
        if (t instanceof LightBlockEntity)
            ((LightBlockEntity) t).addLight(light, timeout);
        else
            Utils.println("ASSERT if(t != null && t instanceof LightBlockEntity)");
    }

    public static void addLight(Coordonate coord, int light, int timeout) {
        addLight(coord.world(), coord.x, coord.y, coord.z, light, timeout);
    }
}
