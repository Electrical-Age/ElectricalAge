package mods.eln.transparentnode.energyconverter;

import mods.eln.node.transparent.TransparentNode.FrontType;
import mods.eln.node.transparent.TransparentNodeBlock;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The EA -> Forge Energy exporter (1.12 Wp11 registerEnergyConverter). Until 26.x it was a simple node with its own
 * block entity; now a transparent-node device like the other machines (it burns out in place and is repaired), with
 * its 1.12 block names and its baked block model (FRONT state) kept.
 */
public class EnergyConverterElnToOtherDescriptor extends TransparentNodeDescriptor {

    /** The 1.12 descriptor key (EnergyConverterElnToOtherLVU...), as saved by the simple-node exporter. */
    public final String legacyKey;
    public final ElnDescriptor eln;
    public final Ic2Descriptor ic2;

    public EnergyConverterElnToOtherDescriptor(String name, String legacyKey, ElnDescriptor eln, Ic2Descriptor ic2) {
        super(name, EnergyConverterElnToOtherElement.class, EnergyConverterElnToOtherRender.class);
        this.legacyKey = legacyKey;
        this.eln = eln;
        this.ic2 = ic2;
        cableTerminals = mods.eln.cable.CableTerminals.FRONT; // terminal plate on the EA side (model)
    }

    void applyTo(EnergyConverterElnToOtherElement element) {
        element.inStdVoltage = eln.nominalU;
        element.inPowerMax = eln.maxP;
        element.energyBufferMax = eln.maxP * 2;
    }

    /** As the simple-node block: placed facing away from the player, any of the six directions. */
    @Override
    public FrontType getFrontType() {
        return FrontType.PlayerView;
    }

    /** A full block: stands anywhere (the simple-node block had no floor rule; a migrated one may hang in the air). */
    @Override
    public boolean mustHaveFloor() {
        return false;
    }

    @Override
    public BlockBehaviour.Properties blockProperties(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.ICE).strength(0.5f).sound(SoundType.GLASS) // 1.12 Material.PACKED_ICE
            .noOcclusion(); // the condenser model doesn't fill the cube
    }

    @Override
    public TransparentNodeBlock newBlock(BlockBehaviour.Properties p) {
        return new EnergyConverterElnToOtherBlock(p, this);
    }

    public static class ElnDescriptor {
        public double nominalU;
        public double maxP;

        public ElnDescriptor(double nominalU, double maxP) {
            this.nominalU = nominalU;
            this.maxP = maxP;
        }
    }

    public static class Ic2Descriptor {
        public double outMax;
        public int tier;

        public Ic2Descriptor(double outMax, int tier) {
            this.outMax = outMax;
            this.tier = tier;
        }
    }
}
