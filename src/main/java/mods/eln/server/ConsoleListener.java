/**
 * ConsoleListener.java V2.00 (lambdaShade)
 */

package mods.eln.server;

import mods.eln.Eln;
import mods.eln.misc.Color;
import mods.eln.misc.Version;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static mods.eln.i18n.I18N.tr;


public class ConsoleListener extends CommandBase {

	private List<String> cmdVisibleList;
	private final String cmdNameStr_listCmd = "ls";
	private final String cmdNameStr_man = "man";
	private final String cmdNameStr_about = "about";
	private final String cmdNameStr_aging = "aging";
	private final String cmdNameStr_lampAging = "lampAging";
	private final String cmdNameStr_batteryAging = "batteryAging";
	private final String cmdNameStr_heatFurnaceFuel = "heatFurnaceFuel";
	private final String cmdNameStr_newWind = "newWind";
	private final String cmdNameStr_regenOre = "regenOre";
	private final String cmdNameStr_generateLangFileTemplate = "generateLangFileTemplate";
	private final String cmdNameStr_killMonstersAroundLamps = "killMonstersAroundLamps";

	private final String strOffsetL0 = "  ";
	private final String strOffsetL1 = "    ";

	private final int lineWrapMaxLength = 60;

	public ConsoleListener(){
		//Visible commands when autocomplete request
		cmdVisibleList = new ArrayList<String>();
		cmdVisibleList.add(cmdNameStr_listCmd);
		cmdVisibleList.add(cmdNameStr_man);
		cmdVisibleList.add(cmdNameStr_about);
		cmdVisibleList.add(cmdNameStr_aging);
		cmdVisibleList.add(cmdNameStr_lampAging);
		cmdVisibleList.add(cmdNameStr_batteryAging);
		cmdVisibleList.add(cmdNameStr_heatFurnaceFuel);
		cmdVisibleList.add(cmdNameStr_newWind);
		cmdVisibleList.add(cmdNameStr_regenOre);
		cmdVisibleList.add(cmdNameStr_generateLangFileTemplate);
		cmdVisibleList.add(cmdNameStr_killMonstersAroundLamps);
		java.util.Collections.sort(cmdVisibleList);
	}

	@Override
	public String getCommandName() {
		return "eln";
	}
	
	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		//TODO Rewrite
		String str = Color.COLOR_DARK_CYAN+"ELN mod console."+Color.COLOR_BRIGHT_GREY+" Type \"\\eln \" + TAB";
		return str;
	}

	@Override
	public List addTabCompletionOptions(ICommandSender icommandsender, String[] astring){
		int argc = astring.length;

		switch(argc){
			case 1:
				//Parse for probable commands
				if(astring[0].isEmpty()){
					icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_CYAN + "ELN >"));
					icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_BRIGHT_GREY   + "   \""+cmdNameStr_listCmd+"\" to print the full command list."));
					icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_BRIGHT_GREY   + "   \""+ cmdNameStr_man +"\" + <command> for command usage (or command + TAB)."));
					List<String> ret = new ArrayList<String>();
					ret.add(cmdNameStr_listCmd);
					return ret;
				}
				//icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_GREY + "ELN > Console > Available commands :\n"));
				List<String> cmdl = new ArrayList<String>();
				Iterator<String> iter = cmdVisibleList.iterator();
				while(iter.hasNext()){
					String val = iter.next();
					if(val.toLowerCase().startsWith(astring[0].toLowerCase()))
						cmdl.add(val);
				}
				if(cmdl.size()!=1)
					return cmdl;
				if(!cmdl.get(0).equals(astring[0]))
					return cmdl;
			case 2:
			default:
				//Return specific command arguments
				commandMan(icommandsender, astring[0]);
				break;
		}
		return null;
	}

	@Override
	public void processCommand(ICommandSender ics, String[] astring) {
		String cmd = astring[0];

		if(cmd.isEmpty()){ //Will normally never append.
			return;
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_man)) {
			if(astring.length == 1)
				commandMan(ics,cmdNameStr_man);
			else{
				if(!checkArgCount(ics,astring,1))
					return;
				commandMan(ics,astring[1]);
			}
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_listCmd)) {
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_listCmd);
			cprint(ics, strOffsetL0 + "Public command list :");
			String line = "";
			Iterator<String> iter = cmdVisibleList.iterator();
			while(iter.hasNext()){
				String val = iter.next();
				if((line.length()+val.length()+2) > (lineWrapMaxLength-strOffsetL0.length())){
					cprint(ics, strOffsetL0 + Color.COLOR_DARK_GREY + line);
					line = "";
				}
				line += val;
				if(iter.hasNext())
					line += ", ";
				else {
					line += ".";
					cprint(ics, strOffsetL0 + Color.COLOR_DARK_GREY + line);
				}
			}

		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_about)) {
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_about);
			cprint(ics, strOffsetL0+Eln.NAME+" - Codename "+Eln.MODID.toUpperCase());
			cprint(ics, strOffsetL0+"V"+String.valueOf(Version.MAJOR)+'.'+String.valueOf(Version.MINOR)+" r"+Version.REVISION);
			String authorsStr = "";
			for(int idx = 0 ; idx < Eln.AUTHORS.length ; idx++)
				authorsStr += Eln.AUTHORS[idx]+ ' ';
			cprint(ics, strOffsetL0+"Authors: "+Color.COLOR_DARK_GREY+authorsStr);
			cprint(ics, strOffsetL0+"Website: "+Color.COLOR_DARK_GREY+Eln.URL);
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_aging)){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_aging);
			if(!checkArgCount(ics,astring,1))
				return;
			ConsoleArg<Boolean> arg0 = getArgBool(ics,astring[1]);
			if(!arg0.valid)
				return;
			SaveConfig.instance.batteryAging = (arg0.value);
			SaveConfig.instance.electricalLampAging = (arg0.value);
			SaveConfig.instance.heatFurnaceFuel = (arg0.value);
			SaveConfig.instance.infinitePortableBattery = (!arg0.value);
			cprint(ics, strOffsetL0+"Batteries / Furnace Fuel / Lamp aging : " + Color.COLOR_DARK_GREEN + boolToStr(arg0.value));
			cprint(ics, strOffsetL0+"Parameter saved in the map.");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_lampAging)){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_lampAging);
			if(!checkArgCount(ics,astring,1))
				return;
			ConsoleArg<Boolean> arg0 = getArgBool(ics,astring[1]);
			if(!arg0.valid)
				return;
			SaveConfig.instance.electricalLampAging = (arg0.value);
			cprint(ics, strOffsetL0+"Lamp aging : " + Color.COLOR_DARK_GREEN + boolToStr(arg0.value));
			cprint(ics, strOffsetL0+"Parameter saved in the map.");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_batteryAging)){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_batteryAging);
			if(!checkArgCount(ics,astring,1))
				return;
			ConsoleArg<Boolean> arg0 = getArgBool(ics,astring[1]);
			if(!arg0.valid)
				return;
			SaveConfig.instance.batteryAging = (arg0.value);
			cprint(ics, strOffsetL0+"Non portable batteries aging : " + Color.COLOR_DARK_GREEN + boolToStr(arg0.value));
			cprint(ics, strOffsetL0+"Parameter saved in the map.");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_heatFurnaceFuel)){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_heatFurnaceFuel);
			if(!checkArgCount(ics,astring,1))
				return;
			ConsoleArg<Boolean> arg0 = getArgBool(ics,astring[1]);
			if(!arg0.valid)
				return;
			SaveConfig.instance.heatFurnaceFuel = (arg0.value);
			cprint(ics, strOffsetL0 + "Furnace fuel aging : " + Color.COLOR_DARK_GREEN + boolToStr(arg0.value));
			cprint(ics, strOffsetL0+"Parameter saved in the map.");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_newWind)){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_newWind);
			if(!checkArgCount(ics, astring,0))
				return;
			Eln.wind.newWindTarget();
			cprint(ics, strOffsetL0 + "New random wind amplitude target : " + Eln.wind.getTargetNotFiltred());
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_regenOre)){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_regenOre);
			if(!checkArgCount(ics,astring,1))
				return;
			ConsoleArg<Boolean> arg0 = getArgBool(ics,astring[1]);
			if(!arg0.valid)
				return;
			Eln.instance.saveConfig.reGenOre = arg0.value;
				cprint(ics, strOffsetL0 + "Regenerate ore at next map reload : " + Color.COLOR_DARK_GREEN + boolToStr(arg0.value));
			cprint(ics, strOffsetL0+"Parameter saved in the map and effective once.");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_generateLangFileTemplate)){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_generateLangFileTemplate);
			cprint(ics, strOffsetL0 + "New language system parses source code, see here how to generate language " +
				"files: https://github.com/Electrical-Age/ElectricalAge");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_killMonstersAroundLamps)){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + cmdNameStr_killMonstersAroundLamps);
			if(!checkArgCount(ics,astring,1))
				return;
			ConsoleArg<Boolean> arg0 = getArgBool(ics,astring[1]);
			if(!arg0.valid)
				return;
			Eln.instance.killMonstersAroundLamps = arg0.value;
			cprint(ics, strOffsetL0+"Avoid monsters spawning around lamps : " + Color.COLOR_DARK_GREEN + boolToStr(arg0.value));
			cprint(ics, strOffsetL0 + "Warning: Command effective to this game instance only.");
		}
		else{
			cprint(ics,Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_RED + "Error: Unknown command.");
		}

		return;

		//Eln.simulator.setSimplify(!astring[1].equals("0"));
		//Eln.simulator.pleaseCrash = true;
	}

	private boolean checkArgCount(ICommandSender ics, String[] args, int exceptedArgc){
		if((args.length-1)!=exceptedArgc){
			cprint(ics, Color.COLOR_DARK_CYAN + "ELN > " + Color.COLOR_DARK_YELLOW + args[0]);
			cprint(ics, strOffsetL0+Color.COLOR_DARK_RED+"Error: Bad argument count.");
			return false;
		}
		return true;
	}

	private ConsoleArg<Boolean> getArgBool(ICommandSender ics, String arg){
		arg = arg.toLowerCase();
		if (arg.isEmpty()){
			cprint(ics, strOffsetL0+"Error: Empty argument."); //TODO <translate>
			return new ConsoleArg<Boolean>(false,null);
		}
		if(arg.equals("0") || arg.equals("false") || arg.equals("no") || arg.equals("disabled")){
			return new ConsoleArg<Boolean>(true,false);
		}
		else if(arg.equals("1") || arg.equals("true") || arg.equals("yes") || arg.equals("enabled")){
			return new ConsoleArg<Boolean>(true,true);
		}
		else{
			cprint(ics, strOffsetL0+"Error: Unexcepted argument type."); //TODO <translate>
			return new ConsoleArg<Boolean>(false,null);
		}

	}

	private String boolToStr(boolean val){
		if(val)
			return "Enabled";
		else
			return "Disabled";
	}

	private void commandMan(ICommandSender ics, String cmd) {
		cprint(ics, (Color.COLOR_DARK_CYAN+"ELN >"+Color.COLOR_DARK_YELLOW+" man > "+Color.COLOR_WHITE+cmd));
		if(cmd.equalsIgnoreCase(cmdNameStr_man)){
			cprint(ics, strOffsetL0+"Returns help for a given command.");
			cprint(ics, "");
			cprint(ics, strOffsetL0+"Parameters :");
			cprint(ics, strOffsetL1+"@0:string : Command name to get documentation.");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_about)) {
			cprint(ics, strOffsetL0 + "Returns useful information on this mod.");
			cprint(ics, "");
			cprint(ics, strOffsetL0 + "No input parameters.");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_listCmd)){
			cprint(ics, strOffsetL0 + "Lists all ELN publicly available commands.");
			cprint(ics, "");
			cprint(ics, strOffsetL0 + "No input parameters.");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_about)) {
			cprint(ics, strOffsetL0 + "Gives some information about ElectricalAge mod.");
			cprint(ics, "");
			cprint(ics, strOffsetL0 + "No input parameters.");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_aging)){
			cprint(ics, strOffsetL0 + "Enables/disables aging on :");
			cprint(ics, strOffsetL0 + "- Portable and standards batteries,");
			cprint(ics, strOffsetL0 + "- Lamps,");
			cprint(ics, strOffsetL0 + "- Fuel into electrical furnaces.");
			cprint(ics, strOffsetL0 + "Acts as a combination of the following commands :");
			cprint(ics, strOffsetL0 + "- "+ cmdNameStr_batteryAging +", "+ cmdNameStr_lampAging +", "+ cmdNameStr_heatFurnaceFuel);
			cprint(ics, strOffsetL0 + "Changes stored into the map.");
			cprint(ics, "");
			cprint(ics, strOffsetL0+"Parameters :");
			cprint(ics, strOffsetL1+"@0:bool : Aging state (enabled/disabled).");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_lampAging)){
			cprint(ics, strOffsetL0 + "Enables/disables aging on lamps.");
			cprint(ics, strOffsetL0 + "Changes stored into the map.");
			cprint(ics, "");
			cprint(ics, strOffsetL0+"Parameters :");
			cprint(ics, strOffsetL1+"@0:bool : Aging state (enabled/disabled).");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_batteryAging)){
			cprint(ics, strOffsetL0 + "Enables/disables aging on standard batteries.");
			cprint(ics, strOffsetL0 + "Changes stored into the map.");
			cprint(ics, "");
			cprint(ics, strOffsetL0+"Parameters :");
			cprint(ics, strOffsetL1+"@0:bool : Aging state (enabled/disabled).");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_heatFurnaceFuel)){
			cprint(ics, strOffsetL0 + "Enables/disables aging on fuel into electrical furnaces.");
			cprint(ics, strOffsetL0 + "Changes stored into the map.");
			cprint(ics, "");
			cprint(ics, strOffsetL0+"Parameters :");
			cprint(ics, strOffsetL1+"@0:bool : Aging state (enabled/disabled).");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_newWind)){
			cprint(ics, strOffsetL0 + "Changes progressively the wind to another target amplitude.");
			cprint(ics, strOffsetL0 + "Changes stored into the map.");
			cprint(ics, "");
			cprint(ics, strOffsetL0 + "No input parameters.");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_regenOre)){
			cprint(ics, strOffsetL0 + "When set, regenerates ELN ores at the next map reload.");
			cprint(ics, strOffsetL0 + "Changes stored into the map and effective once when set.");
			cprint(ics, "");
			cprint(ics, strOffsetL0+"Parameters :");
			cprint(ics, strOffsetL1+"@0:bool : Regenerate flag (enabled/disabled).");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_generateLangFileTemplate)){
			cprint(ics, strOffsetL0 + "Generate a new language file or complete an existing one");
			cprint(ics, strOffsetL0 + "with missing fields.");
			cprint(ics, "");
			cprint(ics, strOffsetL0+"Parameters :");
			cprint(ics, strOffsetL1+"@0:string : full file path.");
			cprint(ics, "");
		}
		else if(cmd.equalsIgnoreCase(cmdNameStr_killMonstersAroundLamps)){
			cprint(ics, strOffsetL0 + "When set, monsters don't spawn around the lamps (default).");
			cprint(ics, strOffsetL0 + "When clear, leaving lights on in dark zones is recommended...");
			cprint(ics, strOffsetL0 + "Effective only during this game instance.");
			cprint(ics, strOffsetL0 + "(See \"Eln.cfg\" for permanent effect.)");
			cprint(ics, "");
			cprint(ics, strOffsetL0+"Parameters :");
			cprint(ics, strOffsetL1+"@0:bool : Enable/disable.");
			cprint(ics, "");
		}
		else{
			cprint(ics, Color.COLOR_DARK_RED + strOffsetL0 + "Error : Unknown/Undocumented command.");
		}

	}

	private void cprint(ICommandSender ics, String text){
		ics.addChatMessage(new ChatComponentText(Color.COLOR_BRIGHT_GREY+text));
	}
}

