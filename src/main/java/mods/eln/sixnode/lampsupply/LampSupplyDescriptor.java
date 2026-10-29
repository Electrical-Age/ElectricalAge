package mods.eln.sixnode.lampsupply;


import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        super(name, LampSupplyElement.class, null);
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
        float rotYaw = UtilsClient.clientPlayerYaw() / 360.f;
        float rotPitch = UtilsClient.clientPlayerPitch() / 180.f;
        float pos = (((float) UtilsClient.clientPlayerX()) + ((float) UtilsClient.clientPlayerZ())) / 64.f;
        if (window != null)
            window.draw((1f - openFactor) * windowOpenAngle, 0f, 0f, 1f, rotYaw + pos + (openFactor * 0.5f), rotPitch * 0.65f);
        UtilsClient.disableBlend();
        //UtilsClient.enableDepthTest();
        UtilsClient.enableCulling();
    }
    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    @Override
    public boolean hasItemDraw() {
        return true;
    }

    @Override
    public void drawItem(boolean entity) {
        draw(1f);

    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        list.add(tr("Supplies all lamps on the channel."));
    }

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).inverse();
    }

    /** Its window is drawn open in the model: not part of the box (Baughn 2026-10-29). */
    @Override
    protected java.util.Set<Obj3DPart> footprintIgnores() {
        return window == null ? java.util.Set.of() : java.util.Set.of(window);
    }
}
