package mods.eln.bike;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * The bikes: one entity class (ElectricBike) and one item class, told apart by type. The electric bike (Baughn
 * 2026-10-28) and the terrain bike (Baughn 2026-10-29: fancier and much dearer): it rides up a whole block, jumps 2.5,
 * and its motor draws 150 W at the same top speed; fat knobbly tyres, a suspension fork, a sturdier frame, another
 * colour. What both share (speed, steering, averaging, crashes, tip-over) is in BikePhysics; what differs is here, the
 * terrain bike's from the synced config ([bike] terrain*).
 */
public enum BikeType {
    ELECTRIC("electric_bike", new int[]{0x3466BC, 0x3466BC, 0x4F5F6D, 0x3B2D26}),
    TERRAIN("terrain_bike", new int[]{0xC8642A, 0x3A3E44, 0x4F5F6D, 0x2A2A2C});

    /** The terrain bike's tunables (ElnConfig): a whole-block step, a 2.5-block jump, a 150 W motor. */
    public static volatile float terrainStep = 1.0f;
    public static volatile double terrainJumpVy = BikePhysics.velocityForPeak(2.5), terrainWatts = 150;

    public final String id;
    /** Paint defaults per BikeColors group, as tints over the neutral texture. */
    final int[] defaults;

    BikeType(String id, int[] defaults) {
        this.id = id;
        this.defaults = defaults;
    }

    public float stepHeight() {
        return this == TERRAIN ? terrainStep : BikePhysics.STEP_HEIGHT;
    }

    /** Upward speed of a full-charge jump (b/t). */
    public double jumpVy() {
        return this == TERRAIN ? terrainJumpVy : BikePhysics.JUMP_VY;
    }

    /** The motor's draw at full speed (W). */
    public double motorWatts() {
        return this == TERRAIN ? terrainWatts : BikePhysics.MOTOR_WATTS;
    }

    public int defaultColor(int group) {
        return defaults[group];
    }

    /** "Not a motorcycle" (Baughn); "Still not a motorcycle". */
    public String tooltipKey() {
        return "item.eln." + id + ".tooltip";
    }

    public Item item() {
        return this == TERRAIN ? mods.eln.registry.ElnRegistry.TERRAIN_BIKE.get() : mods.eln.registry.ElnRegistry.ELECTRIC_BIKE.get();
    }

    public EntityType<ElectricBike> entityType() {
        return this == TERRAIN ? mods.eln.registry.ElnRegistry.TERRAIN_BIKE_ENTITY.get() : mods.eln.registry.ElnRegistry.ELECTRIC_BIKE_ENTITY.get();
    }

    public static BikeType of(EntityType<?> t) {
        return t == mods.eln.registry.ElnRegistry.TERRAIN_BIKE_ENTITY.get() ? TERRAIN : ELECTRIC;
    }
}
