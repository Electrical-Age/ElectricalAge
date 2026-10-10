package mods.eln.gametest;

import mods.eln.Eln;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Consumer;

/**
 * Registers every {@link TestCase} as a GameTest eln:&lt;id&gt; in structure eln:floor_WxHxD. Run them headless with
 * `gradlew runGameTestServer` (exit code = number of failed tests), or in game with `/test runall`.
 */
public final class ElnGameTests {
    private ElnGameTests() {
    }

    static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
        DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, Eln.MODID);

    /** The cases; ported 1.12 selftest cases are added here batch by batch. */
    static List<TestCase> cases() {
        java.util.List<TestCase> l = new java.util.ArrayList<>(List.of(new SimTickCase(), new KotlinRuntimeCase(), new GoldenCase()));
        l.addAll(mods.eln.gametest.cases.Batch1Cases.cases());
        l.addAll(mods.eln.gametest.cases.SolarCases.cases());
        mods.eln.gametest.cases.Wp9aCases.addTo(l);
        mods.eln.gametest.cases.Wp9bCases.addTo(l);
        mods.eln.gametest.cases.Wp9cCases.addTo(l);
        mods.eln.gametest.cases.LogicTruthCases.addTo(l);
        mods.eln.gametest.cases.ChipMoreCases.addTo(l);
        mods.eln.gametest.cases.WirelessMoreCases.addTo(l);
        mods.eln.gametest.cases.Wp10aCases.addTo(l);
        mods.eln.gametest.cases.Wp10bCases.addTo(l);
        mods.eln.gametest.cases.WorldSensorCases.addTo(l);
        mods.eln.gametest.cases.PortHelperCases.addTo(l);
        return l;
    }

    public static void register(IEventBus modBus) {
        for (TestCase c : cases()) FUNCTIONS.register(c.id(), () -> helper -> run(helper, c));
        FUNCTIONS.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, ElnGameTests::registerTests);
    }

    static void run(GameTestHelper helper, TestCase c) {
        GameTestContext ctx = new GameTestContext(helper, c.id());
        // tests share one level: a node left by an earlier test here would join this test's circuits
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        for (mods.eln.node.NodeBase n : new java.util.ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (n.coordonate.dimention == mods.eln.misc.Dimensions.id(helper.getLevel())
                && bounds.contains(n.coordonate.x + .5, n.coordonate.y + .5, n.coordonate.z + .5)) {
                ctx.check(c.id() + ": no leaked node from a previous test", false, n.getClass().getSimpleName() + " at " + n.coordonate);
                ctx.finish();
                return;
            }
        }
        // the stone floor (y = 0) of the 1.12 selftest rows; laid here, the structure only gives the empty box
        for (int x = 0; x < c.width(); x++)
            for (int z = 0; z < c.floorDepth(); z++) ctx.floor(ctx.at(x, 0, z));
        try {
            c.build(ctx);
        } catch (RuntimeException e) {
            ctx.check(c.id() + " build", false, e.toString());
            ctx.finish();
            return;
        }
        helper.runAfterDelay(c.ticks(), () -> {
            try {
                c.measure(ctx);
            } catch (RuntimeException e) {
                ctx.check(c.id() + " measure", false, e.toString());
            }
            persistence(helper, ctx);
            ctx.finish();
        });
    }

    static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
            Identifier.fromNamespaceAndPath(Eln.MODID, "default"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (TestCase c : cases()) {
            Identifier id = Identifier.fromNamespaceAndPath(Eln.MODID, c.id());
            Identifier structure = Identifier.fromNamespaceAndPath(Eln.MODID,
                "floor_" + c.width() + "x" + c.height() + "x" + c.depth());
            ResourceKey<Consumer<GameTestHelper>> fn = ResourceKey.create(Registries.TEST_FUNCTION, id);
            event.registerTest(id, new FunctionGameTestInstance(fn,
                new TestData<>(env, structure, c.ticks() + 40, 0, true)));
        }
    }

    /**
     * Save/load round trip of every node in the test's bounds, in the state the case left it (1.12 selftest
     * persistence pass): only live sim state (voltages, switch-resistor states) may differ.
     */
    static void persistence(GameTestHelper helper, GameTestContext ctx) {
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        int ok = 0, total = 0;
        for (mods.eln.node.NodeBase n : new java.util.ArrayList<>(mods.eln.node.NodeManager.instance.getNodes())) {
            if (n.isDestructing() || n.coordonate.dimention != mods.eln.misc.Dimensions.id(helper.getLevel())) continue;
            if (!bounds.contains(n.coordonate.x + .5, n.coordonate.y + .5, n.coordonate.z + .5)) continue;
            total++;
            try {
                Reload r = Reload.node(helper.getLevel(), n.coordonate.pos());
                java.util.List<String> hard = new java.util.ArrayList<>();
                for (String d : r.differences()) if (!Reload.isTransient(d)) hard.add(d);
                if (hard.isEmpty()) ok++;
                else ctx.check("persistence " + Reload.describe(r.fresh) + " at " + n.coordonate, false, "NBT: " + String.join("; ", hard));
            } catch (ReflectiveOperationException | RuntimeException e) {
                ctx.check("persistence at " + n.coordonate, false, e.toString());
            }
        }
        if (total > 0) ctx.check("persistence: every node keeps its saved state", ok == total, ok + "/" + total);
    }
}
