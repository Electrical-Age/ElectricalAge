package mods.eln.sixnode.lampsocket;

import mods.eln.misc.VoltageLevelColor;
import mods.eln.node.six.SixNodeDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.List;

import static mods.eln.i18n.I18N.tr;

public class LampSocketDescriptor extends SixNodeDescriptor {

    public LampSocketType socketType;
    LampSocketObjRender render;

    public boolean cameraOpt = true;

    public int range;
    public String modelName;
    float alphaZMin, alphaZMax, alphaZBoot;
    public boolean cableFront = true;
    public boolean cableLeft = true;
    public boolean cableRight = true;
    public boolean cableBack = true;

    public float initialRotateDeg = 0.f;
    public boolean rotateOnlyBy180Deg = false;

    public boolean paintable = false;

    public LampSocketDescriptor(String name, LampSocketObjRender render,
                                LampSocketType socketType,
                                boolean paintable,
                                int range,
                                float alphaZMin, float alphaZMax,
                                float alphaZBoot) {
        super(name, LampSocketElement.class, null);
        this.socketType = socketType;
        this.paintable = paintable;
        this.range = range;
        this.alphaZMin = alphaZMin;
        this.alphaZMax = alphaZMax;
        this.alphaZBoot = alphaZBoot;
        this.render = render;

        voltageLevelColor = VoltageLevelColor.Neutral;
    }

    public void setInitialOrientation(float rotateDeg) {
        this.initialRotateDeg = rotateDeg;
    }

    public void setUserRotationLibertyDegrees(boolean only180) {
        this.rotateOnlyBy180Deg = only180;
    }

    boolean noCameraOpt() {
        return cameraOpt;
    }

    public void setParent(net.minecraft.world.item.Item item, int damage) {
        super.setParent(item, damage);
        Data.addLight(newItemStack());
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public boolean hasVolume() {
        return hasGhostGroup();
    }

    @Override
    public void addInformation(ItemStack itemStack, Player entityPlayer, List list, boolean par4) {
        super.addInformation(itemStack, entityPlayer, list, par4);
        //list.add("Socket Type : " + socketType.toString());

        if (range != 0 || alphaZMin != alphaZMax) {
            //list.add("Projector");
            if (range != 0) {
                list.add(tr("Spot range: %1$ blocks", range));
            }
            if (alphaZMin != alphaZMax) {
                list.add(tr("Angle: %1$° to %2$°", ((int) alphaZMin), ((int) alphaZMax)));
            }
        }
    }
}
