package mods.eln.solver;

import java.awt.List;
import java.util.LinkedList;

public interface IOperatorMapper {
	IOperator newOperator(String key,int depthDelta,java.util.List<Object> arg,int argOffset);
	
}
