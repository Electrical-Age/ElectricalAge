package mods.eln.node.six;

import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import org.lwjgl.opengl.GL11;

public class SixNodeItem extends GenericItemBlockUsingDamage<SixNodeDescriptor> implements IItemRenderer {

    public SixNodeItem(Block b) {
        super(b);
        setHasSubtypes(true);
        setUnlocalizedName("SixNodeItem");
    }

    @Override
    public int getMetadata(int damageValue) {
        return damageValue;
    }

    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10) {
        Block block = par3World.getBlock(par4, par5, par6);

        if ((block == Blocks.snow_layer) && ((par3World.getBlockMetadata(par4, par5, par6) & 0x7) < 1)) {
            par7 = 1;
        } else if ((block != Blocks.vine) && (block != Blocks.tallgrass) && (block != Blocks.deadbush) && (!block.isReplaceable(par3World, par4, par5, par6))) {
            if (par7 == 0)
                par5--;

            if (par7 == 1)
                par5++;

            if (par7 == 2)
                par6--;

            if (par7 == 3)
                par6++;

            if (par7 == 4)
                par4--;

            if (par7 == 5)
                par4++;
        }

        if (par1ItemStack.stackSize == 0)
            return false;
        if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
            return false;
        if ((par5 == 255) && (this.field_150939_a.getMaterial().isSolid()))
            return false;

        int i1 = getMetadata(par1ItemStack.getItemDamage());
        int j1 = this.field_150939_a.onBlockPlaced(par3World, par4, par5, par6, par7, par8, par9, par10, i1);

        if (placeBlockAt(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10, j1)) {
            par3World.playSoundEffect(par4 + 0.5F, par5 + 0.5F, par6 + 0.5F, this.field_150939_a.stepSound.func_150496_b(), (this.field_150939_a.stepSound.getVolume() + 1.0F) / 2.0F, this.field_150939_a.stepSound.getPitch() * 0.8F);
            par1ItemStack.stackSize -= 1;
        }

        return true;
    }

    /**
     * Returns true if the given ItemBlock can be placed on the given side of the given block position.
     */

    // func_150936_a <= canPlaceItemBlockOnSide
    @Override
    public boolean func_150936_a(World par1World, int x, int y, int z, int par5, EntityPlayer par6EntityPlayer, ItemStack par7ItemStack) {
        if (!isStackValidToPlace(par7ItemStack))
            return false;
        int[] vect = new int[]{x, y, z};
        Direction.fromIntMinecraftSide(par5).applyTo(vect, 1);
        SixNodeDescriptor descriptor = getDescriptor(par7ItemStack);
        if (descriptor.canBePlacedOnSide(par6EntityPlayer, new Coordonate(x, y, z, par1World), Direction.fromIntMinecraftSide(par5).getInverse()) == false) {
            return false;
        }
        if (par1World.getBlock(vect[0], vect[1], vect[2]) == Eln.sixNodeBlock)
            return true;
        if (super.func_150936_a(par1World, x, y, z, par5, par6EntityPlayer, par7ItemStack))
            return true;

        return false;
    }

    public boolean isStackValidToPlace(ItemStack stack) {
        SixNodeDescriptor descriptor = getDescriptor(stack);
        return descriptor != null;
    }

    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        if (world.isRemote)
            return false;
        if (!isStackValidToPlace(stack))
            return false;

        Direction direction = Direction.fromIntMinecraftSide(side).getInverse();
        Block blockOld = world.getBlock(x, y, z);
        SixNodeBlock block = (SixNodeBlock) Block.getBlockFromItem(this);
        if (blockOld == Blocks.air || blockOld.isReplaceable(world, x, y, z)) {
            // blockID = this.getBlockID();

            Coordonate coord = new Coordonate(x, y, z, world);
            SixNodeDescriptor descriptor = getDescriptor(stack);

            String error;
            if ((error = descriptor.checkCanPlace(coord, direction, LRDU.Up)) != null) {
                Utils.addChatMessage(player, error);
                return false;
            }

            GhostGroup ghostgroup = descriptor.getGhostGroup(direction, LRDU.Up);
            if (ghostgroup != null)
                ghostgroup.plot(coord, coord, descriptor.getGhostGroupUuid());

            if (block.getIfOtherBlockIsSolid(world, x, y, z, direction)) {

                SixNode sixNode = new SixNode();
                sixNode.onBlockPlacedBy(new Coordonate(x, y, z, world), direction, player, stack);
                sixNode.createSubBlock(stack, direction, player);

                world.setBlock(x, y, z, block, metadata, 0x03);
                block.getIfOtherBlockIsSolid(world, x, y, z, direction);
                block.onBlockPlacedBy(world, x, y, z, Direction.fromIntMinecraftSide(side).getInverse(), player, metadata);
                return true;

            }
        } else if (blockOld == block) {

            SixNode sixNode = (SixNode) ((SixNodeEntity) world.getTileEntity(x, y, z)).getNode();
            if (sixNode == null) {
                world.setBlockToAir(x, y, z);
                return false;
            }
            if (sixNode.getSideEnable(direction) == false && block.getIfOtherBlockIsSolid(world, x, y, z, direction)) {
                sixNode.createSubBlock(stack, direction, player);
                block.onBlockPlacedBy(world, x, y, z, Direction.fromIntMinecraftSide(side).getInverse(), player, metadata);
                return true;
            }

        } else {
            SixNode sixNode = (SixNode) ((SixNodeEntity) world.getTileEntity(x, y, z)).getNode();
            if (sixNode == null) {
                world.setBlockToAir(x, y, z);
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        if (getDescriptor(item) == null)
            return false;
        return getDescriptor(item).handleRenderType(item, type);
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (!isStackValidToPlace(item))
            return false;
        return getDescriptor(item).shouldUseRenderHelper(type, item, helper);
    }

    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        if (!isStackValidToPlace(item))
            return false;
        return getDescriptor(item).shouldUseRenderHelperEln(type, item, helper);
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (!isStackValidToPlace(item))
            return;

        Minecraft.getMinecraft().mcProfiler.startSection("SixNodeItem");
        if (shouldUseRenderHelperEln(type, item, null)) {
            switch (type) {

                case ENTITY:
                    GL11.glRotatef(90, 0, 0, 1);
                    // GL11.glTranslatef(0, 1, 0);
                    break;

                case EQUIPPED_FIRST_PERSON:
                    GL11.glRotatef(160, 0, 1, 0);
                    GL11.glTranslatef(-0.70f, 1, -0.7f);
                    GL11.glScalef(1.8f, 1.8f, 1.8f);
                    GL11.glRotatef(-90, 1, 0, 0);
                    break;
                case EQUIPPED:
                    GL11.glRotatef(180, 0, 1, 0);
                    GL11.glTranslatef(-0.70f, 1, -0.7f);
                    GL11.glScalef(1.5f, 1.5f, 1.5f);
                    break;
                case FIRST_PERSON_MAP:
                    // GL11.glTranslatef(0, 1, 0);
                    break;
                case INVENTORY:
                    GL11.glRotatef(-90, 0, 1, 0);
                    GL11.glRotatef(-90, 1, 0, 0);
                    break;
                default:
                    break;
            }
        }
        // GL11.glTranslatef(0, 1, 0);
        getDescriptor(item).renderItem(type, item, data);
        Minecraft.getMinecraft().mcProfiler.endSection();
    }
}
