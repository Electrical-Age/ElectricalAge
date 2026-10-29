package mods.eln.client.entity;

import mods.eln.bike.ElectricBike;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** What the bike's renderer needs for a frame (BikeRenderer.extractRenderState). */
public class BikeRenderState extends EntityRenderState {
    public float yRot, wheel, crank, stand, hurtTime, damageTime;
    public int hurtDir;
    public ElectricBike.Mode mode = ElectricBike.Mode.PARKED;
    /** The paint (BikeColors, per group); and which group this copy draws (BikeRenderer draws one copy per group). */
    public int[] colors = new int[mods.eln.bike.BikeColors.GROUPS];
    public BikeModel.Group group = BikeModel.Group.BASE;

    /** A copy for drawing one group. */
    BikeRenderState forGroup(BikeModel.Group g) {
        BikeRenderState c = new BikeRenderState();
        c.yRot = yRot;
        c.wheel = wheel;
        c.crank = crank;
        c.stand = stand;
        c.hurtTime = hurtTime;
        c.damageTime = damageTime;
        c.hurtDir = hurtDir;
        c.mode = mode;
        c.colors = colors;
        c.group = g;
        c.lightCoords = lightCoords;
        c.outlineColor = outlineColor;
        return c;
    }
}
