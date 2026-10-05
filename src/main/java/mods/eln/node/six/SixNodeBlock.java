package mods.eln.node.six;


import mods.eln.compat.WorldCompat;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.List;
import java.util.Random;

public class SixNodeBlock extends NodeBlock {
    // public static ArrayList<Integer> repertoriedItemStackId = new ArrayList<Integer>();

    // private IIcon icon;
    public SixNodeBlock(Material material, Class tileEntityClass) {
        super(material, tileEntityClass, 0);

        // setBlockTextureName("eln:air");
    }


    private static RayTraceResult newHit(int x, int y, int z, int side, Vec3d hit) {
        return new RayTraceResult(hit, EnumFacing.byIndex(side), new BlockPos(x, y, z));
    }

    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        SixNodeEntity entity = (SixNodeEntity) world.getTileEntity(pos);
        if (entity != null) {
            SixNodeElementRender render = entity.elementRenderList[Direction.fromIntMinecraftSide(target.sideHit.getIndex()).getInt()];
            if (render != null) {
                return render.sixNodeDescriptor.newItemStack();
            }
        }

        return super.getPickBlock(state, target, world, pos, player);
    }

    // TODO(1.12 WP6 icon): registerBlockIcons ("eln:air") and getIcon (camouflage) removed; camouflage is dropped for now.

    /** 1.7.10 getCollisionBoundingBoxFromPool: full block if camouflaged or the element has volume, else none. */
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (nodeHasCache(world, x, y, z) || hasVolume(world, x, y, z))
            return super.getCollisionBoundingBox(state, world, pos);
        else
            return NULL_AABB;
    }

    public boolean hasVolume(IBlockAccess world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
        if (!(tileEntity instanceof SixNodeEntity) || tileEntity.getWorld() == null) return false;
        return hasVolume(tileEntity.getWorld(), x, y, z);
    }


    public boolean hasVolume(World world, int x, int y, int z) {
        SixNodeEntity entity = getEntity(world, x, y, z);
        if (entity == null) return false;
        return entity.hasVolume(world, x, y, z);

    }

    @Override
    public float getBlockHardness(IBlockState state, World world, BlockPos pos) {
        return 0.3f;
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        if (world == null)
            return 0;
        SixNodeEntity entity = getEntity(world, x, y, z);
        return entity == null ? 0 : entity.getDamageValue(world, x, y, z);
    }

    SixNodeEntity getEntity(World world, int x, int y, int z) {
        TileEntity tileEntity = WorldCompat.getTileEntity(world, x, y, z);
        if (tileEntity != null && tileEntity instanceof SixNodeEntity)
            return (SixNodeEntity) tileEntity;
        Utils.println("ASSERTSixNodeEntity getEntity() null");
        return null;

    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        /*
		 * for (Integer id : repertoriedItemStackId) { subItems.add(new ItemStack(this, 1, id)); }
		 */
        Eln.sixNodeItem.getSubItems(tab, subItems);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return true;
    }

    /** 1.7.10 used render type 0 with a transparent "eln:air" icon (or the camouflage block's icon). Camouflage is
     *  dropped for now, so nothing is drawn as a block. TODO(1.12 WP5): particle texture / camouflage. */
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }

	/*
	 * @Override public int getLightOpacity(World world, int x, int y, int z) {
	 * 
	 * return 255; }
	 */

    @Override
    public Item getItemDropped(IBlockState state, Random p_149650_2_, int p_149650_3_) {

        return Items.AIR;
    }

    public int quantityDropped(Random par1Random) {
        return 0;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean canPlaceBlockOnSide(World par1World, BlockPos pos, EnumFacing side) {
		/* see canPlaceBlockAt; it needs changing if this method is fixed */
        return true;/*
					 * if(par1World.isRemote) return true; SixNodeEntity tileEntity = (SixNodeEntity) par1World.getBlockTileEntity(par2, par3, par4); if(tileEntity == null || (tileEntity instanceof SixNodeEntity) == false) return true; Direction direction = Direction.fromIntMinecraftSide(par5); SixNode node = (SixNode) tileEntity.getNode(); if(node == null) return true; if(node.getSideEnable(direction))return false;
					 * 
					 * return true;
					 */
    }

    @Override
    public boolean canPlaceBlockAt(World par1World, BlockPos pos) {
		/* This should probably call canPlaceBlockOnSide with each
		 * appropriate side to see if it can go somewhere.
		 * (cf. BlockLever, BlockTorch, etc)

		 * Currently, canPlaceBlockOnSide returns true and defers
		 * check to other code.  The rest of the sixnode code isn't
		 * expecting blind canPlaceBlockAt to work, so things that
		 * call it (e.g. Rannuncarpus) confuse it terribly and leak
		 * cables and nodepieces.

		 * So for now, make the Rannuncarpus et al ignore it.
		 */
		return false;
    }

    @Override
    public boolean onBlockPlacedBy(World world, int x, int y, int z, Direction direction, EntityLivingBase entityLiving, int metadata) {

        return true;
    }

    /*
     * @Override public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int minecraftSide, float vx, float vy, float vz) { SixNodeEntity tileEntity = (SixNodeEntity) world.getBlockTileEntity(x, y, z);
     *
     * return tileEntity.onBlockActivated(entityPlayer, Direction.fromIntMinecraftSide(minecraftSide),vx,vy,vz); }
     */
    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer entityPlayer, boolean willHarvest) {
        if (world.isRemote) return false;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        SixNodeEntity tileEntity = (SixNodeEntity) WorldCompat.getTileEntity(world, x, y, z);

        RayTraceResult MOP = collisionRayTrace(world, x, y, z, entityPlayer);
        if (MOP == null) return false;

        SixNode sixNode = (SixNode) tileEntity.getNode();
        if (sixNode == null) return true;
        if (sixNode.sixNodeCacheBlock != Blocks.AIR) {

            if (Utils.isCreative((EntityPlayerMP) entityPlayer) == false) {
                ItemStack stack = new ItemStack(sixNode.sixNodeCacheBlock, 1, sixNode.sixNodeCacheBlockMeta);
                sixNode.dropItem(stack);
            }

            sixNode.sixNodeCacheBlock = Blocks.AIR;

            Chunk chunk = WorldCompat.getChunkFromBlockCoords(world, x, z);
            Utils.generateHeightMap(chunk);
            Utils.updateSkylight(chunk);
            chunk.generateSkylightMap();
            Utils.updateAllLightTypes(world, x, y, z);

            sixNode.setNeedPublish(true);
            return false;
        }
        if (false == sixNode.playerAskToBreakSubBlock((EntityPlayerMP) entityPlayer, Direction.fromIntMinecraftSide(MOP.sideHit.getIndex())))
            return false;

        if (sixNode.getIfSideRemain()) return true;

        return super.removedByPlayer(state, world, pos, entityPlayer, willHarvest);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (world.isRemote == false) {
            SixNodeEntity tileEntity = (SixNodeEntity) WorldCompat.getTileEntity(world, x, y, z);
            SixNode sixNode = (SixNode) tileEntity.getNode();
            if (sixNode == null) return;

            for (Direction direction : Direction.values()) {
                if (sixNode.getSideEnable(direction)) {
                    sixNode.deleteSubBlock(null, direction);
                }
            }
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block par5, BlockPos fromPos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        SixNodeEntity tileEntity = (SixNodeEntity) WorldCompat.getTileEntity(world, x, y, z);
        SixNode sixNode = (SixNode) tileEntity.getNode();
        if (sixNode == null) return;

        for (Direction direction : Direction.values()) {
            if (sixNode.getSideEnable(direction)) {
                if (!getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                    sixNode.deleteSubBlock(null, direction);
                }
            }
        }

        if (!sixNode.getIfSideRemain()) {
            WorldCompat.setBlockToAir(world, x, y, z);
        } else {
            super.neighborChanged(state, world, pos, par5, fromPos);
        }
    }

    double w = 0.0;

    boolean[] booltemp = new boolean[6];

    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        return collisionRayTrace(world, pos.getX(), pos.getY(), pos.getZ(), start, end);
    }

    public RayTraceResult collisionRayTrace(World world, int x, int y, int z, Vec3d start, Vec3d end) {
        if (nodeHasCache(world, x, y, z)) {
            BlockPos pos = new BlockPos(x, y, z);
            return super.collisionRayTrace(world.getBlockState(pos), world, pos, start, end);
        }
        SixNodeEntity tileEntity = (SixNodeEntity) WorldCompat.getTileEntity(world, x, y, z);
        if (tileEntity == null) return null;
        if (world.isRemote) {
            booltemp[0] = tileEntity.getSyncronizedSideEnable(Direction.XN);
            booltemp[1] = tileEntity.getSyncronizedSideEnable(Direction.XP);
            booltemp[2] = tileEntity.getSyncronizedSideEnable(Direction.YN);
            booltemp[3] = tileEntity.getSyncronizedSideEnable(Direction.YP);
            booltemp[4] = tileEntity.getSyncronizedSideEnable(Direction.ZN);
            booltemp[5] = tileEntity.getSyncronizedSideEnable(Direction.ZP);
            SixNodeEntity entity = getEntity(world, x, y, z);
            if (entity != null) {
                SixNodeElementRender element = entity.elementRenderList[Direction.YN.getInt()];
                // setBlockBounds(0, 0, 0, 1, 1, 1);
                if (element != null && element.sixNodeDescriptor.hasVolume()) {

                    return newHit(x, y, z, Direction.YN.toSideValue(), new Vec3d(0.5, 0.5, 0.5));
                }
            }

        } else {
            SixNode sixNode = (SixNode) tileEntity.getNode();
            if (sixNode == null) return null;
            booltemp[0] = sixNode.getSideEnable(Direction.XN);
            booltemp[1] = sixNode.getSideEnable(Direction.XP);
            booltemp[2] = sixNode.getSideEnable(Direction.YN);
            booltemp[3] = sixNode.getSideEnable(Direction.YP);
            booltemp[4] = sixNode.getSideEnable(Direction.ZN);
            booltemp[5] = sixNode.getSideEnable(Direction.ZP);
            SixNodeEntity entity = getEntity(world, x, y, z);
            if (entity != null) {
                NodeBase node = entity.getNode();
                if (node != null && node instanceof SixNode) {
                    SixNodeElement element = ((SixNode) node).sideElementList[Direction.YN.getInt()];
                    if (element != null && element.sixNodeElementDescriptor.hasVolume())
                        return newHit(x, y, z, Direction.YN.toSideValue(), new Vec3d(0.5, 0.5, 0.5));
                }
            }

        }
        // XN

        if (isIn(x, end.x, start.x) && booltemp[0]) {
            double hitX, hitY, hitZ, ratio;
            ratio = (x - start.x) / (end.x - start.x);
            if (ratio <= 1.1) {
                hitX = start.x + ratio * (end.x - start.x);
                hitY = start.y + ratio * (end.y - start.y);
                hitZ = start.z + ratio * (end.z - start.z);
                if (isIn(hitY, y + w, y + 1 - w) && isIn(hitZ, z + w, z + 1 - w))
                    return newHit(x, y, z, Direction.XN.toSideValue(), new Vec3d(hitX, hitY, hitZ));
            }
        }
        // XP
        if (isIn(x + 1, start.x, end.x) && booltemp[1]) {
            double hitX, hitY, hitZ, ratio;
            ratio = (x + 1 - start.x) / (end.x - start.x);
            if (ratio <= 1.1) {
                hitX = start.x + ratio * (end.x - start.x);
                hitY = start.y + ratio * (end.y - start.y);
                hitZ = start.z + ratio * (end.z - start.z);
                if (isIn(hitY, y + w, y + 1 - w) && isIn(hitZ, z + w, z + 1 - w))
                    return newHit(x, y, z, Direction.XP.toSideValue(), new Vec3d(hitX, hitY, hitZ));
            }
        }
        // YN
        if (isIn(y, end.y, start.y) && booltemp[2]) {
            double hitX, hitY, hitZ, ratio;
            ratio = (y - start.y) / (end.y - start.y);
            if (ratio <= 1.1) {
                hitX = start.x + ratio * (end.x - start.x);
                hitY = start.y + ratio * (end.y - start.y);
                hitZ = start.z + ratio * (end.z - start.z);
                if (isIn(hitX, x + w, x + 1 - w) && isIn(hitZ, z + w, z + 1 - w))
                    return newHit(x, y, z, Direction.YN.toSideValue(), new Vec3d(hitX, hitY, hitZ));
            }

        }
        // YP
        if (isIn(y + 1, start.y, end.y) && booltemp[3]) {
            double hitX, hitY, hitZ, ratio;
            ratio = (y + 1 - start.y) / (end.y - start.y);
            if (ratio <= 1.1) {
                hitX = start.x + ratio * (end.x - start.x);
                hitY = start.y + ratio * (end.y - start.y);
                hitZ = start.z + ratio * (end.z - start.z);
                if (isIn(hitX, x + w, x + 1 - w) && isIn(hitZ, z + w, z + 1 - w))
                    return newHit(x, y, z, Direction.YP.toSideValue(), new Vec3d(hitX, hitY, hitZ));
            }
        }
        // ZN
        if (isIn(z, end.z, start.z) && booltemp[4]) {
            double hitX, hitY, hitZ, ratio;
            ratio = (z - start.z) / (end.z - start.z);
            if (ratio <= 1.1) {
                hitX = start.x + ratio * (end.x - start.x);
                hitY = start.y + ratio * (end.y - start.y);
                hitZ = start.z + ratio * (end.z - start.z);
                if (isIn(hitY, y + w, y + 1 - w) && isIn(hitX, x + w, x + 1 - w))
                    return newHit(x, y, z, Direction.ZN.toSideValue(), new Vec3d(hitX, hitY, hitZ));
            }
        }
        // ZP
        if (isIn(z + 1, start.z, end.z) && booltemp[5]) {
            double hitX, hitY, hitZ, ratio;
            ratio = (z + 1 - start.z) / (end.z - start.z);
            if (ratio <= 1.1) {
                hitX = start.x + ratio * (end.x - start.x);
                hitY = start.y + ratio * (end.y - start.y);
                hitZ = start.z + ratio * (end.z - start.z);
                if (isIn(hitY, y + w, y + 1 - w) && isIn(hitX, x + w, x + 1 - w))
                    return newHit(x, y, z, Direction.ZP.toSideValue(), new Vec3d(hitX, hitY, hitZ));
            }
        }

        return null;
    }

    public static boolean isIn(double value, double min, double max) {
        if (value >= min && value <= max) return true;
        return false;
    }

    public RayTraceResult collisionRayTrace(World world, int x, int y, int z, EntityPlayer entityLiving) {

        // double distanceMax = (double)Minecraft.getMinecraft().playerController.getBlockReachDistance();
        double distanceMax = 5.0;
        Vec3d start = new Vec3d(entityLiving.posX, entityLiving.posY, entityLiving.posZ);

        if (!world.isRemote) start = start.add(0, 1.62, 0);
        Vec3d var5 = entityLiving.getLook(0.5f);
        Vec3d end = start.add(var5.x * distanceMax, var5.y * distanceMax, var5.z * distanceMax);

        return collisionRayTrace(world, x, y, z, start, end);
    }

    public boolean getIfOtherBlockIsSolid(World world, int x, int y, int z, Direction direction) {

        int[] vect = new int[3];
        vect[0] = x;
        vect[1] = y;
        vect[2] = z;
        direction.applyTo(vect, 1);

        IBlockState otherState = world.getBlockState(new BlockPos(vect[0], vect[1], vect[2]));
        Block block = otherState.getBlock();
        if (block == Blocks.AIR) return false;
        if (otherState.isOpaqueCube()) return true;

        return false;
    }

    public boolean nodeHasCache(IBlockAccess world, int x, int y, int z) {
        if (Utils.isRemote(world)) {
            TileEntity tileEntity = WorldCompat.getTileEntity(world, x, y, z);
            if (tileEntity != null && tileEntity instanceof SixNodeEntity)
                return ((SixNodeEntity) tileEntity).sixNodeCacheBlock != Blocks.AIR;
            else
                Utils.println("ASSERT B public boolean nodeHasCache(World world, int x, int y, int z) ");

        } else {
            SixNodeEntity tileEntity = (SixNodeEntity) WorldCompat.getTileEntity(world, x, y, z);
            SixNode sixNode = (SixNode) tileEntity.getNode();
            if (sixNode != null)
                return sixNode.sixNodeCacheBlock != Blocks.AIR;
            else
                Utils.println("ASSERT A public boolean nodeHasCache(World world, int x, int y, int z) ");
        }
        return false;
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess w, BlockPos pos) {

        TileEntity e = w.getTileEntity(pos);
        if (e == null) return 0;
        SixNodeEntity sne = (SixNodeEntity) e;
        Block b = sne.sixNodeCacheBlock;
        if (b == Blocks.AIR) return 0;
        // return b.getIcon(w, x, y, z, side);
        try {
            return b.getLightOpacity(b.getDefaultState());
        } catch (Exception e2) {
            return 255;
        }

    }

    public String getNodeUuid() {

        return "s";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World w, BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (hasVolume(w, x, y, z)) return super.getSelectedBoundingBox(state, w, pos);
        RayTraceResult col = collisionRayTrace(w, x, y, z, Minecraft.getMinecraft().player);
        double h = 0.2;
        double hn = 1 - h;

        double b = 0.02;
        double bn = 1 - 0.02;
        if (col != null) {
            // Utils.println(Direction.fromIntMinecraftSide(col.sideHit));
            switch (Direction.fromIntMinecraftSide(col.sideHit.getIndex())) {
                case XN:
                    return new AxisAlignedBB((double) x + b, (double) y, (double) z, (double) x + h, (double) y + 1, (double) z + 1);
                case XP:
                    return new AxisAlignedBB((double) x + hn, (double) y, (double) z, (double) x + bn, (double) y + 1, (double) z + 1);
                case YN:
                    return new AxisAlignedBB((double) x, (double) y + b, (double) z, (double) x + 1, (double) y + h, (double) z + 1);
                case YP:
                    return new AxisAlignedBB((double) x, (double) y + hn, (double) z, (double) x + 1, (double) y + bn, (double) z + 1);
                case ZN:
                    return new AxisAlignedBB((double) x, (double) y, (double) z + b, (double) x + 1, (double) y + 1, (double) z + h);
                case ZP:
                    return new AxisAlignedBB((double) x, (double) y, (double) z + hn, (double) x + 1, (double) y + 1, (double) z + bn);

            }
        }
        return new AxisAlignedBB(0.5, 0.5, 0.5, 0.5, 0.5, 0.5);//super.getSelectedBoundingBoxFromPool(w, x, y, z);
        // return new AxisAlignedBB((double)p_149633_2_ , (double)p_149633_3_ , (double)p_149633_4_ + this.minZ+0.2, (double)p_149633_2_ + this.maxX, (double)p_149633_3_ + this.maxY, (double)p_149633_4_ + this.maxZ);
        // return super.getSelectedBoundingBoxFromPool(w, x, y, z);
    }
}
