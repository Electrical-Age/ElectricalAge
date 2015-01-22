package mods.eln.node;

public interface IVoltageDestructorDescriptor {
	public double getVoltageDestructionMax();
	public double getVoltageDestructionStart();
	public double getVoltageDestructionPerOverflow();
	public double getVoltageDestructionProbabilityPerOverflow();
}
