package mods.eln.sixnode.electricalgatesource;


import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalGateSourceDescriptor extends SixNodeDescriptor {

    public boolean onOffOnly;

    public boolean autoReset = false;

    enum ObjType {Pot, Button}

    ObjType objType;
    float leverTx;
    ElectricalGateSourceRenderObj render;

    public ElectricalGateSourceDescriptor(String name, ElectricalGateSourceRenderObj render, boolean onOffOnly,
                                          String iconName) {
        super(name, ElectricalGateSourceElement.class, null, iconName);
        this.render = render;
        this.onOffOnly = onOffOnly;

        voltageLevelColor = VoltageLevelColor.SignalVoltage;
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Provides configurable signal\nvoltage.").split("\n"));
    }

    public void setWithAutoReset() {
        autoReset = true;
    }
    // 26.x port: draw() removed (client rendering; redone in P3)

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
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
