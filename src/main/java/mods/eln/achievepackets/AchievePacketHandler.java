package mods.eln.achievepackets;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import mods.eln.Achievements;

public class AchievePacketHandler implements IMessageHandler<AchievePacket, IMessage> {

    @Override
    public IMessage onMessage(AchievePacket message, MessageContext ctx) {
        //System.out.println("Got message: " + message.text);
        if (message.text.equals("openWiki")) {
            ctx.getServerHandler().playerEntity.triggerAchievement(Achievements.openGuide);
        } else if (message.text.equals("craft50VMacerator")) {
            ctx.getServerHandler().playerEntity.triggerAchievement(Achievements.craft50VMacerator);
        } else {
            System.out.println("[ELN]: ELN Wiki Achievement Handler has received an invalid message/packet: " + message.text);
        }
        return null;
    }
}
