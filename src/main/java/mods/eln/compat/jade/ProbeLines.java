package mods.eln.compat.jade;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ordered two-tier Jade probe lines: {@link #headline} entries always show, {@link #detail} entries show only while
 * sneaking / holding Jade's details key (or with {@code probe.details = true}).
 * <p>
 * Extends {@link LinkedHashMap} so every {@code Map<String, String>} caller (introspection, gametests, Kotlin overrides)
 * sees all entries in exact headline-first, detail-second order.
 */
public class ProbeLines extends LinkedHashMap<String, String> {
    private final LinkedHashMap<String, String> head = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> det = new LinkedHashMap<>();

    public ProbeLines() {
    }

    public static ProbeLines of() {
        return new ProbeLines();
    }

    /** Wrap an arbitrary map, treating up to {@code headlineLimit} entries as headline and the rest as detail. */
    public static ProbeLines from(Map<String, String> map, int headlineLimit) {
        if (map instanceof ProbeLines p) {
            ProbeLines copy = new ProbeLines();
            copy.addAll(p);
            return copy;
        }
        ProbeLines out = new ProbeLines();
        if (map == null) return out;
        int i = 0;
        for (var e : map.entrySet()) {
            if (i++ < headlineLimit) out.headline(e.getKey(), e.getValue());
            else out.detail(e.getKey(), e.getValue());
        }
        return out;
    }

    /** Always-visible line (appended to the headline section, before details). */
    public ProbeLines headline(String key, String value) {
        if (key == null || value == null) return this;
        det.remove(key);
        head.put(key, value);
        sync();
        return this;
    }

    /** Always-visible line prepended to the top of the headline (e.g. an active fault or warning). */
    public ProbeLines headlineFirst(String key, String value) {
        if (key == null || value == null) return this;
        det.remove(key);
        head.remove(key);
        LinkedHashMap<String, String> next = new LinkedHashMap<>();
        next.put(key, value);
        next.putAll(head);
        head.clear();
        head.putAll(next);
        sync();
        return this;
    }

    /** Detail-only line (shown when sneaking / holding Jade's details key). */
    public ProbeLines detail(String key, String value) {
        if (key == null || value == null) return this;
        head.remove(key);
        det.put(key, value);
        sync();
        return this;
    }

    /** Copy another map in order, preserving headline vs. detail if it is a {@link ProbeLines} (else headline). */
    public ProbeLines addAll(Map<String, String> other) {
        if (other == null || other.isEmpty()) return this;
        if (other instanceof ProbeLines p) {
            for (var e : p.head.entrySet()) {
                det.remove(e.getKey());
                head.put(e.getKey(), e.getValue());
            }
            for (var e : p.det.entrySet()) {
                head.remove(e.getKey());
                det.put(e.getKey(), e.getValue());
            }
        } else {
            for (var e : other.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                det.remove(e.getKey());
                head.put(e.getKey(), e.getValue());
            }
        }
        sync();
        return this;
    }

    /** Copy every entry of {@code other} into the detail section. */
    public ProbeLines addAllDetail(Map<String, String> other) {
        if (other == null || other.isEmpty()) return this;
        for (var e : other.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            head.remove(e.getKey());
            det.put(e.getKey(), e.getValue());
        }
        sync();
        return this;
    }

    public int headlineCount() {
        return head.size();
    }

    public boolean hasDetails() {
        return !det.isEmpty();
    }

    @Override
    public String put(String key, String value) {
        String prev = get(key);
        headline(key, value);
        return prev;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        if (m == null) return;
        if (m instanceof ProbeLines p) {
            addAll(p);
            return;
        }
        for (var e : m.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                det.remove(e.getKey());
                head.put(e.getKey(), e.getValue());
            }
        }
        sync();
    }

    @Override
    public String remove(Object key) {
        head.remove(key);
        det.remove(key);
        return super.remove(key);
    }

    @Override
    public void clear() {
        head.clear();
        det.clear();
        super.clear();
    }

    private void sync() {
        super.clear();
        for (var e : head.entrySet()) super.put(e.getKey(), e.getValue());
        for (var e : det.entrySet()) super.put(e.getKey(), e.getValue());
    }
}
