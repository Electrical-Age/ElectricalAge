package mods.eln.sixnode.powerinductorsix;

import mods.eln.misc.UtilsClient;



import mods.eln.registry.ElnDeviceRegistry;
import mods.eln.misc.Utils;
import mods.eln.Eln;
import mods.eln.item.FerromagneticCoreDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.VoltageLevelColor;
import mods.eln.misc.series.ISerie;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.sim.mna.misc.MnaConst;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class PowerInductorSixDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    Obj3DPart InductorBaseExtention, InductorCables, InductorCore, Base;

    ISerie serie;

    public PowerInductorSixDescriptor(String name,
                                      Obj3D obj,
                                      ISerie serie) {
        super(name, PowerInductorSixElement.class, null);
        this.serie = serie;
        this.obj = obj;
        if (obj != null) {
            InductorBaseExtention = obj.getPart("InductorBaseExtention");
            InductorCables = obj.getPart("InductorCables");
            InductorCore = obj.getPart("InductorCore");
            Base = obj.getPart("Base");
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public double getlValue(int cableCount) {
        if (cableCount == 0) return 0;
        return serie.getValue(cableCount - 1);
    }

    public double getlValue(Container inventory) {
        ItemStack core = inventory.getItem(PowerInductorSixContainer.cableId);
        if (Utils.isEmpty(core))
            return getlValue(0);
        else
            return getlValue(core.getCount());
    }

    public double getRsValue(Container inventory) {
        ItemStack core = inventory.getItem(PowerInductorSixContainer.coreId);

        if (Utils.isEmpty(core)) return MnaConst.highImpedance;
        FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

        double coreFactor = coreDescriptor.cableMultiplicator;

        return ElnDeviceRegistry.lowVoltageCableDescriptor.electricalRs * coreFactor;
    }

    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addEnergy(newItemStack());
    }

    void draw() {
        //UtilsClient.disableCulling();
        //UtilsClient.disableTexture();
        if (null != Base) Base.draw();
        if (null != InductorBaseExtention) InductorBaseExtention.draw();
        if (null != InductorCables) InductorCables.draw();
        if (null != InductorCore) InductorCore.draw();
    }

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
