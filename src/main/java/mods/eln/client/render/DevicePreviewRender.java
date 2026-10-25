package mods.eln.client.render;

import mods.eln.Eln;
import mods.eln.gl.GL11;
import mods.eln.gl.GlFrame;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.six.SixNodeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * With a chip (a device that goes anywhere on a face) in hand: its model, see-through, where a right-click would put it;
 * reddish where it may not go (node.voxel.DevicePreview decides, by the placement rules).
 */
public final class DevicePreviewRender {
    private DevicePreviewRender() {
    }

    private record Shown(boolean ok, BlockPos pos, Direction side, int ox, int oy, int oz, LRDU front, int id) {}

    private static volatile Shown shown;

    public static void read(DataInputStream in) throws IOException {
        int state = in.readByte();
        if (state == 0) {
            shown = null;
            return;
        }
        BlockPos pos = new BlockPos(in.readInt(), in.readInt(), in.readInt());
        Direction side = Direction.fromInt(in.readByte());
        int ox = in.readByte(), oy = in.readByte(), oz = in.readByte();
        LRDU front = LRDU.fromInt(in.readByte());
        int id = in.readShort();
        shown = side == null || front == null ? null : new Shown(state == 1, pos, side, ox, oy, oz, front, id);
    }

    public static void onExtract(ExtractBlockOutlineRenderStateEvent e) {
        var player = Minecraft.getInstance().player;
        Shown s = shown;
        if (s == null || player == null || !(player.getMainHandItem().getItem() instanceof SixNodeItem item)
            || item.descriptor.parentItemDamage != s.id) return;
        var d = item.descriptor;
        e.addCustomRenderer((state, collector, pose, lrs) -> {
            Vec3 cam = lrs.cameraRenderState.pos;
            pose.pushPose();
            pose.translate(s.pos.getX() - cam.x, s.pos.getY() - cam.y, s.pos.getZ() - cam.z);
            GlFrame frame = mods.eln.client.render.GlSubmit.open(pose);
            try {
                frame.ghost = s.ok ? 0x99FFFFFF : 0x99FF5050;
                GL11.glTranslatef(.5f + s.ox / 16f, .5f + s.oy / 16f, .5f + s.oz / 16f);
                s.side.glRotateXnRef();
                GL11.glTranslatef(-0.5f, 0f, 0f);
                d.drawPlaced(s.front);
            } catch (RuntimeException ex) {
                ElnRenderErrors.report("device preview", ex);
            } finally {
                GlFrame.close();
            }
            mods.eln.client.render.GlSubmit.submit(frame, pose, collector, 0xF000F0);
            pose.popPose();
            return false;
        });
    }
}
