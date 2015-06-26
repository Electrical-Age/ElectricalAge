package mods.eln.transparentnode.turbine;

import mods.eln.sim.IProcess;
import mods.eln.sim.mna.SubSystem.Th;
import mods.eln.sim.mna.misc.IRootSystemPreStepProcess;


public class TurbineElectricalProcess implements IProcess, IRootSystemPreStepProcess {
    TurbineElement turbine;

    int id;

    public TurbineElectricalProcess(TurbineElement t) {
        this.turbine = t;
    }


    @Override
    public void process(double time) {
        /*TurbineDescriptor descriptor = turbine.descriptor;
		double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
		if(deltaT < 0) return;
		double deltaU = turbine.positiveLoad.getSubSystem().solve(turbine.positiveLoad);//turbine.positiveLoad.getU();
		double targetU = descriptor.TtoU.getValue(deltaT);

		PowerSource eps = turbine.electricalPowerSourceProcess;
		eps.setUmax(targetU);
		double eP = 0;
		if(targetU - deltaU > 0)
			eP = (targetU - deltaU) * descriptor.powerOutPerDeltaU;
		eP = eps.getP()*0.1 + eP*0.9;
		
		eP = Math.min(eP, turbine.descriptor.nominalP*3);
		//eP = 250;
		eps.setP(eP);		*/
        TurbineDescriptor descriptor = turbine.descriptor;
        double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
        //if(deltaT < 0) return;
        double deltaU = turbine.positiveLoad.getSubSystem().solve(turbine.positiveLoad);//turbine.positiveLoad.getU();
        double targetU = descriptor.TtoU.getValue(deltaT);


        Th th = turbine.positiveLoad.getSubSystem().getTh(turbine.positiveLoad, turbine.electricalPowerSourceProcess);
        double Ut;
        if (targetU < th.U) {
            Ut = th.U;
        } else if (th.isHighImpedance()) {
            Ut = targetU;
        } else {
            //double f = descriptor.powerOutPerDeltaU;
            //Ut = (Math.sqrt(-4*f*th.R*th.U+f*f*th.R*th.R+4*f*targetU*th.R)+2*th.U-f*th.R)/2;
            double a = 1 / th.R;
            double b = descriptor.powerOutPerDeltaU - th.U / th.R;
            double c = -descriptor.powerOutPerDeltaU * targetU;
            Ut = (-b + Math.sqrt(b * b - 4 * a * c)) / (2 * a);
        }

        double i = (Ut - th.U) / th.R;
        double p = i * Ut;
        double pMax = descriptor.nominalP * 1.5;
        if (p > pMax) {
            Ut = (Math.sqrt(th.U * th.U + 4 * pMax * th.R) + th.U) / 2;
            Ut = Math.min(Ut, targetU);
            if (Double.isNaN(Ut)) Ut = 0;
            if (Ut < th.U) Ut = th.U;
            //double pCalc = Ut*(Ut-th.U)/th.R;

        }

//		Ut = th.U;
        //double Ut2 = -(Math.sqrt(-4*f*th.R*th.U+f*f*th.R*th.R+4*f*targetU*th.R)-2*th.U+f*th.R)/2;

        turbine.electricalPowerSourceProcess.setU(Ut);
        //turbine.electricalPowerSourceProcess.setU(th.U);
		
		/*PowerSource eps = turbine.electricalPowerSourceProcess;
		eps.setUmax(targetU);
		double eP = 0;
		if(targetU - deltaU > 0)
			eP = (targetU - deltaU) * descriptor.powerOutPerDeltaU;
		eP = eps.getP()*0.1 + eP*0.9;
		
		eP = Math.min(eP, turbine.descriptor.nominalP*3);
		
		eps.setUmax(Ut);
		eP = 2500;
		eps.setP(eP);	*/
    }


    @Override
    public void rootSystemPreStepProcess() {
        process(0);
    }

}
