package mods.eln.integration.minetweaker.utils;

import minetweaker.IUndoableAction;

public abstract class BasicUndoableAction implements IUndoableAction {
	MinetweakerMachine machine;

	public BasicUndoableAction(MinetweakerMachine machine) {
		this.machine = machine;
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public String describe() {
		return machine.addDesc;
	}

	@Override
	public String describeUndo() {
		return "Undo" + machine.removeDesc;
	}

	@Override
	public Object getOverrideKey() {
		return null;
	}

}
