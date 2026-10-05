package mods.eln.client.render;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** Client-side `/elnclient reloadrender | showrender` (ClientCommandHandler; works on any server). */
@SideOnly(Side.CLIENT)
public class ElnClientCommand extends CommandBase {
    @Override
    public String getName() {
        return "elnclient";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/elnclient reloadrender | showrender  (item transforms in config/eln-render.cfg)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        String cmd = args.length > 0 ? args[0] : "";
        if (cmd.equalsIgnoreCase("reloadrender")) {
            List<String> problems = ItemTransforms.load();
            say(sender, "eln: reloaded config/eln-render.cfg" + (problems.isEmpty() ? "" : " with " + problems.size() + " problem(s):"));
            for (String p : problems) say(sender, "  " + p);
        } else if (cmd.equalsIgnoreCase("showrender")) {
            for (String l : ItemTransforms.describe()) say(sender, l);
        } else {
            say(sender, getUsage(sender));
        }
    }

    private static void say(ICommandSender s, String m) {
        s.sendMessage(new TextComponentString(m));
    }
}
