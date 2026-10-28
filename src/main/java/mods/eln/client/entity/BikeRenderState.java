package mods.eln.client.entity;

import mods.eln.bike.ElectricBike;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** What the bike's renderer needs for a frame (BikeRenderer.extractRenderState). */
public class BikeRenderState extends EntityRenderState {
    public float yRot, wheel, crank, stand, hurtTime, damageTime;
    public int hurtDir;
    public ElectricBike.Mode mode = ElectricBike.Mode.PARKED;
}
