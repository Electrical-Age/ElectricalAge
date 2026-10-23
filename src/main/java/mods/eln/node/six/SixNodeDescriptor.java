package mods.eln.node.six;

import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

import static mods.eln.i18n.I18N.tr;

public class SixNodeDescriptor extends GenericItemBlockUsingDamageDescriptor {
    public Class ElementClass, RenderClass;
    public VoltageLevelColor voltageLevelColor = VoltageLevelColor.None;

    public SixNodeDescriptor(String name, Class ElementClass, Class RenderClass) {
        super(name);
        this.ElementClass = ElementClass;
        this.RenderClass = RenderClass;
    }

    public SixNodeDescriptor(String name, Class ElementClass, Class RenderClass, String iconName) {
        super(name, iconName);
        this.ElementClass = ElementClass;
        this.RenderClass = RenderClass;
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)
// 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    public boolean hasVolume() {
        return false;
    }

    public boolean canBePlacedOnSide(Player player, Coordonate c, Direction side) {
        return canBePlacedOnSide(player, side);
    }

    public boolean canBePlacedOnSide(Player player, Direction side) {
        if (placeDirection != null) {
            for (Direction d : placeDirection) {
                if (d == side)
                    return true;
            }
            Utils.sendMessage(player, tr("You can't place this block at this side"));
            return false;
        }
        return true;
    }


    public void setGhostGroup(GhostGroup ghostGroup) {
        this.ghostGroup = ghostGroup;
    }

    protected GhostGroup ghostGroup = null;

    public boolean hasGhostGroup() {
        return ghostGroup != null;
    }

    public GhostGroup getGhostGroup(Direction side, LRDU front) {
        if (ghostGroup == null) return null;
        return ghostGroup.newRotate(side, front);
    }

    public int getGhostGroupUuid() {
        return -1;
    }

    /** Faces this device may be placed on (null = any). */
    @javax.annotation.Nullable
    public Direction[] getPlaceDirection() {
        return placeDirection;
    }

    public void setPlaceDirection(Direction d) {
        placeDirection = new Direction[]{d};
    }

    public void setPlaceDirection(Direction[] d) {
        placeDirection = d;
    }

    protected Direction[] placeDirection = null;

    public String checkCanPlace(Coordonate coord, Direction direction, LRDU front) {
        if (placeDirection != null) {
            boolean ok = false;
            for (Direction d : placeDirection) {
                if (d == direction) {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                return tr("You can't place this block at this side");
        }
        GhostGroup ghostGroup = getGhostGroup(direction, front);
        if (ghostGroup != null && !ghostGroup.canBePloted(coord))
            return tr("Not enough space for this block");
        return null;
    }

    public LRDU getFrontFromPlace(Direction side, Player player) {
        switch (side) {
            case YN:
            case YP:
                Direction viewDirection = Utils.entityLivingHorizontalViewDirection(player);
                LRDU front = side.getLRDUGoingTo(viewDirection);
                return side == Direction.YN ? front : front.inverse();

            default:
                return LRDU.Up;
        }
    }
}
