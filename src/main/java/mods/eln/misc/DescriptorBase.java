package mods.eln.misc;

public class DescriptorBase {

    public String descriptorKey;

    /** Simple-node devices: faces with a floor terminal plate, where floor cables draw a lug (null = none). */
    public mods.eln.cable.CableTerminals cableTerminals;

    public DescriptorBase(String key) {
        this.descriptorKey = key;
        DescriptorManager.put(key, this);
    }
}
