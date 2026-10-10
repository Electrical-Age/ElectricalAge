package mods.eln.transparentnode.teleporter;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

public class TeleporterDescriptor extends TransparentNodeDescriptor {

    private Obj3D obj;
    public Obj3DPart main, ext_control, ext_power;
    public Obj3DPart door_out, door_in, door_in_charge;
    public Obj3DPart indoor_open, indoor_closed;
    public Obj3DPart outlampline0_alpha, outlampline0;
    public Obj3DPart[] leds = new Obj3DPart[10];
    public Obj3DPart scr0_electrictity, scr1_cables, scr2_transporter, scr3_userin, scr5_dooropen, src4_doorclosed;
    public Obj3DPart gyro_alpha, gyro, whiteblur;

    public TeleporterDescriptor(
        String name, Obj3D obj,
        ElectricalCableDescriptor cable,
        Coordonate areaCoordonate, Coordonate lightCoordonate,
        int areaH,
        Coordonate[] powerCoordonate,
        GhostGroup ghostDoorOpen, GhostGroup ghostDoorClose

    ) {
        super(name, TeleporterElement.class, null);
        this.cable = cable;
        this.obj = obj;
        this.powerCoordonate = powerCoordonate;
        if (obj != null) {
            main = obj.getPart("main");
            ext_control = obj.getPart("ext_control");
            ext_power = obj.getPart("ext_power");
            door_out = obj.getPart("door_out");
            door_in_charge = obj.getPart("door_in_charge");
            door_in = obj.getPart("door_in");
            indoor_closed = obj.getPart("indoor_closed");
            indoor_open = obj.getPart("indoor_open");
            outlampline0_alpha = obj.getPart("outlampline0_alpha");
            outlampline0 = obj.getPart("outlampline0");
            scr0_electrictity = obj.getPart("scr0_electrictity");
            scr1_cables = obj.getPart("scr1_cables");
            scr2_transporter = obj.getPart("scr2_transporter");
            scr3_userin = obj.getPart("scr3_userin");
            scr5_dooropen = obj.getPart("scr5_dooropen");
            src4_doorclosed = obj.getPart("src4_doorclosed");
            gyro_alpha = obj.getPart("gyro_alpha");
            gyro = obj.getPart("gyro");
            whiteblur = obj.getPart("whiteblur");

            for (int idx = 0; idx < 10; idx++) {
                leds[idx] = obj.getPart("led" + idx);
            }
        }
        this.areaCoordonate = areaCoordonate;
        this.areaH = areaH;
        this.ghostDoorClose = ghostDoorClose;
        this.ghostDoorOpen = ghostDoorOpen;
        this.lightCoordonate = lightCoordonate;

        voltageLevelColor = VoltageLevelColor.HighVoltage;
    }

    public GhostGroup ghostDoorOpen, ghostDoorClose;

    int areaH;
    public Coordonate areaCoordonate, lightCoordonate;

    public AABB getBB(Coordonate c, Direction front) {
        Coordonate temp = new Coordonate(areaCoordonate);
        temp.setDimention(c.dimention);
        temp.applyTransformation(front, c);

        AABB bb = new AABB(temp.x, temp.y, temp.z, temp.x + 1, temp.y + areaH, temp.z + 1);
        return bb;
    }

    public Coordonate getTeleportCoordonate(Direction front, Coordonate c) {
        Coordonate temp = new Coordonate(areaCoordonate);
        temp.setDimention(c.dimention);
        temp.applyTransformation(front, c);

        return temp;
    }


    @Override
    public void setParent(Item item, int damage) {

        super.setParent(item, damage);
        Data.addMachine(newItemStack());
    }

    public ElectricalCableDescriptor cable;
// 26.x port: draw() removed (client rendering; redone in P3)

    Coordonate[] powerCoordonate;

    public Coordonate[] getPowerCoordonate(Level w) {
        Coordonate[] temp = new Coordonate[powerCoordonate.length];
        for (int idx = 0; idx < temp.length; idx++) {
            temp[idx] = new Coordonate(powerCoordonate[idx]);
            temp[idx].setDimention(mods.eln.misc.Dimensions.id(w));
        }
        return temp;
    }


    @Override
    public int getSpawnDeltaX() {

        return 4;
    }

    String chargeSound = null;
    float chargeVolume = 0;

    public TeleporterDescriptor setChargeSound(String sound, float volume) {
        chargeSound = sound;
        chargeVolume = volume;
        return this;
    }


    // 26.x port: handleRenderType() removed (client rendering; redone in P3)

    // 26.x port: shouldUseRenderHelper() removed (client rendering; redone in P3)

    // 26.x port: renderItem() removed (client rendering; redone in P3)
}
