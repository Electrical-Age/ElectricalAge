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
		return "Miaou";
	}
	
	// Method called when the command is typed in
	@Override
	public void processCommand(ICommandSender icommandsender, String[] astring)
	{
		if(astring[0].equals("opt")){
			Eln.simulator.setSimplify(!astring[1].equals("0"));
		}
	}

}