package mods.eln.fluidcore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Steps many independent networks on several threads (08-core-plan.md M3). Networks share nothing mutable (WaterAir's
 * vapour cache and the step's hard deadline are per thread), so each is stepped by one thread and the result is the same
 * as stepping them one by one. The calling thread is one of the pool's: it takes tasks too, and waits for none while
 * there is work.
 */
public final class FluidPool implements AutoCloseable {
    /** The helpers beside the calling thread (null: none, everything runs on the caller). */
    private final ExecutorService workers;
    private final int threads;

    /** threads in all, the calling thread counted: 1 runs everything on the caller. */
    public FluidPool(int threads) {
        this.threads = Math.max(1, threads);
        workers = this.threads == 1 ? null : Executors.newFixedThreadPool(this.threads - 1, r -> {
            Thread t = new Thread(r, "fluidcore-worker");
            t.setDaemon(true);
            return t;
        });
    }

    /** The threads tasks run on, the caller's included. */
    public int threads() {
        return threads;
    }

    /**
     * Run every task once, on this thread and the helpers, and return when all have ended. Tasks are taken in list
     * order (the long ones first spreads them best). A task's exception doesn't stop the others; the first is rethrown
     * here once all have ended.
     */
    public void runAll(List<? extends Runnable> tasks) {
        int n = tasks.size();
        if (n == 0) return;
        AtomicInteger next = new AtomicInteger();
        AtomicReference<Throwable> failed = new AtomicReference<>();
        Runnable drain = () -> {
            for (int i; (i = next.getAndIncrement()) < n; ) {
                try {
                    tasks.get(i).run();
                } catch (Throwable t) {
                    failed.compareAndSet(null, t);
                }
            }
        };
        List<Future<?>> helping = new ArrayList<>();
        if (workers != null) for (int k = Math.min(threads - 1, n - 1); k > 0; k--) helping.add(workers.submit(drain));
        drain.run();
        boolean interrupted = false;
        for (Future<?> f : helping) {
            while (true) {
                try {
                    f.get();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;   // (a helper may still hold a task: wait it out, then say so)
                } catch (ExecutionException e) {
                    failed.compareAndSet(null, e.getCause());
                    break;
                }
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
        Throwable t = failed.get();
        if (t instanceof RuntimeException r) throw r;
        if (t instanceof Error e) throw e;
        if (t != null) throw new IllegalStateException("a task failed", t);
    }

    /** One step of dt for every network, in parallel; returns when all are done (a network's exception is rethrown). */
    public void stepAll(List<Network> nets, double dt) {
        List<Runnable> jobs = new ArrayList<>(nets.size());
        for (Network n : nets) jobs.add(() -> n.step(dt));
        runAll(jobs);
    }

    @Override
    public void close() {
        if (workers != null) workers.shutdownNow();
    }
}
