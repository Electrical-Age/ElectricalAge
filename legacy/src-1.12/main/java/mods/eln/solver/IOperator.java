package mods.eln.solver;

public interface IOperator extends IValue {

    void setOperator(IValue[] values);

    int getRedstoneCost();
}
