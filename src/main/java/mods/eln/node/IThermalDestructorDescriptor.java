package mods.eln.node;

public interface IThermalDestructorDescriptor {
	public double getThermalDestructionMax();
	public double getThermalDestructionStart();
	public double getThermalDestructionPerOverflow();
	public double getThermalDestructionProbabilityPerOverflow();
}
