package mods.eln.node;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

public class SixNodeDescriptor extends GenericItemBlockUsingDamageDescriptor implements IItemRenderer{
	public Class ElementClass,RenderClass;
	public SixNodeDescriptor(String name,
							 Class ElementClass,Class RenderClass) {
		super(name);
		this.ElementClass = ElementClass;
		this.RenderClass = RenderClass;
		// TODO Auto-generated constructor stub
	}
	@Override
	public boolean handleRenderType(ItemStack item, ItemRenderType type) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
			ItemRendererHelper helper) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
		// TODO Auto-generated method stub
		
	}
	public boolean hasVolume() {
		// TODO Auto-generated method stub
		return false;
	}

	
	public boolean canBePlacedOnSide(Direction side)
	{
		if(placeDirection != null){
			boolean ok = false;
			for(Direction d : placeDirection){
				if(d == side) {
					return true;
				}
			}
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
		// TODO Auto-generated method stub
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
	
	Direction[] placeDirection = null;
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
		if(ghostGroup != null && ghostGroup.canBePloted(coord) == false) return "Not enough space for this block";
		return null;
	}
}
