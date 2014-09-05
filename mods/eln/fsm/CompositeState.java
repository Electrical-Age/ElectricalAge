package mods.eln.fsm;

public class CompositeState extends StateMachine implements State {
	@Override
	public void enter() {
		reset();
	}

	@Override
	public void leave() {
		stop();
	}

	@Override
	public State state(double time) {
		process(time);
		return null;
	}

}
