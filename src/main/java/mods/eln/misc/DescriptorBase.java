package mods.eln.misc;

public class DescriptorBase {

	public String descriptorKey;

	public DescriptorBase(String key) {
		this.descriptorKey = key;
		DescriptorManager.put(key,this);
	}
}
