package mods.eln.sixnode.lampsocket;

import mods.eln.misc.UtilsClient;

import mods.eln.gl.GL11;

import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.misc.Utils;
import net.minecraft.resources.Identifier;

public class LampSocketStandardObjRender implements LampSocketObjRender {

    private Obj3D obj;
    private Obj3DPart socket, socket_unlightable, socket_lightable, lampOn, lampOff, lightAlphaPlane, lightAlphaPlaneNoDepth;
    Identifier tOn, tOff;
    private boolean onOffModel;

    public LampSocketStandardObjRender(Obj3D obj, boolean onOffModel) {
        this.obj = obj;
        this.onOffModel = onOffModel;
        if (obj != null) {
            socket = obj.getPart("socket");
            lampOn = obj.getPart("lampOn");
            lampOff = obj.getPart("lampOff");
            socket_unlightable = obj.getPart("socket_unlightable");
            socket_lightable = obj.getPart("socket_lightable");
            lightAlphaPlane = obj.getPart("lightAlpha");
            lightAlphaPlaneNoDepth = obj.getPart("lightAlphaNoDepth");
            tOff = obj.getModelResourceLocation(obj.getString("tOff"));
            tOn = obj.getModelResourceLocation(obj.getString("tOn"));
        }
    }

    // 26.x port: draw() removed (client rendering; redone in P3)

    // 26.x port: draw() removed (client rendering; redone in P3)
// 26.x port: draw() removed (client rendering; redone in P3)
}
