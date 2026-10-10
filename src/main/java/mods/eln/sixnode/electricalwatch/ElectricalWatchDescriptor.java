package mods.eln.sixnode.electricalwatch;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.six.SixNodeDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public class ElectricalWatchDescriptor extends SixNodeDescriptor {

    private Obj3DPart base, cHour, cMin; //Analog
    private Obj3DPart digits[] = new Obj3DPart[4]; //Digital
    private Obj3DPart dot, glass; //Digital

    enum Kind {ANALOG, DIGITAL}

    ;
    private Kind kind;
    double powerConsumtion;

    Obj3D obj;

    public ElectricalWatchDescriptor(String name, Obj3D obj, double powerConsumtion) {
        super(name, ElectricalWatchElement.class, null);
        this.obj = obj;
        this.powerConsumtion = powerConsumtion;
        kind = Kind.valueOf(obj.getString("type").toUpperCase(Locale.ROOT));
        if (obj != null) {
            if (kind == Kind.ANALOG) {
                base = obj.getPart("base");
                cHour = obj.getPart("cHour");
                cMin = obj.getPart("cMin");
            } else if (kind == Kind.DIGITAL) {
                base = obj.getPart("base");
                glass = obj.getPart("glass");
                dot = obj.getPart("digitDot");
                digits[3] = obj.getPart("digit3");
                digits[2] = obj.getPart("digit2");
                digits[1] = obj.getPart("digit1");
                digits[0] = obj.getPart("digit0");
            }
        }

        voltageLevelColor = VoltageLevelColor.Neutral;
    }
    // 26.x port: draw() removed (client rendering; redone in P3)
    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        //Data.addSignal(newItemStack());
    }

    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelperEln() removed (client rendering; redone in P3)
    // 26.x port: renderItem() removed (client rendering; redone in P3)

    @Override
    public LRDU getFrontFromPlace(Direction side, Player player) {
        return super.getFrontFromPlace(side, player).left();
    }
}
