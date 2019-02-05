package mods.eln.item.electricalitem;

import mods.eln.Eln;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.sixnode.lampsocket.LightBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class LampItem extends GenericItemUsingDamageDescriptor {

    abstract int getLightState(ItemStack stack);

    abstract int getRange(ItemStack stack);

    abstract int getLight(ItemStack stack);

    public LampItem(String name) {
        super(name);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5) {
        if (world.isRemote) return;
        if (getLightState(stack) == 0) return;
        int light = getLight(stack);
        if (light == 0) return;

        for (int yOffset = 0; yOffset < 2; yOffset++) {
            double x = entity.posX, y = entity.posY + 1.62 - yOffset, z = entity.posZ;

            Vec3d v = entity.getLookVec();
            v = new Vec3d(v.xCoord * 0.25, v.yCoord * 0.25, v.zCoord * 0.25);

            int range = getRange(stack) + 1;
            int rCount = 0;

            for (int idx = 0; idx < range; idx++) {
                x += v.xCoord;
                y += v.yCoord;
                z += v.zCoord;
                BlockPos pos = new BlockPos(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
                Block block = world.getBlockState(pos).getBlock();
                if (!world.isAirBlock(pos) && block != Eln.instance.lightBlock /*&& Block.blocksList[blockId].isOpaqueCube() == false*/) {
                    x -= v.xCoord;
                    y -= v.yCoord;
                    z -= v.zCoord;
                    break;
                }
                rCount++;
            }

            while (rCount > 0) {
                BlockPos pos = new BlockPos(MathHelper.floor_double(x), MathHelper.floor_double(y), MathHelper.floor_double(z));
                if (world.isAirBlock(pos) || world.getBlockState(pos).getBlock() == Eln.instance.lightBlock) {
                    //break;
                    LightBlockEntity.addLight(world, pos, light, 10);
                    return;/*
                    x -= v.xCoord * 4;
					y -= v.yCoord * 4;
					z -= v.zCoord * 4;
					rCount -= 4;*/
                }
                x -= v.xCoord;
                y -= v.yCoord;
                z -= v.zCoord;
                rCount--;
            }
        }
    }
}
