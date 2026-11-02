package mods.eln.api.spi;

import mods.eln.api.ElnApi;
import mods.eln.api.impl.HostedMachines;
import mods.eln.api.impl.lowlevel.CircuitImpl;
import mods.eln.api.lowlevel.Circuit;
import mods.eln.api.testkit.ElnTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The API's entry points find EA's implementation by name, and say how to run without EA when it isn't there. */
public class ImplementationsTest {
    @Test
    void theEntryPointsReachEasImplementation() {
        assertSame(HostedMachines.INSTANCE, ElnApi.machines());
        assertInstanceOf(CircuitImpl.class, Circuit.create("probe"));
        assertTrue(ElnTestKit.bench().machines().isEmpty());
        assertTrue(ElnApi.apiVersion().matches("\\d+\\.\\d+\\.\\d+"), ElnApi.apiVersion());
    }

    @Test
    void withoutEaTheApiSaysWhatIsMissingAndHowToRunWithoutIt() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> Implementations.load(ElnApiProvider.class, "mods.eln.api.impl.NotThere"));
        assertTrue(e.getMessage().contains("Electrical Age (mod id \"eln\") is not loaded"), e.getMessage());
        assertTrue(e.getMessage().contains("ModList.get().isLoaded(\"eln\")"), e.getMessage());
        assertTrue(e.getMessage().contains(ElnApi.API_VERSION), e.getMessage());
        IllegalStateException wrong = assertThrows(IllegalStateException.class,
            () -> Implementations.load(ElnApiProvider.class, "java.lang.String"));
        assertTrue(wrong.getMessage().contains("not Electrical Age's ElnApiProvider"), wrong.getMessage());
    }
}
