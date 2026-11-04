package mods.eln.api.impl;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registrations by key, open until close(): the same value twice is a no-op, another value for a key throws, and so
 * does anything after close(). Thread-safe (mods' common setups run in parallel).
 */
public final class RegistrationBook<K, V> {
    private final Map<K, V> entries = new ConcurrentHashMap<>();
    private final String what, deadline;
    private volatile boolean closed;

    /** what: the call, for messages; deadline: when to call it, for the too-late message. */
    public RegistrationBook(String what, String deadline) {
        this.what = what;
        this.deadline = deadline;
    }

    public synchronized void register(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (closed) throw new IllegalStateException(what + "(" + key + "): too late, " + deadline);
        V had = entries.putIfAbsent(key, value);
        if (had != null && !had.equals(value))
            throw new IllegalStateException(what + "(" + key + "): already registered with another function; register a type once");
    }

    /** No more registrations from now on. */
    public synchronized void close() {
        closed = true;
    }

    public boolean closed() {
        return closed;
    }

    /** key's value, or null. */
    public V get(K key) {
        return key == null ? null : entries.get(key);
    }

    public Set<K> keys() {
        return java.util.Collections.unmodifiableSet(entries.keySet());
    }
}
