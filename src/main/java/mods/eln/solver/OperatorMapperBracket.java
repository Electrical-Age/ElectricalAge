package mods.eln.solver;

public class OperatorMapperBracket implements IOperatorMapper {

    public OperatorMapperBracket() {
    }

    @Override
    public IOperator newOperator(String key, int depthDelta, java.util.List<Object> arg, int argOffset) {
        if (depthDelta != -1) return null;
        if (!key.equals("(")) return null;
        if (argOffset > arg.size() - 3) return null;
        if (arg.get(argOffset + 1) instanceof IValue
            && arg.get(argOffset + 2) instanceof String && ((String) arg.get(argOffset + 2)).equals(")")) {

            IOperator o = new Equation.Bracket();
            o.setOperator(new IValue[]{(IValue) arg.get(argOffset + 1)});
            arg.set(argOffset, o);
            arg.remove(argOffset + 1);
            arg.remove(argOffset + 1);
            return o;
        }

        return null;
    }
}
