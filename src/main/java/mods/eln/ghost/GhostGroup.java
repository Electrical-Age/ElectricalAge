package mods.eln.ghost;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.ArrayList;

public class GhostGroup {

    public GhostGroup() {
    }

    class GhostGroupElement {

        int x, y, z;
        Block block;
        int meta;

        public GhostGroupElement(int x, int y, int z, Block block, int meta) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
        }
    }

    ArrayList<GhostGroupElement> elementList = new ArrayList<GhostGroupElement>();

    public void addElement(int x, int y, int z) {
        elementList.add(new GhostGroupElement(x, y, z, Eln.ghostBlock, GhostBlock.tCube));
    }

    public void addElement(int x, int y, int z, Block block, int meta) {
        elementList.add(new GhostGroupElement(x, y, z, block, meta));
    }

    public void removeElement(int x, int y, int z) {
        java.util.Iterator<GhostGroupElement> i = elementList.iterator();
        GhostGroupElement g;

        while (i.hasNext()) {
            g = i.next();
            if (g.x == x && g.y == y && g.z == z) {
                i.remove();
            }
        }
    }

    public void addRectangle(int x1, int x2, int y1, int y2, int z1, int z2) {
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    addElement(x, y, z);
                }
            }
        }
    }

    public boolean canBePloted(Coordonate c) {
        return canBePloted(c.world(), c.x, c.y, c.z);
    }

    public boolean canBePloted(World world, int x, int y, int z) {
        for (GhostGroupElement element : elementList) {
            if (false == Eln.ghostManager.canCreateGhostAt(world, x + element.x, y + element.y, z + element.z))
                return false;
        }
        return true;
    }

    public boolean plot(Coordonate coordonate, Coordonate observerCoordonate, int UUID) {
        if (canBePloted(coordonate.world(), coordonate.x, coordonate.y, coordonate.z) == false) return false;

        for (GhostGroupElement element : elementList) {
            Eln.ghostManager.createGhost(coordonate.newWithOffset(element.x, element.y, element.z), observerCoordonate, UUID, element.block, element.meta);
        }
        return true;
    }

    public void erase(Coordonate observerCoordonate) {
        Eln.ghostManager.removeGhostAndBlockWithObserver(observerCoordonate);
    }

    public void erase(Coordonate observerCoordonate, int uuid) {
        Eln.ghostManager.removeGhostAndBlockWithObserver(observerCoordonate, uuid);
    }

    public void eraseGeo(Coordonate coordonate) {
        for (GhostGroupElement element : elementList) {
            Eln.ghostManager.removeGhostAndBlock(coordonate.newWithOffset(element.x, element.y, element.z));
        }
    }

    public GhostGroup newRotate(Direction dir) {
        GhostGroup other = new GhostGroup();
        for (GhostGroupElement element : this.elementList) {
            int x, y, z;
            switch (dir) {
                case XN:
                    x = element.x;
                    y = element.y;
                    z = element.z;
                    break;
                case XP:
                    x = -element.x;
                    y = element.y;
                    z = -element.z;
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
                    x = -element.y;
                    y = element.x;
                    z = element.z;
                    break;
                case YP:
                    x = element.y;
                    y = -element.x;
                    z = element.z;
                    break;
            }
            other.addElement(x, y, z, element.block, element.meta);
        }

        return other;
    }

    public GhostGroup newRotate(Direction dir, LRDU front) {
        GhostGroup g = newRotate(dir);
        return g;
    }

    public int size() {
        return elementList.size();
    }

	/*public void eraseWithNoNotification(Coordonate observerCoordonate) {
        Eln.ghostManager.removeGhostAndBlockWithObserver(observerCoordonate);
	}
	*/
}
