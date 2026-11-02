package mods.eln.introspect;

/**
 * Server: an element, ghost observer or entity that adds to its own description. Nothing is required: without it a
 * device still gets its id, ports, loads and Jade lines. Fill spec only from fixed data, state with live values, and
 * sync with what each client must agree on (every number with a tolerance).
 */
public interface Describable {
    void describe(Report r);
}
