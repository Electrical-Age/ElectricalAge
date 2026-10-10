package mods.eln.sixnode.electricaltimeout;


import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalTimeoutDescriptor extends SixNodeDescriptor {

    Obj3D obj;
    Obj3DPart main, rot, led;
    float rotStart, rotEnd;

    String tickSound = null;
    float tickVolume = 0f;

    public ElectricalTimeoutDescriptor(String name, Obj3D obj) {
        super(name, ElectricalTimeoutElement.class, null);
        if (obj != null) {
            main = obj.getPart("main");
            rot = obj.getPart("rot");
            if (rot != null) {
                rotStart = rot.getFloat("rotStart");
                rotEnd = rot.getFloat("rotEnd");
            }
            led = obj.getPart("led");
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Upon application of a high signal,\nthe timer maintains the output high for\na configurable interval. Can be re-triggered.").split("\n"));
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }
    // 26.x port: draw() removed (client rendering; redone in P3)

    public ElectricalTimeoutDescriptor setTickSound(String tickSound, float tickVolume) {
        this.tickSound = tickSound;
        this.tickVolume = tickVolume;
        return this;
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
