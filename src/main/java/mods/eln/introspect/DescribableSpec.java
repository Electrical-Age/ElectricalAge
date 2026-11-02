package mods.eln.introspect;

/** A descriptor that states its ratings. It sees no world and no element, so the spec can't take on live values. */
public interface DescribableSpec {
    void describeSpec(Section spec);
}
