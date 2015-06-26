package mods.eln.server;

import mods.eln.Eln;
import mods.eln.misc.Color;
import mods.eln.misc.LangFileParser;
import mods.eln.misc.Utils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

public class ConsoleListener extends CommandBase {

    @Override
    public String getCommandName() {
        return "eln";
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        String str = "0 -> off, 1 -> on\n";
        str += "aging  lampAging  batteryAging  heatFurnaceFuel  reGenOre";
        return str;
    }

    // Method called when the command is typed in
    @Override
    public void processCommand(ICommandSender icommandsender, String[] astring) {
        String a = astring[0].toLowerCase();
        boolean ack = false;
        if (a.equals("opt")) {
            //Eln.simulator.setSimplify(!astring[1].equals("0"));
            ack = true;
            printBooleanResult(icommandsender, ack);
        } else if (a.equals("aging")) {
            SaveConfig.instance.batteryAging = (!astring[1].equals("0"));
            SaveConfig.instance.electricalLampAging = (!astring[1].equals("0"));
            SaveConfig.instance.heatFurnaceFuel = (!astring[1].equals("0"));
            SaveConfig.instance.infinitPortableBattery = (astring[1].equals("0"));
            ack = true;
            printBooleanResult(icommandsender, ack);
        } else if (a.equals("lampaging")) {
            SaveConfig.instance.electricalLampAging = (!astring[1].equals("0"));
            ack = true;
            printBooleanResult(icommandsender, ack);
        } else if (a.equals("batteryaging")) {
            SaveConfig.instance.batteryAging = (!astring[1].equals("0"));
            ack = true;
            printBooleanResult(icommandsender, ack);
        } else if (a.equals("heatfurnacefuel")) {
            SaveConfig.instance.heatFurnaceFuel = (!astring[1].equals("0"));
            ack = true;
            printBooleanResult(icommandsender, ack);
        } else if (a.equals("newwind")) {
            Eln.wind.newWindTarget();
            Utils.println("newWind : " + Eln.wind.getTargetNotFiltred());
            ack = true;
            printBooleanResult(icommandsender, ack);
        } else if (a.equals("regenore")) {
            Eln.instance.saveConfig.reGenOre = (!astring[1].equals("0"));
            ack = true;
            printBooleanResult(icommandsender, ack);
        } else if (a.equals("generatelangfiletemplate")) {
            LangFileParser.RetStatus retCode = LangFileParser.parseAndFillFile(astring[1]);
            if (retCode == LangFileParser.RetStatus.SUCCESS)
                icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_GREEN + "Success."));
            else if (retCode == LangFileParser.RetStatus.ERR__BAD_HEADER)
                icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_RED + "Error: Existing file has a bad header."));
            else if (retCode == LangFileParser.RetStatus.ERR__IO_ERROR)
                icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_RED + "I/O Error."));
            else if (retCode == LangFileParser.RetStatus.ERR__PARSING_ERROR)
                icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_RED + "Parsing error: malformed file."));
            else
                icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_YELLOW + "Unknown status returned."));
        }
        //Eln.simulator.pleaseCrash = true;
    }

    private void printBooleanResult(ICommandSender icommandsender, Boolean ack) {
        if (ack) {
            icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_GREEN + "Success."));
        } else {
            icommandsender.addChatMessage(new ChatComponentText(Color.COLOR_DARK_RED + "Error."));
        }
    }
}
