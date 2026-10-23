package mods.eln.ore;

/**
 * An EA ore block (1.12: one eln:ore block with a TYPE property; 26.x: one block per ore, plain Block with stone-like
 * properties set at registration: hardness 3, resistance 5, pickaxe, drops itself).
 */
public class OreBlock extends net.minecraft.world.level.block.Block {
    public final OreDescriptor descriptor;

    public OreBlock(Properties properties, OreDescriptor descriptor) {
        super(properties);
        this.descriptor = descriptor;
    }
}
