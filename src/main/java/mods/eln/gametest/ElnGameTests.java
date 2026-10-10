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
        return List.of(new SimTickCase(), new KotlinRuntimeCase());
    }

    public static void register(IEventBus modBus) {
        for (TestCase c : cases()) FUNCTIONS.register(c.id(), () -> helper -> run(helper, c));
        FUNCTIONS.register(modBus);
        modBus.addListener(RegisterGameTestsEvent.class, ElnGameTests::registerTests);
    }

    static void run(GameTestHelper helper, TestCase c) {
        GameTestContext ctx = new GameTestContext(helper, c.id());
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
}
