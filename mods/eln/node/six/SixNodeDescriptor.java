package mods.eln.node.six;

import mods.eln.Translator;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class SixNodeDescriptor extends GenericItemBlockUsingDamageDescriptor implements IItemRenderer{
	public Class<?> ElementClass,RenderClass;
	public SixNodeDescriptor(String name,
							 Class<?> ElementClass,Class<?> RenderClass) {
		super(name);
		this.ElementClass = ElementClass;
		this.RenderClass = RenderClass;
		
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		
		return true;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		
		if(type == ItemRenderType.INVENTORY)
			return false;
		return ! use2DIcon();
	}

	public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		
		return ! use2DIcon();
	}
	
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		if(getIcon() != null)
		UtilsClient.drawIcon(type,getIcon().getIconName().replace("eln:", "textures/blocks/")+".png");
	}
	
	public boolean hasVolume() {
		
		return false;
	}

	public boolean canBePlacedOnSide(EntityPlayer player,Coordonate c,Direction side){
		return canBePlacedOnSide(player,side);
	}
	
	public boolean canBePlacedOnSide(EntityPlayer player,Direction side)
	{
		if(placeDirection != null){
			for(Direction d : placeDirection){
				if(d == side) {
					return true;
				}
			}
			Utils.addChatMessage(player,Translator.translate("eln.core.tile.block.cantplaceside"));
			return false;
		}
		return true;
	}

	
	public void setGhostGroup(GhostGroup ghostGroup)
	{
		this.ghostGroup = ghostGroup;
	}
	
	protected GhostGroup ghostGroup = null;
	
	public boolean hasGhostGroup()
	{
		return ghostGroup != null;
	}
	public GhostGroup getGhostGroup(Direction side,LRDU front) {
		if(ghostGroup == null) return null;
		return ghostGroup.newRotate(side,front);
	}
	public int getGhostGroupUuid() {
		
		return -1;
	}
	
	public void setPlaceDirection(Direction d)
	{
		placeDirection = new Direction[]{d};
	}	
	public void setPlaceDirection(Direction []d)
	{
		placeDirection = d;
	}
	
	protected Direction[] placeDirection = null;
	public String checkCanPlace(Coordonate coord, Direction direction,LRDU front) {
		if(placeDirection != null){
			boolean ok = false;
			for(Direction d : placeDirection){
				if(d == direction) {
					ok = true;
					break;
				}
			}
			if(ok == false)
				return "Can not be placed at this side";
		}
		GhostGroup ghostGroup = getGhostGroup(direction,front);
		if(ghostGroup != null && ghostGroup.canBePloted(coord) == false) return Translator.translate("eln.core.tile.block.nonenoughspace");
		return null;
	}
	
	
}
