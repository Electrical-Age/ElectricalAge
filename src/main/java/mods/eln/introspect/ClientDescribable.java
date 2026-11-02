package mods.eln.introspect;

/**
 * Client: a render or entity that says what this client believes. Put the same sync keys as the server half (the
 * agent compares them) and anything else in {@link Report#client()}.
 */
public interface ClientDescribable {
    void describeClient(Report r);
}
