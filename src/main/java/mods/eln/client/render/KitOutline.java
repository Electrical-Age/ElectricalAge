package mods.eln.client.render;

import mods.eln.node.voxel.KitItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** With a cable kit in hand: the server's preview of the next click (node.voxel.KitPreview), green if it would work. */
public final class KitOutline {
    private KitOutline() {
    }

    private static volatile List<int[]> boxes = List.of();
    private static volatile int state = 1;

    public static void read(DataInputStream in) throws IOException {
        int k = in.readByte();   // (0 won't work, 1 works, 2 connects to a machine: KitPreview.send)
        int n = in.readShort();
        List<int[]> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) l.add(new int[]{in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt(), in.readInt()});
        state = k;
        boxes = l;
    }

    public static void onExtract(ExtractBlockOutlineRenderStateEvent e) {
        var player = Minecraft.getInstance().player;
        List<int[]> bs = boxes;
        if (player == null || bs.isEmpty() || !(player.getMainHandItem().getItem() instanceof KitItem)) return;   // (cable and pipe kits)
        int st = state;
        int color = st == 2 ? 0xE040D8FF : st == 1 ? 0xC040FF60 : 0xC0FF4040;   // (cyan: into a machine's port)
        e.addCustomRenderer((state, collector, pose, lrs) -> {
            Vec3 cam = lrs.cameraRenderState.pos;
            for (int[] b : bs) {
                pose.pushPose();
                pose.translate(b[0] / 16.0 - cam.x, b[1] / 16.0 - cam.y, b[2] / 16.0 - cam.z);
                collector.submitShapeOutline(pose, Shapes.box(0, 0, 0, (b[3] - b[0]) / 16.0, (b[4] - b[1]) / 16.0, (b[5] - b[2]) / 16.0),
                    RenderTypes.linesTranslucent(), color, 2.5f, false);
                pose.popPose();
            }
            return false;   // (and the block's own outline)
        });
    }
}
