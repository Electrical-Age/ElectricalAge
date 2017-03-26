package mods.eln.sixnode.lampsupply;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class LampSupplyDescriptor extends SixNodeDescriptor {

    private Obj3D obj;
    Obj3DPart base;
    private Obj3DPart window;
    private float windowOpenAngle;
    public boolean isWireless;
    public int range;
    public int channelCount = 3;

    public LampSupplyDescriptor(String name, Obj3D obj, int range) {
        super(name, LampSupplyElement.class, LampSupplyRender.class);
        this.isWireless = isWireless;
        this.range = range;
        this.obj = obj;
        if (obj != null) {
            base = obj.getPart("base");
            window = obj.getPart("window");
        }
        if (window != null) {
            windowOpenAngle = window.getFloat("windowOpenAngle");
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    @Override
    public boolean use2DIcon() {
        return true;
    }

    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addLight(newItemStack(1));
    }

    public void draw(float openFactor) {
        if (base != null) base.draw();
        //UtilsClient.drawLight(led);
        UtilsClient.disableCulling();
        //UtilsClient.disableDepthTest();
        UtilsClient.enableBlend();
        obj.bindTexture("Glass.png");
        float rotYaw = Minecraft.getMinecraft().thePlayer.rotationYaw / 360.f;
        float rotPitch = Minecraft.getMinecraft().thePlayer.rotationPitch / 180.f;
        float pos = (((float) Minecraft.getMinecraft().thePlayer.posX) + ((float) Minecraft.getMinecraft().thePlayer.posZ)) / 64.f;
        if (window != null)
            window.draw((1f - openFactor) * windowOpenAngle, 0f, 0f, 1f, rotYaw + pos + (openFactor * 0.5f), rotPitch * 0.65f);
        UtilsClient.disableBlend();
        //UtilsClient.enableDepthTest();
        UtilsClient.enableCulling();
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelperEln(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return type != ItemRenderType.INVENTORY;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        if (type == ItemRenderType.INVENTORY) {
            super.renderItem(type, item, data);
        } else {
            draw(1f);
        }
    }

    @Override
    public void addInformation(ItemStack itemStack, EntityPlayer entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Supplies all lamps on the channel."));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, EntityPlayer player) {
        return super.getFrontFromPlace(side, player).inverse();
    }
}
