package mods.eln.sim;

import mods.eln.Eln;

public class TimeRemover implements IProcess{
	public TimeRemover(ITimeRemoverObserver observer){
		this.observer = observer;
	}
	ITimeRemoverObserver observer;
	
	public void setTimeout(double timeout){
		if(this.timeout <= 0){
			observer.timeRemoverAdd();
			Eln.simulator.addSlowProcess(this);	
		}
		this.timeout = timeout;
	}
	double timeout = 0;
	
	
	@Override
	public void process(double time) {
		// TODO Auto-generated method stub
		if(timeout > 0){
			timeout -= time;
			if(timeout <= 0){
				observer.timeRemoverRemove();
				Eln.simulator.removeSlowProcess(this);
			}
		}
	}

}
