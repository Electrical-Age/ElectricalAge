package mods.eln.solver;

public abstract class OperatorAB implements IOperator {

    protected IValue a, b;

    @Override
    public void setOperator(IValue[] values) {
        this.a = values[0];
        this.b = values[1];
    }
}
