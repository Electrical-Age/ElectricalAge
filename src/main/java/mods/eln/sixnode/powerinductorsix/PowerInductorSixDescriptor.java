package mods.eln.sixnode.powerinductorsix;

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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class PowerInductorSixDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    Obj3DPart InductorBaseExtention, InductorCables, InductorCore, Base;

    ISerie serie;

    public PowerInductorSixDescriptor(String name,
                                      Obj3D obj,
                                      ISerie serie) {
        super(name, PowerInductorSixElement.class, PowerInductorSixRender.class);
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

    public double getlValue(IInventory inventory) {
        ItemStack core = inventory.getStackInSlot(PowerInductorSixContainer.cableId);
        return getlValue(core.getCount());
    }

    public double getRsValue(IInventory inventory) {
        ItemStack core = inventory.getStackInSlot(PowerInductorSixContainer.coreId);

        if (core.isEmpty()) return MnaConst.highImpedance;
        FerromagneticCoreDescriptor coreDescriptor = (FerromagneticCoreDescriptor) FerromagneticCoreDescriptor.getDescriptor(core);

        double coreFactor = coreDescriptor.cableMultiplicator;

        return Eln.instance.lowVoltageCableDescriptor.electricalRs * coreFactor;
    }

    public void setParent(net.minecraft.item.Item item, int damage) {
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

    // TODO(1.10): Fix item render.
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
//        return type != ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return true;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (type != ItemRenderType.INVENTORY) {
//            GL11.glTranslatef(0.0f, 0.0f, -0.2f);
//            GL11.glScalef(1.25f, 1.25f, 1.25f);
//            GL11.glRotatef(-90.f, 0.f, 1.f, 0.f);
//            draw();
//        } else {
//            super.renderItem(type, item, data);
//        }
//    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
