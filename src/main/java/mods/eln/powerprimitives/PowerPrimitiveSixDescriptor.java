package mods.eln.powerprimitives;

import mods.eln.gl.GL11;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;

/**
 * The six-node Power Capacitor and Power Inductor (PowerElectricPrimitives models): placed anywhere, front turned a
 * quarter left of the player's, the same item pose, drawn by PowerPrimitiveSixRender. A subclass gives its model's
 * draw, its short-lead draw, the render's turn, and its own modelTurn and bodyBounds.
 */
public abstract class PowerPrimitiveSixDescriptor extends SixNodeDescriptor {

    protected PowerPrimitiveSixDescriptor(String name, Class<?> elementClass) {
        super(name, elementClass, null);
        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    /** The whole model, centred on its face. */
    protected abstract void draw();

    /** Off centre: the core, its base and leads shortened to its pins. */
    protected abstract void drawShort();

    /** The render's turn to the element's front, before draw or drawShort. */
    protected abstract void turnToFront(LRDU front);

    @Override
    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        GL11.glTranslatef(0.0f, 0.0f, -0.2f);
        GL11.glScalef(1.25f, 1.25f, 1.25f);
        GL11.glRotatef(-90.f, 0.f, 1.f, 0.f);
        draw();
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).left();
    }

    /** Small: placed anywhere on a face of a voxel block, in line on a cable, its leads shortened (Baughn, batch 3). */
    @Override
    public boolean placeAnywhere() {
        return true;
    }

    @Override
    public void drawPlaced(Direction side, LRDU front) {
        LRDU turn = modelTurn(side, front);
        if (turn != null) turn.glRotateOnX();
        drawShort();
    }
}
