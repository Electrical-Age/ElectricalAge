package mods.eln.robotics.computer.hw;

/**
 * The threads computers run their slices on: a fixed pool of (cores - 2) daemon threads, at least one, taking ready
 * computers in turn (Argon's review: a pool rather than a thread per computer, so the server thread and the network
 * keep their cores and a hundred computers don't mean a hundred threads).
 */
public final class ComputerPool {
    private static volatile java.util.concurrent.ExecutorService pool;

    private ComputerPool() {
    }

    public static int threads() {
        return Math.max(1, Runtime.getRuntime().availableProcessors() - 2);
    }

    static java.util.concurrent.ExecutorService get() {
        java.util.concurrent.ExecutorService p = pool;
        if (p == null) {
            synchronized (ComputerPool.class) {
                if (pool == null) {
                    java.util.concurrent.atomic.AtomicInteger n = new java.util.concurrent.atomic.AtomicInteger();
                    pool = java.util.concurrent.Executors.newFixedThreadPool(threads(), r -> {
                        Thread t = new Thread(r, "eln-computer-" + n.incrementAndGet());
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY - 1);
                        return t;
                    });
                }
                p = pool;
            }
        }
        return p;
    }

    /** Stop the threads (server stopping); slices in flight finish, queued ones are dropped. */
    public static synchronized void shutdown() {
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
    }
}
