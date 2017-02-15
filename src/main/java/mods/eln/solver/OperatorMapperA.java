package mods.eln.solver;

public class OperatorMapperA implements IOperatorMapper {

    private Class operator;
    private String key;

    public OperatorMapperA(String key, Class operator) {
        this.operator = operator;
        this.key = key;
    }

    @Override
    public IOperator newOperator(String key, int depthDelta, java.util.List<Object> arg, int argOffset) {
        if (depthDelta != 0) return null;
        if (!this.key.equals(key)) return null;
        if (argOffset - 1 >= 0 && arg.get(argOffset - 1) instanceof IValue) return null;
        if (argOffset + 1 > arg.size() - 1 || !(arg.get(argOffset + 1) instanceof IValue)) return null;

        IOperator o;

        try {
            o = (IOperator) operator.newInstance();
            o.setOperator(new IValue[]{(IValue) arg.get(argOffset + 1)});
            arg.set(argOffset, o);
            arg.remove(argOffset + 1);
            return o;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
