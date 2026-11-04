package mods.eln.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * The API's debug trace: with the system property eln.api.debug=true (-Deln.api.debug=true on the server's command
 * line), every host, connect, disconnect, port link change and trip of the Machine API and its low level is logged
 * (logger "eln.api", INFO) with its volts and ohms. Off by default: nothing is formatted when off.
 */
public final class ApiTrace {
    private ApiTrace() {
    }

    /** The system property that turns the trace on. */
    public static final String PROPERTY = "eln.api.debug";
    private static final Logger LOG = LoggerFactory.getLogger("eln.api");
    private static volatile boolean enabled = Boolean.getBoolean(PROPERTY);
    private static volatile Consumer<String> sink;

    public static boolean enabled() {
        return enabled;
    }

    /** Turns the trace on or off at run time (tests; the property sets it at start). */
    public static void setEnabled(boolean on) {
        enabled = on;
    }

    /** Also hands every line to sink (tests), or stops (null). */
    public static void setSink(Consumer<String> s) {
        sink = s;
    }

    /** Logs String.format(Locale.ROOT, format, args) when the trace is on. */
    public static void log(String format, Object... args) {
        if (!enabled) return;
        String line = String.format(Locale.ROOT, format, args);
        LOG.info("[eln api] {}", line);
        Consumer<String> s = sink;
        if (s != null) s.accept(line);
    }
}
