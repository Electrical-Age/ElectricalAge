package mods.eln;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class ConsoleListener extends CommandBase{

	@Override
	public String getCommandName()
	{
		return "eln";
	}
	
	@Override
	public String getCommandUsage(ICommandSender icommandsender)
	{
		String str = "0 -> off, 1 -> on\n";
		str += "opt\n";
		str += "aging\n";
		str += "lampAging\n";
		str += "batteryAging\n";
		str += "heatFurnaceFuel\n";
		return str;
	}
	
	// Method called when the command is typed in
	@Override
	public void processCommand(ICommandSender icommandsender, String[] astring)
	{
		String a = astring[0].toLowerCase();
		if(a.equals("opt")){
			Eln.simulator.setSimplify(!astring[1].equals("0"));
		}else if(a.equals("aging")){
			SaveConfig.instance.batteryAging = (!astring[1].equals("0"));
			SaveConfig.instance.electricalLampAging = (!astring[1].equals("0"));
			SaveConfig.instance.heatFurnaceFuel = (!astring[1].equals("0"));
		}else if(a.equals("lampAging")){
			SaveConfig.instance.electricalLampAging = (!astring[1].equals("0"));
		}else if(a.equals("batteryAging")){
			SaveConfig.instance.batteryAging = (!astring[1].equals("0"));
		}else if(a.equals("heatFurnaceFuel")){
			SaveConfig.instance.heatFurnaceFuel = (!astring[1].equals("0"));	
		}
	}

}	