package mods.eln.sixnode.lampsocket;

import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL11;

public class LampSocketSuspendedObjRender implements LampSocketObjRender {

    private Obj3D obj;
    private Obj3DPart socket, chain, base, lightAlphaPlaneNoDepth;
    Identifier tOn, tOff;
    private boolean onOffModel;
    private int length;
    float baseLength, chainLength, chainFactor;

    public LampSocketSuspendedObjRender(Obj3D obj, boolean onOffModel, int length) {
        this.obj = obj;
        this.length = length;
        this.onOffModel = onOffModel;
        if (obj != null) {
            socket = obj.getPart("socket");
            chain = obj.getPart("chain");
            base = obj.getPart("base");
            lightAlphaPlaneNoDepth = obj.getPart("lightAlphaNoDepth");
            tOff = obj.getModelResourceLocation(obj.getString("tOff"));
            tOn = obj.getModelResourceLocation(obj.getString("tOn"));
            chainLength = chain.getFloat("length");
            chainFactor = chain.getFloat("factor");
            baseLength = base.getFloat("length");
        }
    }

    // 26.x port: draw() removed (client rendering; redone in P3)

    // 26.x port: draw() removed (client rendering; redone in P3)
// 26.x port: draw() removed (client rendering; redone in P3)
}
