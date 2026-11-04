package mods.eln.api.docs;

import mods.eln.api.MachineSpec;
import mods.eln.api.Tier;
import mods.eln.api.element.ElementMachine;
import mods.eln.api.element.Machines;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.transparent.TransparentNode;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/** docs/api.md's EA element on the Machine API (EA's own devices; these name EA types). */
public class HeaterElement extends TransparentNodeElement {
    // doc:begin element
    static final MachineSpec SPEC = MachineSpec.consumer(Tier.V50).ratedWatts(500).build();

    final ElementMachine machine;

    public HeaterElement(TransparentNode node, TransparentNodeDescriptor descriptor) {
        super(node, descriptor);
        // in the constructor: its parts must be in the element's lists before readFromNBT
        machine = Machines.forElement(this, SPEC);
        slowProcessList.add(dt -> machine.takeJoules(machine.ratedJoulesPerTick())); // the work, after the machine's books
    }

    @Override
    public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
        return machine.electricalLoad(side, lrdu);
    }

    @Override
    public int getConnectionMask(Direction side, LRDU lrdu) {
        return machine.connectionMask(side, lrdu);
    }

    @Override
    public Map<String, String> getWaila() {
        Map<String, String> info = super.getWaila();
        info.putAll(machine.wailaLines());
        return info;
    }
    // doc:end element

    @Override
    public void initialize() {
        connect();
    }

    @Override
    public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
        return null;
    }

    @Override
    public String multiMeterString(Direction side) {
        return mods.eln.api.Meters.volts(machine.volts());
    }

    @Override
    public String thermoMeterString(Direction side) {
        return "";
    }

    @Override
    public boolean onBlockActivated(Player player, Direction side, float vx, float vy, float vz) {
        return false;
    }
}
