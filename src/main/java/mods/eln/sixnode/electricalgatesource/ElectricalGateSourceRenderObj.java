package mods.eln.sixnode.electricalgatesource;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;


import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.sixnode.electricalgatesource.ElectricalGateSourceDescriptor.ObjType;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.awt.*;

public class ElectricalGateSourceRenderObj {

    private Obj3DPart rot;
    private Obj3DPart main;
    private Obj3D obj;
    private Obj3DPart lever;
    private Obj3DPart led;
    private Obj3DPart halo;
    ObjType objType;
    float leverTx;

    private float rotAlphaOn, rotAlphaOff;
    public float speed;

    public ElectricalGateSourceRenderObj(Obj3D obj) {
        this.obj = obj;
        if (obj != null) {
            main = obj.getPart("main");

            if (obj.getString("type").equals("pot")) {
                objType = ObjType.Pot;
                rot = obj.getPart("rot");
                if (rot != null) {
                    rotAlphaOff = rot.getFloat("alphaOff");
                    rotAlphaOn = rot.getFloat("alphaOn");
                    speed = rot.getFloat("speed");
                }
            }

            if (obj.getString("type").equals("button")) {
                lever = obj.getPart("button");
                led = obj.getPart("led");
                halo = obj.getPart("halo");

                objType = ObjType.Button;
                if (lever != null) {
                    speed = lever.getFloat("speed");
                    leverTx = lever.getFloat("tx");
                }
            }
        }
    }
    // 26.x port: draw() removed (client rendering; redone in P3)
}
