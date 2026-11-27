package mods.eln.fluidcore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FluidPool.runAll: every task once, the caller working too, a failure rethrown only after the rest have run. */
class FluidPoolTest {
    @Test
    void everyTaskRunsOnceOnSeveralThreadsTheCallerAmongThem() {
        try (FluidPool pool = new FluidPool(4)) {
            for (int round = 0; round < 200; round++) {
                int n = 1 + round % 37;
                AtomicInteger[] ran = new AtomicInteger[n];
                Set<Thread> on = ConcurrentHashMap.newKeySet();
                List<Runnable> tasks = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    AtomicInteger c = ran[i] = new AtomicInteger();
                    tasks.add(() -> {
                        c.incrementAndGet();
                        on.add(Thread.currentThread());
                        long x = 0;
                        for (int k = 0; k < 20_000; k++) x += k * 31L;   // (some work, so the helpers get a share)
                        if (x < 0) throw new AssertionError();
                    });
                }
                pool.runAll(tasks);
                for (int i = 0; i < n; i++) assertEquals(1, ran[i].get(), "task " + i + " of " + n);
                assertTrue(on.size() <= 4, "at most the pool's threads: " + on.size());
            }
        }
    }

    @Test
    void oneThreadRunsThemInOrderOnTheCaller() {
        try (FluidPool pool = new FluidPool(1)) {
            List<Integer> order = new ArrayList<>();
            List<Runnable> tasks = new ArrayList<>();
            Thread me = Thread.currentThread();
            for (int i = 0; i < 10; i++) {
                int k = i;
                tasks.add(() -> {
                    assertSame(me, Thread.currentThread());
                    order.add(k);
                });
            }
            pool.runAll(tasks);
            assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), order);
        }
    }

    @Test
    void aFailureIsRethrownAfterTheOthersRan() {
        try (FluidPool pool = new FluidPool(3)) {
            AtomicInteger ran = new AtomicInteger();
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                int k = i;
                tasks.add(() -> {
                    ran.incrementAndGet();
                    if (k == 4) throw new IllegalArgumentException("task 4");
                });
            }
            var e = assertThrows(IllegalArgumentException.class, () -> pool.runAll(tasks));
            assertEquals("task 4", e.getMessage());
            assertEquals(12, ran.get(), "the others still ran");
            pool.runAll(List.of(ran::incrementAndGet));   // (and the pool still works)
            assertEquals(13, ran.get());
        }
    }
}
