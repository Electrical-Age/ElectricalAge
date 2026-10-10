package mods.eln.selftest.cases;

import mods.eln.Eln;
import mods.eln.misc.Utils;
import mods.eln.node.six.SixNodeElement;
import mods.eln.selftest.SelfTestCase;
import mods.eln.selftest.SelfTestContext;
import mods.eln.sixnode.electricalwindsensor.ElectricalWindSensorDescriptor;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Sensors that read the world and have no case elsewhere. Port step T3 (notes/port26/PLAN.md).
 * Not here: the entity sensor (it measures entity speed between 0.2 s scans, which a fixed-length run can't stage
 * reliably); left to a client/manual check.
 */
public final class WorldSensorCases {
    private WorldSensorCases() {
    }

    public static void addTo(List<SelfTestCase> cases) {
        cases.add(new AnemometerCase());
    }

    /** Anemometer: output = wind(dim, y) / windMax * SVU (refreshed every 0.2 s; the wind changes slowly). */
    static final class AnemometerCase implements SelfTestCase {
        SixNodeElement s;
        BlockPos p;

        public String name() {
            return "sensor anemometer";
        }

        public int width() {
            return 3;
        }

        public void build(SelfTestContext ctx) {
            p = ctx.at(1, 1, 1);
            s = ctx.placeSix(Ids.six("Electrical Anemometer Sensor"), p);
        }

        public void measure(SelfTestContext ctx) {
            double windMax = ((ElectricalWindSensorDescriptor) Eln.sixNodeItem.getDescriptor(Ids.six("Electrical Anemometer Sensor"))).windMax;
            double wind = Utils.getWind(ctx.world().provider.getDimension(), p.getY());
            double u = s.getElectricalLoad(s.front.right()).getU(); // the pin p with front == p.left()
            Wp9cCases.within(ctx, String.format("sensor anemometer output [V] (wind %.2f m/s / %.0f * SVU)", wind, windMax),
                u, Math.min(1, wind / windMax) * Eln.SVU, 0.02 * Eln.SVU);
        }
    }
}
