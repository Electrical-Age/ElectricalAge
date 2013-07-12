package mods.eln.ghost;

import java.util.ArrayList;

import com.google.common.base.CaseFormat;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import net.minecraft.world.World;


public class GhostGroup {

	public GhostGroup() {
		// TODO Auto-generated constructor stub
	}

	class GhostGroupElement{
		public GhostGroupElement(int x,int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
			// TODO Auto-generated constructor stub
		}
		int x,y,z;
	}
	ArrayList<GhostGroupElement> elementList = new ArrayList<GhostGroupElement>();

	
	public void addElement(int x,int y, int z)
	{
		elementList.add(new GhostGroupElement(x, y, z));
	}
	public void addRectangle(int x1,int x2,int y1,int y2,int z1,int z2)
	{
		for(int x = x1;x <= x2;x++)
		{
			for(int y = y1;y <= y2;y++)
			{
				for(int z = z1;z <= z2;z++)
				{
					addElement(x, y, z);
				}
				
			}
			
		}
	}
	
	public boolean canBePloted(World world,int x,int y, int z)
	{
		for(GhostGroupElement element : elementList)
		{
			if(false == Eln.ghostManager.canCreateGhostAt(world,x + element.x,y + element.y,z + element.z)) return false;
		}
		return true;
	}
	
	public boolean plot(Coordonate coordonate,Coordonate observerCoordonate,int UUID)
	{
		if(canBePloted(coordonate.world(),coordonate.x,coordonate.y,coordonate.z) == false) return false;

		for(GhostGroupElement element : elementList)
		{
			Eln.ghostManager.createGhost(coordonate.newWithOffset(element.x,element.y,element.z),observerCoordonate,UUID);
		}
		return true;
	}
	
	public void erase(Coordonate observerCoordonate)
	{
		Eln.ghostManager.removeGhostAndBlockWithObserver(observerCoordonate);
	}
	
	public GhostGroup newRotate(Direction dir)
	{
		GhostGroup other = new GhostGroup();
		for(GhostGroupElement element : this.elementList)
		{
			int x,y,z;
			switch (dir) {
			case XN:
				x = element.x;
				y = element.y;
				z = element.z;
				break;
			case XP:
				x = -element.x;
				y = -element.y;
				z = element.z;
				break;
			case ZN:
				x = -element.z;
				y = element.y;
				z = element.x;
				break;
			case ZP:
				x = element.z;
				y = element.y;
				z = -element.x;
				break;
			default:
			case YN:
			case YP:
				x = 0; y = 0; z = 0;
				break;

			}
			other.addElement(x, y, z);
		}
		

		return other;
	}
	
	
	
}
