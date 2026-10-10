package mods.eln.sixnode.electricalredstoneoutput;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalRedstoneOutputDescriptor extends SixNodeDescriptor {

    public float[] pinDistance;

    Obj3D obj;
    Obj3DPart main, led;

    public ElectricalRedstoneOutputDescriptor(String name, Obj3D obj) {
        super(name, ElectricalRedstoneOutputElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            led = obj.getPart("led");

            pinDistance = Utils.getSixNodePinDistance(main);
        }

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }
    void draw(int redstone) {
        //LRDU.Down.glRotateOnX();
        if (main != null) main.draw();

        float light = redstone / 15f;
        GL11.glColor4f(light, light, light, 1f);
        UtilsClient.drawLight(led);
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addSignal(newItemStack());
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)
    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Converts electrical voltage\ninto a Redstone signal.").split("\n"));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).right();
    }
}
