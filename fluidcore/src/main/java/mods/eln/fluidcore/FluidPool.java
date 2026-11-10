package mods.eln.fluidcore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Steps many independent networks on worker threads (08-core-plan.md M3). Networks share nothing mutable (WaterAir's
 * vapour cache is per thread), so each is stepped by one worker and the result is the same as stepping them one by one.
 */
public final class FluidPool implements AutoCloseable {
    private final ExecutorService workers;

    public FluidPool(int threads) {
        workers = Executors.newFixedThreadPool(threads, r -> {
            Thread t = new Thread(r, "fluidcore-worker");
            t.setDaemon(true);
            return t;
        });
    }

    /** One step of dt for every network, in parallel; returns when all are done (a network's exception is rethrown). */
    public void stepAll(List<Network> nets, double dt) {
        List<Callable<Void>> jobs = new ArrayList<>(nets.size());
        for (Network n : nets) jobs.add(() -> {
            n.step(dt);
            return null;
        });
        try {
            for (Future<Void> f : workers.invokeAll(jobs)) f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("a network's step failed", e.getCause());
        }
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}
