package mods.eln.sim.process.destruct;

import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.node.six.SixNodeElement;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.simplenode.energyconverter.EnergyConverterElnToOtherNode;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;

public class WorldExplosion implements IDestructable {

    Object origine;

    Coordonate c;
    float strength;
    String type;

    public WorldExplosion(Coordonate c) {
        this.c = c;
    }

    public WorldExplosion(SixNodeElement e) {
        this.c = e.getCoordonate();
        this.type = e.toString();
        origine = e;
    }

    public WorldExplosion(TransparentNodeElement e) {
        this.c = e.coordonate();
        this.type = e.toString();
        origine = e;
    }

    public WorldExplosion(EnergyConverterElnToOtherNode e) {
        this.c = e.coordonate;
        this.type = e.toString();
        origine = e;
    }

    public WorldExplosion cableExplosion() {
        strength = 1.5f;
        return this;
    }

    public WorldExplosion machineExplosion() {
        strength = 3;
        return this;
    }

    @Override
    public void destructImpl() {
        //NodeManager.instance.removeNode(NodeManager.instance.getNodeFromCoordonate(c));

        if (Eln.instance.explosionEnable)
            c.world().createExplosion((Entity) null, c.x, c.y, c.z, strength, true);
        else
            c.world().setBlock(c.x, c.y, c.z, Blocks.air);
    }

    @Override
    public String describe() {
        return String.format("%s (%s)", this.type, this.c.toString());
    }
}
