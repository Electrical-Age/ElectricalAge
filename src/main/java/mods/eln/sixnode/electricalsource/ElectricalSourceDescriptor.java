package mods.eln.sixnode.electricalsource;


import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.Eln;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class ElectricalSourceDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    private Obj3DPart main;
    private Obj3DPart led;
    private boolean signalSource = false;

    public ElectricalSourceDescriptor(String name, Obj3D obj, boolean signalSource) {
        super(name, ElectricalSourceElement.class, null);
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");
            led = obj.getPart("led");
        }
        this.signalSource = signalSource;

        if (signalSource) {
          voltageLevelColor = VoltageLevelColor.SignalVoltage;
        } else {
            voltageLevelColor = VoltageLevelColor.Neutral;
        }
    }

    public boolean isSignalSource() {
        return signalSource;
    }
// 26.x port: draw() removed (client rendering; redone in P3)

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        Collections.addAll(list, tr("Provides an ideal voltage source\nwithout energy or power limitation.").split("\\\n"));
        list.add("");
        list.add(tr("Internal resistance: %1$\u2126", Utils.plotValue(ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs)));
        list.add("");
        list.add(tr("Creative block."));
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        if (signalSource) {
            return super.getFrontFromPlace(side, player).left();
        } else {
            return super.getFrontFromPlace(side, player);
        }
    }
}
