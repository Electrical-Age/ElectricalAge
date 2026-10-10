package mods.eln.generic;

/** A 26.x Item that stands for one EA descriptor (1.12: one sub-item, i.e. item + damage, of a shared item). */
public interface IDescriptorItem {
    Object getEaDescriptor();
}
