package mods.eln.node;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import mods.eln.CommonProxy;
import mods.eln.Eln;
import mods.eln.generic.GenericItemBlockUsingDamage;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.item.LampDescriptor;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper;

public class SixNodeItem extends GenericItemBlockUsingDamage<SixNodeDescriptor>  implements IItemRenderer{

	
	
	public SixNodeItem(int id) {
		super(id);
		setHasSubtypes(true);
		setUnlocalizedName("SixNodeItem");
	}
	
	@Override
	public int getMetadata (int damageValue) {
		return damageValue;
	}
	
	
	

    /** COPYPAST
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
	/*
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        int var11 = par3World.getBlockId(par4, par5, par6);

        if (var11 == Block.snow.blockID)
        {
            par7 = 1;
        }
        else if (var11 != Block.vine.blockID && var11 != Block.tallGrass.blockID && var11 != Block.deadBush.blockID
                && (Block.blocksList[var11] == null || !Block.blocksList[var11].isBlockReplaceable(par3World, par4, par5, par6)))
        {
            if (par7 == 0)
            {
                --par5;
            }

            if (par7 == 1)
            {
                ++par5;
            }

            if (par7 == 2)
            {
                --par6;
            }

            if (par7 == 3)
            {
                ++par6;
            }

            if (par7 == 4)
            {
                --par4;
            }

            if (par7 == 5)
            {
                ++par4;
            }
        }

        if (par1ItemStack.stackSize == 0)
        {
            return false;
        }
        else if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else if (par5 == 255 && Block.blocksList[getBlockID()].blockMaterial.isSolid())
        {
            return false;
        }
        else// // // //TOCOMMENT if (par3World.canPlaceEntityOnSide(this.getBlockID(), par4, par5, par6, false, par7, par2EntityPlayer))
        {
            Block var12 = Block.blocksList[this.getBlockID()];
            int var13 = this.getMetadata(par1ItemStack.getItemDamage());
            int var14 = Block.blocksList[this.getBlockID()].func_85104_a(par3World, par4, par5, par6, par7, par8, par9, par10, var13);

            if (placeBlockAt(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10, var14))
            {
                par3World.playSoundEffect((double)((float)par4 + 0.5F), (double)((float)par5 + 0.5F), (double)((float)par6 + 0.5F), var12.stepSound.getPlaceSound(), (var12.stepSound.getVolume() + 1.0F) / 2.0F, var12.stepSound.getPitch() * 0.8F);
                --par1ItemStack.stackSize;
            }

            return true;
        }
       /* else//TOCOMMENT
        {
            return false;
        }*/
  //  }
    /**
     * Callback for item usage. If the item does something special on right clicking, he will have one of those. Return
     * True if something happen and false if it don't. This is for ITEMS, not BLOCKS
     */
    public boolean onItemUse(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, World par3World, int par4, int par5, int par6, int par7, float par8, float par9, float par10)
    {
        int i1 = par3World.getBlockId(par4, par5, par6);

        if (i1 == Block.snow.blockID && (par3World.getBlockMetadata(par4, par5, par6) & 7) < 1)
        {
            par7 = 1;
        }
        else if (i1 != Block.vine.blockID && i1 != Block.tallGrass.blockID && i1 != Block.deadBush.blockID
                && (Block.blocksList[i1] == null || !Block.blocksList[i1].isBlockReplaceable(par3World, par4, par5, par6)))
        {
            if (par7 == 0)
            {
                --par5;
            }

            if (par7 == 1)
            {
                ++par5;
            }

            if (par7 == 2)
            {
                --par6;
            }

            if (par7 == 3)
            {
                ++par6;
            }

            if (par7 == 4)
            {
                --par4;
            }

            if (par7 == 5)
            {
                ++par4;
            }
        }

        if (par1ItemStack.stackSize == 0)
        {
            return false;
        }
        else if (!par2EntityPlayer.canPlayerEdit(par4, par5, par6, par7, par1ItemStack))
        {
            return false;
        }
        else if (par5 == 255 && Block.blocksList[this.getBlockID()].blockMaterial.isSolid())
        {
            return false;
        }
        else//if (par3World.canPlaceEntityOnSide(this.blockID, par4, par5, par6, false, par7, par2EntityPlayer, par1ItemStack))
        {
            Block block = Block.blocksList[this.getBlockID()];
            int j1 = this.getMetadata(par1ItemStack.getItemDamage());
            int k1 = Block.blocksList[this.getBlockID()].onBlockPlaced(par3World, par4, par5, par6, par7, par8, par9, par10, j1);

            if (placeBlockAt(par1ItemStack, par2EntityPlayer, par3World, par4, par5, par6, par7, par8, par9, par10, k1))
            {
                par3World.playSoundEffect((double)((float)par4 + 0.5F), (double)((float)par5 + 0.5F), (double)((float)par6 + 0.5F), block.stepSound.getPlaceSound(), (block.stepSound.getVolume() + 1.0F) / 2.0F, block.stepSound.getPitch() * 0.8F);
                --par1ItemStack.stackSize;
            }

            return true;
        }
    /*    else
        {
            return false;
        }*/
    }

    /**
     * Returns true if the given ItemBlock can be placed on the given side of the given block position.
     */
    public boolean canPlaceItemBlockOnSide(World par1World, int x, int y, int z, int par5, EntityPlayer par6EntityPlayer, ItemStack par7ItemStack)
    {
    	int[] vect = new int[]{x,y,z};
    	Direction.fromIntMinecraftSide(par5).applyTo(vect, 1);
    	SixNodeDescriptor descriptor = getDescriptor(par7ItemStack);
    	if(descriptor.canBePlacedOnSide(Direction.fromIntMinecraftSide(par5).getInverse()) == false)
		{
    		par6EntityPlayer.addChatMessage("You can't place this block at this side");
			return false;
		}
    	if(par1World.getBlockId(vect[0],vect[1],vect[2]) == Eln.sixNodeBlock.blockID) return true;
    	if(super.canPlaceItemBlockOnSide(par1World, x, y, z, par5, par6EntityPlayer, par7ItemStack))return true;  	
    	
    	return false;
    }
    
	
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata)
    {
    
    	if(world.isRemote)return false;
    	
    	Direction direction = Direction.fromIntMinecraftSide(side).getInverse();    	
    	int blockID = world.getBlockId(x, y, z);
    	if(blockID == 0)   		
    	{	
    		blockID = this.getBlockID();
    		SixNodeBlock block = (SixNodeBlock)Block.blocksList[blockID];

	

    	   if(block.getIfOtherBlockIsSolid(world,x,y,z,direction))
    	   {
			   	try {

	               	SixNode sixNode = (SixNode) NodeManager.UUIDToClass[getBlockID()].getConstructor().newInstance();
	               	sixNode.onBlockPlacedBy(new Coordonate(x, y, z,world),direction,player,stack);
	               	sixNode.createSubBlock(stack, direction);
	               	
	               	world.setBlock(x, y, z, blockID, metadata,0x03);
	               	block.getIfOtherBlockIsSolid(world,x,y,z,direction);
	               	block.onBlockPlacedBy(world, x, y, z,Direction.fromIntMinecraftSide(side).getInverse(), player,metadata);
	               	return true;
			   		
	       		} catch (InstantiationException e) {
	   				// TODO Auto-generated catch block
	   				e.printStackTrace();
	   			} catch (IllegalAccessException e) {
	   				// TODO Auto-generated catch block
	   				e.printStackTrace();
	   			} catch (IllegalArgumentException e) {
	   				// TODO Auto-generated catch block
	   				e.printStackTrace();
	   			} catch (InvocationTargetException e) {
	   				// TODO Auto-generated catch block
	   				e.printStackTrace();
	   			} catch (NoSuchMethodException e) {
	   				// TODO Auto-generated catch block
	   				e.printStackTrace();
	   			} catch (SecurityException e) {
	   				// TODO Auto-generated catch block
	   				e.printStackTrace();
	   			}	    		
	       	}
    	}
    	else if(blockID == this.getBlockID())
    	{
    		SixNodeBlock block = (SixNodeBlock)Block.blocksList[blockID];
    	
			SixNode sixNode = (SixNode) ((SixNodeEntity) world.getBlockTileEntity(x, y, z)).getNode();
           	if(sixNode.getSideEnable(direction) == false && block.getIfOtherBlockIsSolid(world,x,y,z,direction))
           	{		
	    		sixNode.createSubBlock(stack, direction);
	    		block.onBlockPlacedBy(world, x, y, z,Direction.fromIntMinecraftSide(side).getInverse(), player,metadata);
	    		return true;
			}

    	}
    	return false;
    }
    /*
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4)
    {
    	try {
			SixNodeElement.getClassFromMetadata(itemStack.getItemDamage()).getMethod("staticInformation", ItemStack.class,EntityPlayer.class,List.class,boolean.class).invoke(null, itemStack,entityPlayer,list,par4);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }*/
    /*
    public static IndexResolver[] itemData = new IndexResolver[256];
    */
 /*   public static ItemData getItemData(int itemDamage)
    {
    	return (ItemData) itemData[itemDamage & 0xFF].get((itemDamage>>8) & 0xFF);
    }
    public static ItemData getItemData(ItemStack itemStack)
    {
    	return getItemData(itemStack.getItemDamage());
    }*/
    /*
	@Override
	public String getItemNameIS(ItemStack itemstack) {
		return getItemName() + ".SixNodeItem" + getItemData(itemstack).name;
	}
	
    @Override
    @SideOnly(Side.CLIENT)
    public int getIconFromDamage(int itemDamage) {
    	return getItemData(itemDamage).iconId;
    }
    @Override
    public String getTextureFile() {
    	// TODO Auto-generated method stub
    	return CommonProxy.ITEMS_PNG;
    }*/

	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		SixNodeDescriptor desc = getDescriptor(item);
		if(desc == null)
			return false;
		return getDescriptor(item).handleRenderType(item, type);
	}

	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return getDescriptor(item).shouldUseRenderHelper(type, item, helper);
	}

	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		switch(type)
		{
		case ENTITY:
			GL11.glRotatef(90, 0, 0, 1);
			//GL11.glTranslatef(0, 1, 0);
			break;
		case EQUIPPED:
			GL11.glRotatef(180, 0, 1, 0);
			GL11.glTranslatef(-0.70f, 1, -0.7f);
			break;
		case FIRST_PERSON_MAP:
			//GL11.glTranslatef(0, 1, 0);
			break;
		case INVENTORY:
			GL11.glRotatef(-90, 0, 1, 0);
			break;
		default:
			break;
		}
		//GL11.glTranslatef(0, 1, 0);
		getDescriptor(item).renderItem(type, item, data);
	}	
    
    
    /*
    
    
    
    
	Hashtable<Integer,GenericItemBlockUsingDamageElement> subItemList = new Hashtable<Integer,GenericItemBlockUsingDamageElement>();
	
	GenericItemBlockUsingDamageElement defaultElement = null;
	

	public void setDefaultElement(GenericItemBlockUsingDamageElement element)
	{
		defaultElement = element;
	}
	
	
	public void addElement(int damage,GenericItemBlockUsingDamageElement element)
	{
		subItemList.put(damage,element);
		ItemStack stack = new ItemStack(this, 1, damage);
		LanguageRegistry.addName(stack,element.name);

	}
	
	public GenericItemBlockUsingDamageElement getElement(int damage)
	{
		return subItemList.get(damage);
	}
	
	public GenericItemBlockUsingDamageElement getElement(ItemStack itemStack)
	{
		if(itemStack.getItem() != this) return defaultElement;
		return getElement(itemStack.getItemDamage());
	}
	
	 
	@Override
	@SideOnly(Side.CLIENT)
	public int getIconFromDamage(int damage) {
		return getElement(damage).iconId;
		
	}
	@Override
	public String getTextureFile () {
		return CommonProxy.ITEMS_PNG;
	}
	@Override
	public String getItemNameIS(ItemStack itemstack) {
		return getItemName() + "." + getElement(itemstack).name;
	}
	
    @SideOnly(Side.CLIENT)
    public void getSubItems(int itemID, CreativeTabs tabs, List list){
	// You can also take a more direct approach and do each one individual but I prefer the lazy / right way
    	for(Entry<Integer, GenericItemBlockUsingDamageElement> entry : subItemList.entrySet()) 
    	{
	        list.add(new ItemStack(itemID, 1, entry.getKey()));
	    }
	}
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4)
    {
    	getElement(itemStack).addInformation(itemStack, entityPlayer, list, par4);
    }*/

}
