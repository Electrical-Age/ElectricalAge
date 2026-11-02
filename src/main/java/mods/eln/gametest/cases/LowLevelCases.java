package mods.eln.gametest.cases;

import mods.eln.api.RelativeFace;
import mods.eln.api.lowlevel.Circuit;
import mods.eln.api.lowlevel.FaceConnection;
import mods.eln.api.lowlevel.HostedNode;
import mods.eln.api.lowlevel.Load;
import mods.eln.api.lowlevel.PortKind;
import mods.eln.api.lowlevel.PortLink;
import mods.eln.api.lowlevel.PortRef;
import mods.eln.api.lowlevel.PortStatus;
import mods.eln.api.lowlevel.ResistorLoad;
import mods.eln.api.lowlevel.Watchdog;
import mods.eln.apitest.ApiTestMachines;
import mods.eln.gametest.TestCase;
import mods.eln.gametest.TestContext;
import mods.eln.node.NodeBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * The low-level API (mods.eln.api.lowlevel) in a world, hosted at a vanilla barrel (another mod's block entity as far
 * as EA knows): a power face on a real 50 V line feeds the circuit's resistor; turning the barrel moves its face off
 * the cable; breaking it retires the circuit. A circuit linked to a cable two blocks away by PortRef draws through
 * the link, says why when the cable is broken (NO_NODE) and links again by itself when it is placed back; links to
 * the wrong kind of port or to empty air fail with their own status. A watchdog trips once and its listener opens
 * the load. A machine's block entity can't host a circuit, nor a block entity two.
 */
public final class LowLevelCases {
    private LowLevelCases() {
    }

    public static void addTo(List<TestCase> cases) {
        cases.add(new Hosted());
        cases.add(new Linked());
        cases.add(new Guarded());
    }

    /** A barrel at x facing facing. */
    static BlockEntity barrel(TestContext c, int x, Direction facing) {
        BlockPos p = c.at(x, 1, 1);
        c.level().setBlock(p, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, facing), 3);
        return c.level().getBlockEntity(p);
    }

    static NodeBase nodeAt(TestContext c, BlockPos p) {
        return HostedMachineCases.nodeAt(c, p);
    }

    /** Source at 1, cable at 2, a barrel at 3 facing east (its back toward the cable) with a 10 ohm heater on its back. */
    abstract static class Fed implements TestCase {
        BlockEntity be;
        Circuit circuit;
        Load line;
        ResistorLoad heater;
        HostedNode node;

        public void build(TestContext c) {
            HostedMachineCases.source(c, 1);
            HostedMachineCases.cable(c, 2);
            be = barrel(c, 3, Direction.EAST);
            circuit = Circuit.create("barrel heater");
            line = circuit.load("line");
            heater = circuit.resistorToGround("heater", line, 10);
        }
    }

    /** Hosted at the barrel: it draws through its back, follows the barrel's turn, and retires when it is broken. */
    static final class Hosted extends Fed {
        boolean hostedAt0, hostedAt15, backAt15, frontAt15, backAt22, retiredAt30, attachedAt30;
        double volts15, amps15, amps22;
        NodeBase nodeAt15, nodeAt30;
        Throwable twice, onMachine, benched;

        public String id() {
            return "api_lowlevel_hosted";
        }

        @Override
        public void build(TestContext c) {
            super.build(c);
            node = circuit.hostAt(be, new FaceConnection(RelativeFace.BACK, line));
            hostedAt0 = node.hosted();
            try {
                Circuit.create("second").hostAt(be);
            } catch (Throwable x) {
                twice = x;
            }
            try {
                mods.eln.api.testkit.ElnTestKit.bench().attach(circuit);
            } catch (Throwable x) {
                benched = x;
            }
            c.level().setBlock(c.at(5, 1, 1), ApiTestMachines.BLOCK.get().defaultBlockState(), 3);
            try {
                Circuit.create("on a machine").hostAt(c.level().getBlockEntity(c.at(5, 1, 1)));
            } catch (Throwable x) {
                onMachine = x;
            }
            c.after(15, () -> {
                hostedAt15 = node.hosted();
                backAt15 = node.connected(RelativeFace.BACK);
                frontAt15 = node.connected(RelativeFace.FRONT);
                volts15 = line.volts();
                amps15 = heater.amps();
                nodeAt15 = nodeAt(c, be.getBlockPos());
                // turned west: its back faces east, away from the cable
                c.level().setBlock(be.getBlockPos(), c.level().getBlockState(be.getBlockPos()).setValue(BlockStateProperties.FACING, Direction.WEST), 3);
            });
            c.after(22, () -> {
                backAt22 = node.connected(RelativeFace.BACK);
                amps22 = heater.amps();
            });
            c.after(25, () -> c.level().destroyBlock(be.getBlockPos(), false));
            c.after(30, () -> {
                retiredAt30 = node.retired();
                attachedAt30 = circuit.attached();
                nodeAt30 = nodeAt(c, be.getBlockPos());
            });
        }

        public void measure(TestContext c) {
            c.check("hosted: it joins at the next tick, not in hostAt", !hostedAt0 && hostedAt15, hostedAt0 + " / " + hostedAt15);
            c.check("hosted: an EA node of its own at the barrel", nodeAt15 instanceof HostedNode, String.valueOf(nodeAt15));
            c.check("hosted: its back is linked to the cable, its front to nothing", backAt15 && !frontAt15, backAt15 + " / " + frontAt15);
            c.check("hosted: the 50 V line reaches its load", volts15 > 45 && volts15 < 50.5, volts15 + " V");
            c.checkValue("hosted: the heater draws by Ohm's law [A]", amps15, volts15 / 10);
            c.check("turned: its back leaves the cable and the heater goes cold", !backAt22 && Math.abs(amps22) < 1e-3,
                backAt22 + ", " + amps22 + " A");
            c.check("broken: the circuit retires and leaves the simulation, the node goes", retiredAt30 && !attachedAt30 && nodeAt30 == null,
                retiredAt30 + " / " + attachedAt30 + " / " + nodeAt30);
            c.check("rules: a block entity hosts one circuit", twice instanceof IllegalStateException, String.valueOf(twice));
            c.check("rules: a hosted circuit can't go on a bench too", benched instanceof IllegalStateException, String.valueOf(benched));
            c.check("rules: a machine's block entity can't host a circuit", onMachine instanceof IllegalStateException
                && onMachine.getMessage().contains("machine"), String.valueOf(onMachine));
        }
    }

    /** Linked by PortRef to the cable two blocks away: it draws through the link, and relinks after a break. */
    static final class Linked implements TestCase {
        PortLink link, wrong, air;
        ResistorLoad heater;
        final List<PortStatus> seen = new ArrayList<>();
        PortStatus at15, wrongAt15, airAt15, at22, at32;
        double amps15, linkAmps15, amps22, amps32;

        public String id() {
            return "api_lowlevel_port";
        }

        @Override
        public void build(TestContext c) {
            HostedMachineCases.source(c, 1);
            HostedMachineCases.cable(c, 2);
            BlockPos cable = c.at(2, 1, 1);
            BlockEntity be = barrel(c, 5, Direction.UP);   // (not beside anything of EA's: no faces at all)
            Circuit circuit = Circuit.create("remote heater");
            Load line = circuit.load("line");
            heater = circuit.resistorToGround("heater", line, 10);
            link = circuit.connect(line, PortRef.of(cable, Direction.EAST, PortKind.POWER)).onStatus(seen::add);
            wrong = circuit.connect(circuit.signalLoad("probe"), PortRef.of(cable, Direction.EAST, PortKind.SIGNAL));
            air = circuit.connect(circuit.load("nowhere"), PortRef.of(c.at(4, 2, 1), Direction.EAST, PortKind.POWER));
            circuit.hostAt(be);
            c.after(15, () -> {
                at15 = link.status();
                wrongAt15 = wrong.status();
                airAt15 = air.status();
                amps15 = heater.amps();
                linkAmps15 = link.amps();
                c.level().destroyBlock(cable, false);
            });
            c.after(22, () -> {
                at22 = link.status();
                amps22 = heater.amps();
                HostedMachineCases.cable(c, 2);
            });
            c.after(32, () -> {
                at32 = link.status();
                amps32 = heater.amps();
            });
        }

        public void measure(TestContext c) {
            c.check("port: linked to the cable's east side", at15 == PortStatus.CONNECTED, String.valueOf(at15));
            c.check("port: the heater draws through the link", amps15 > 4 && amps15 < 5.01, amps15 + " A");
            c.checkValue("port: the link carries the heater's current [A]", linkAmps15, amps15);
            c.check("port: a signal link to a power cable is WRONG_KIND", wrongAt15 == PortStatus.WRONG_KIND, String.valueOf(wrongAt15));
            c.check("port: a link to empty air is NO_NODE", airAt15 == PortStatus.NO_NODE, String.valueOf(airAt15));
            c.check("port: the cable broken, NO_NODE and no current", at22 == PortStatus.NO_NODE && Math.abs(amps22) < 1e-3,
                at22 + ", " + amps22 + " A");
            c.check("port: the cable placed back, it links again by itself", at32 == PortStatus.CONNECTED && amps32 > 4,
                at32 + ", " + amps32 + " A");
            c.check("port: every change reached the listener", seen.equals(List.of(PortStatus.CONNECTED, PortStatus.NO_NODE, PortStatus.CONNECTED)),
                seen.toString());
        }
    }

    /** A watchdog over 40 V on the 50 V line trips once, and its listener opens the heater. */
    static final class Guarded extends Fed {
        Watchdog watchdog;
        final List<String> trips = new ArrayList<>();

        public String id() {
            return "api_lowlevel_watchdog";
        }

        @Override
        public void build(TestContext c) {
            super.build(c);
            watchdog = circuit.watchdogs().volts(line, 0, 40).onTrip(t -> {
                trips.add(t.toString());
                heater.setOhms(Double.POSITIVE_INFINITY);
            });
            circuit.hostAt(be, new FaceConnection(RelativeFace.BACK, line));
        }

        public void measure(TestContext c) {
            c.check("watchdog: tripped once on the 50 V line", watchdog.tripped() && trips.size() == 1
                && trips.get(0).startsWith("line: ") && trips.get(0).endsWith("V over 40.0 V"), trips.toString());
            c.check("watchdog: its listener opened the heater", Double.isInfinite(heater.ohms()) && Math.abs(heater.amps()) < 1e-3,
                heater.ohms() + " ohm, " + heater.amps() + " A");
        }
    }
}
