package mods.eln.anvil;

import mods.eln.gl.GL11;
import mods.eln.misc.Utils;
import mods.eln.misc.UtilsClient;
import mods.eln.node.transparent.TransparentNodeDescriptor;
import mods.eln.node.transparent.TransparentNodeElementRender;
import mods.eln.node.transparent.TransparentNodeEntity;
import mods.eln.sound.LoopedSound;
import net.minecraft.world.item.ItemStack;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** The electrical anvil (ElectricalAnvilDescriptor): the desk, the tool lying on it, sparks and the arc's sizzle while it welds. */
public class ElectricalAnvilRender extends TransparentNodeElementRender {
    final ElectricalAnvilDescriptor desc;
    public byte status;
    public float progress, powerW, voltage, penalty, floor;
    public boolean welding;
    /** The name asked for (null: none), the credit per material id, the tool. */
    public String pendingName;
    public final Map<String, Float> credit = new LinkedHashMap<>();
    public ItemStack tool = ItemStack.EMPTY;
    final mods.eln.anim.Emitter sparks = new mods.eln.anim.Emitter();
    static final java.util.Random RND = new java.util.Random();

    public ElectricalAnvilRender(TransparentNodeEntity entity, TransparentNodeDescriptor d) {
        super(entity, d);
        desc = (ElectricalAnvilDescriptor) d;
        addLoopedSound(new LoopedSound("eln:arc_weld", coordonate()) {
            @Override
            public float getVolume() {
                return welding ? 0.35f : 0;
            }

            @Override
            public float getPitch() {
                return status == ElectricalAnvilElement.ENGRAVING ? 1.4f : 1f;   // (engraving: a finer arc)
            }
        });
    }

    public void clientSetName(String name) {
        clientSendString(ElectricalAnvilElement.SET_NAME, name);
    }

    @Override
    public void draw() {
        front.glRotateXnRef();
        desc.draw();
        float[] t = desc.point("tool");
        if (t != null && !tool.isEmpty()) {
            // the tool lies face up on the work plate (drawFlatItem's face is +x: turned up)
            GL11.glPushMatrix();
            GL11.glTranslatef(t[0], t[1] + 0.02f, t[2]);
            GL11.glRotatef(90, 0, 0, 1);
            UtilsClient.drawFlatItem(tool, 0, 0, 0, 0.42f, 3f);
            GL11.glPopMatrix();
        }
    }

    @Override
    public void refresh(float deltaT) {
        var level = tileEntity.getLevel();
        float[] v = desc.point("spark");
        int n = sparks.step(deltaT, welding && v != null ? 30 : 0);
        if (level == null || n == 0) return;
        var at = mods.eln.anim.Emitter.vent(tileEntity.getBlockPos(), front, v[0], v[1], v[2]);
        double x = at.x, y = at.y, z = at.z;
        for (int i = 0; i < n; i++) {
            double vx = (RND.nextDouble() - 0.5) * 0.25, vz = (RND.nextDouble() - 0.5) * 0.25;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, y, z, vx, 0.05 + RND.nextDouble() * 0.15, vz);
        }
    }

    @Override
    public void networkUnserialize(DataInputStream stream) {
        super.networkUnserialize(stream);
        try {
            status = stream.readByte();
            progress = stream.readFloat();
            powerW = stream.readFloat();
            voltage = stream.readFloat();
            welding = stream.readBoolean();
            String n = stream.readUTF();
            pendingName = n.equals("\u0000") ? null : n;
            credit.clear();
            int k = stream.readByte();
            for (int i = 0; i < k; i++) credit.put(stream.readUTF(), stream.readFloat());
            penalty = stream.readInt();
            floor = stream.readInt();
            ItemStack s = Utils.unserialiseItemStack(stream);
            tool = s == null ? ItemStack.EMPTY : s;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
