package mods.eln.transparentnode.teleporter;

import mods.eln.ghost.GhostGroup;
import mods.eln.misc.*;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.sixnode.electricalcable.ElectricalCableDescriptor;
import mods.eln.wiki.Data;
import net.minecraft.item.Item;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TeleporterDescriptor extends TransparentNodeDescriptor {

    private Obj3D obj;
    Obj3DPart main, ext_control, ext_power;
    Obj3DPart door_out, door_in, door_in_charge;
    Obj3DPart indoor_open, indoor_closed;
    private Obj3DPart outlampline0_alpha, outlampline0;
    Obj3DPart[] leds = new Obj3DPart[10];
    Obj3DPart scr0_electrictity, scr1_cables, scr2_transporter, scr3_userin, scr5_dooropen, src4_doorclosed;
    Obj3DPart gyro_alpha, gyro, whiteblur;

    public TeleporterDescriptor(
        String name, Obj3D obj,
        ElectricalCableDescriptor cable,
        Coordinate areaCoordinate, Coordinate lightCoordinate,
        int areaH,
        Coordinate[] powerCoordinate,
        GhostGroup ghostDoorOpen, GhostGroup ghostDoorClose

    ) {
        super(name, TeleporterElement.class, TeleporterRender.class);
        this.cable = cable;
        this.obj = obj;
        this.powerCoordinate = powerCoordinate;
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
        this.areaCoordinate = areaCoordinate;
        this.areaH = areaH;
        this.ghostDoorClose = ghostDoorClose;
        this.ghostDoorOpen = ghostDoorOpen;
        this.lightCoordinate = lightCoordinate;

        voltageLevelColor = VoltageLevelColor.HighVoltage;
    }

    GhostGroup ghostDoorOpen, ghostDoorClose;

    private int areaH;
    private Coordinate areaCoordinate;
    Coordinate lightCoordinate;

    AxisAlignedBB getBB(Coordinate c, Direction front) {
        Coordinate temp = new Coordinate(areaCoordinate);
        temp.setDimension(c.getDimension());
        temp.applyTransformation(front, c);

        return new AxisAlignedBB(temp.pos);
    }

    Coordinate getTeleportCoordinate(Direction front, Coordinate c) {
        Coordinate temp = new Coordinate(areaCoordinate);
        temp.setDimension(c.getDimension());
        temp.applyTransformation(front, c);

        return temp;
    }


    @Override
    public void setParent(Item item, int damage) {
        super.setParent(item, damage);
        Data.addMachine(newItemStack());
    }

    public ElectricalCableDescriptor cable;


    public void draw() {
        if (main != null) main.draw();
        if (ext_control != null) ext_control.draw();
        if (ext_power != null) ext_power.draw();
        if (door_out != null) door_out.draw();
    }

    private Coordinate[] powerCoordinate;

    Coordinate[] getPowerCoordinate(World w) {
        Coordinate[] temp = new Coordinate[powerCoordinate.length];
        for (int idx = 0; idx < temp.length; idx++) {
            temp[idx] = new Coordinate(powerCoordinate[idx]);
            temp[idx].setDimension(w.provider.getDimension());
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


    // TODO(1.10): Fix item render.
//    @Override
//    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
//        return true;
//    }
//
//    @Override
//    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item,
//                                         ItemRendererHelper helper) {
//        return type != ItemRenderType.INVENTORY;
//    }
//
//    @Override
//    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
//        if (type == ItemRenderType.INVENTORY) {
//            super.renderItem(type, item, data);
//        } else {
//            objItemScale(obj);
//            main.draw();
//            ext_control.draw();
//            ext_power.draw();
//            UtilsClient.disableCulling();
//            door_out.draw();
//            UtilsClient.enableCulling();
//            indoor_open.draw();
//        }
//    }
}
