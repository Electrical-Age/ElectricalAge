package mods.eln.fsm;

public interface State {
	void enter();
	State state(double time);
	void leave();
}
