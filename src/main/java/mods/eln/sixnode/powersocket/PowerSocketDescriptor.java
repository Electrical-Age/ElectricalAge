package mods.eln.sixnode.powersocket;

import mods.eln.gl.GL11;

import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class PowerSocketDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    private Obj3DPart base;
    private Obj3DPart socket;
    private int subID;

    public int range;

    public PowerSocketDescriptor(int subID, String name, Obj3D obj, int range) {
        super(name, PowerSocketElement.class, null);
        this.subID = subID;
        this.range = range;
        this.obj = obj;
        if (obj != null) {
            base = obj.getPart("SocketBase");
            switch (subID) {
                case 1:
                    socket = obj.getPart("Socket50V");
                    voltageLevelColor = VoltageLevelColor.LowVoltage;
                    break;
                case 2:
                    socket = obj.getPart("Socket200V");
                    voltageLevelColor = VoltageLevelColor.MediumVoltage;
                    break;
                default:
                    socket = null;
            }
        }

    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addLight(newItemStack(1));
    }

    public void draw() {
        //GL11.glRotatef(90.f,1.f,0.f,0.f);
        if (base != null)
            base.draw();
        if (socket != null)
            socket.draw();
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Supplies any device\nplugged in with energy.").split("\n"));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return LRDU.Down;
    }
}
