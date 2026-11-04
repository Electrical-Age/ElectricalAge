package mods.eln.node;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** NodeBase.getElectricalLoad(side, lrdu, mask): the port's load only for a connection of a kind it takes. */
public class MaskedLoadTest {
    /** A node with a power port on XP (any position) and a signal gate on YP, coloured. */
    static final class Node extends GhostNode {
        final ElectricalLoad power = new ElectricalLoad(), gate = new ElectricalLoad();

        @Override
        public int getSideConnectionMask(Direction side, LRDU lrdu) {
            if (side == Direction.XP) return maskElectricalPower;
            if (side == Direction.YP) return maskElectricalGate | (3 << maskColorShift) | maskColorCareData;
            return 0;
        }

        @Override
        public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
            return null;
        }

        @Override
        public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
            return side == Direction.XP ? power : side == Direction.YP ? gate : null;
        }

        @Override
        public void initializeFromThat(Direction front, net.minecraft.world.entity.LivingEntity e, net.minecraft.world.item.ItemStack s) {
        }

        @Override
        public void initializeFromNBT() {
        }
    }

    @Test
    void theLoadOnlyForAKindThePortTakes() {
        Node n = new Node();
        assertSame(n.power, n.getElectricalLoad(Direction.XP, LRDU.Up, NodeBase.maskElectricalPower));
        assertSame(n.power, n.getElectricalLoad(Direction.XP, LRDU.Left, NodeBase.maskElectricalAll));
        assertNull(n.getElectricalLoad(Direction.XP, LRDU.Up, NodeBase.maskElectricalGate));
        assertSame(n.gate, n.getElectricalLoad(Direction.YP, LRDU.Down, NodeBase.maskElectricalGate));
        assertNull(n.getElectricalLoad(Direction.YP, LRDU.Down, NodeBase.maskElectricalPower));
        assertNull(n.getElectricalLoad(Direction.ZN, LRDU.Down, NodeBase.maskElectricalAll), "no port: nothing");
    }
}
