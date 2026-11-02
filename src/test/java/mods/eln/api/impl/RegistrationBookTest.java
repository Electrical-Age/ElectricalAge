package mods.eln.api.impl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The registration rules of ElnApi.machines().register: idempotent, one function per type, closed after its deadline. */
public class RegistrationBookTest {
    static RegistrationBook<String, Function<String, Integer>> book() {
        return new RegistrationBook<>("register", "call it in FMLCommonSetupEvent");
    }

    @Test
    void theSameFunctionAgainIsANoOp() {
        var b = book();
        Function<String, Integer> f = String::length;
        b.register("mill", f);
        b.register("mill", f);
        assertSame(f, b.get("mill"));
        assertEquals(1, b.keys().size());
    }

    @Test
    void anotherFunctionForATypeThrows() {
        var b = book();
        b.register("mill", s -> 1);
        var e = assertThrows(IllegalStateException.class, () -> b.register("mill", s -> 2));
        assertTrue(e.getMessage().contains("already registered"), e.getMessage());
    }

    @Test
    void afterCloseItThrowsNamingTheDeadline() {
        var b = book();
        Function<String, Integer> f = s -> 1;
        b.register("mill", f);
        b.close();
        var e = assertThrows(IllegalStateException.class, () -> b.register("press", s -> 1));
        assertTrue(e.getMessage().contains("FMLCommonSetupEvent"), e.getMessage());
        assertThrows(IllegalStateException.class, () -> b.register("mill", f));   // (even the same one: too late is too late)
        assertNull(b.get("press"));
        assertNull(b.get(null));
    }

    @Test
    void nullsThrow() {
        var b = book();
        assertThrows(NullPointerException.class, () -> b.register(null, s -> 1));
        assertThrows(NullPointerException.class, () -> b.register("mill", null));
    }

    /** Common setups run in parallel: every type registered from many threads is there. */
    @Test
    void parallelRegistrationsAllLand() throws InterruptedException {
        var b = book();
        List<Thread> ts = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            int k = t;
            ts.add(new Thread(() -> {
                for (int i = 0; i < 100; i++) b.register("t" + k + "_" + i, s -> 0);
            }));
        }
        for (Thread t : ts) t.start();
        for (Thread t : ts) t.join();
        assertEquals(800, b.keys().size());
    }
}
