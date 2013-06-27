package mods.eln.item;

import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptorWithComment;
import mods.eln.ghost.GhostGroup;
import mods.eln.misc.FunctionTable;
import mods.eln.misc.IFunction;

public class WindRotorDescriptor extends GenericItemUsingDamageDescriptor {
	double nominalWind, nominalPower;
	public double maximalWind;
	public WindRotorDescriptor(
			String name, 
			GhostGroup ghostGroupe,
			int environnementalWidthStart,int environnementalWidthEnd,
			int environnementalHeightStart,int environnementalHeightEnd,
			int environnementalDepthStart,int environnementalDepthEnd,
			int environementalOffsetBlock,
			IFunction environnementalFunction,
			WindRotorAxeType axe,
			FunctionTable PfW,
			double nominalWind,double nominalPower,
			double maximalWind
			) 
	{
		super(name);
		this.ghostGroupe = ghostGroupe;
		this.axe = axe;
		this.PfW = PfW.duplicate(nominalWind, nominalPower);
		this.nominalPower = nominalPower;
		this.nominalWind = nominalWind;
		this.maximalWind = maximalWind;
		this.environnementalWidthStart = environnementalWidthStart;
		this.environnementalWidthEnd =environnementalWidthEnd ;
		this.environnementalHeightStart = environnementalHeightStart;
		this.environnementalHeightEnd = environnementalHeightEnd;
		this.environnementalDepthStart = environnementalDepthStart;
		this.environnementalDepthEnd = environnementalDepthEnd;
		this.environnementalFunction = environnementalFunction;
		this.environementalOffsetBlock = environementalOffsetBlock;
	}
	public 	int environnementalWidthStart, environnementalWidthEnd, environnementalHeightStart, environnementalHeightEnd, environnementalDepthStart, environnementalDepthEnd;
	public int environementalOffsetBlock;
	public IFunction environnementalFunction;

	
	public GhostGroup ghostGroupe;

	public enum WindRotorAxeType{horizontal,vertical};

	public WindRotorAxeType axe;
	public FunctionTable PfW;
	
	public double windBreakLimit;

}
