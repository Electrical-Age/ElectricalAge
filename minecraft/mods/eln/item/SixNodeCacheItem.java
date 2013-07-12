package mods.eln.item;

import net.minecraft.block.Block;
import net.minecraft.util.Icon;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;

public class SixNodeCacheItem extends GenericItemUsingDamageDescriptor{
	public static SixNodeCacheItem[] map = new SixNodeCacheItem[128];
	public Obj3D model;
	public int mapIndex;
	public SixNodeCacheItem(
			String name,
			Obj3D model,
			int mapIndex
			) {
		super(name);
		this.model = model;
		this.mapIndex = mapIndex;
		map[mapIndex] = this;
	}
	
	
	public void draw()
	{
		if(model != null)model.draw("furnace");
		
	}
}
