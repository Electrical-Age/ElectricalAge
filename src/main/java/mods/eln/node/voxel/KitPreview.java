package mods.eln.node.voxel;

import mods.eln.Eln;
import mods.eln.network.ElnNet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the next click with a cable kit would do, shown as outlines (Baughn 2026-10-18: no preview of where it goes;
 * like the chisel's): before a route starts, the end it would start at; during one, the whole route to where the
 * player aims (the router runs on the server, so a few times a second, and only when the aim or the plan changes).
 * Sent as an EA packet (Eln.packetKitPreview): ok, then boxes in world voxels; drawn by client.render.KitOutline.
 */
public final class KitPreview {
    private KitPreview() {
    }

    /** Per player: what was last computed (key) and sent. */
    private record Last(String key, boolean sentEmpty) {}

    private static final Map<UUID, Last> LAST = new HashMap<>();

    public static void forget(UUID player) {
        LAST.remove(player);
    }

    public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level) || p.tickCount % 4 != 0) return;
        Last last = LAST.get(p.getUUID());
        if (!(p.getMainHandItem().getItem() instanceof CableKitItem kit)) {
            if (last != null && !last.sentEmpty) {
                send(p, true, List.of());
                LAST.put(p.getUUID(), new Last("", true));
            }
            return;
        }
        HitResult h = p.pick(p.blockInteractionRange(), 1f, false);
        CablePlacer.Plan plan = CablePlacer.PLANS.get(p.getUUID());
        String key = h instanceof BlockHitResult bh && h.getType() == HitResult.Type.BLOCK
            ? bh.getBlockPos().toShortString() + bh.getDirection() + (int) Math.floor(bh.getLocation().x * 8) + "," + (int) Math.floor(bh.getLocation().y * 8)
            + "," + (int) Math.floor(bh.getLocation().z * 8) : "none";
        key += "|" + kit.tier + "|" + (plan == null ? "-" : plan.start + "" + plan.waypoints.size() + plan.touched) + "|" + p.isSecondaryUseActive();
        if (last != null && last.key.equals(key)) return;
        boolean ok = true;
        String why = null;   // (in the action bar too: in stopped land the outlines' colours are drained grey)
        List<int[]> boxes = new ArrayList<>();
        if (h instanceof BlockHitResult bh && h.getType() == HitResult.Type.BLOCK && !p.isSecondaryUseActive()) {
            long[][] join = new long[1][];
            CableRouter.End at = CablePlacer.endAt(level, bh.getBlockPos(), bh.getDirection(), bh.getLocation(), plan == null, join);
            if (at == null) {
                ok = false;
                why = "Not a place for a cable";
            }
            else if (plan == null || plan.tier != kit.tier) boxes.add(marker(at));
            else {
                CableRouter.Result r = CablePlacer.route(level, plan, at, join[0]);
                if (r.error != null) {
                    ok = false;
                    why = "Can't reach there: " + r.error;
                    boxes.add(marker(at));
                } else {
                    for (CableRouter.Piece pc : CableRouter.pieces(r.steps)) {
                        CablePart part = pc.face() < 0 ? CablePart.knuckle(kit.tier, pc.points()[0], pc.points()[1], pc.points()[2])
                            : new CablePart(kit.tier, pc.face(), pc.points());
                        for (int[] b : part.boxes())
                            boxes.add(new int[]{(int) pc.bx() * 16 + b[0], (int) pc.by() * 16 + b[1], (int) pc.bz() * 16 + b[2],
                                (int) pc.bx() * 16 + b[3], (int) pc.by() * 16 + b[4], (int) pc.bz() * 16 + b[5]});
                    }
                    int len = r.length();
                    if (!p.isCreative() && KitItem.length(p.getMainHandItem()) < len) {
                        ok = false;
                        why = String.format("Too long: %.1f blocks of cable, the kit holds %.1f", len / 16.0, KitItem.length(p.getMainHandItem()) / 16.0);
                    }
                }
            }
        }
        send(p, ok, boxes);
        if (why != null) p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(why));
        LAST.put(p.getUUID(), new Last(key, boxes.isEmpty()));
    }

    /** A small cube round an end point (world voxels). */
    static int[] marker(CableRouter.End at) {
        return new int[]{(int) at.x() - 1, (int) at.y() - 1, (int) at.z() - 1, (int) at.x() + 1, (int) at.y() + 1, (int) at.z() + 1};
    }

    static void send(ServerPlayer p, boolean ok, List<int[]> boxes) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(Eln.packetKitPreview);
            out.writeBoolean(ok);
            int n = Math.min(boxes.size(), 2000);
            out.writeShort(n);
            for (int i = 0; i < n; i++) for (int v : boxes.get(i)) out.writeInt(v);
        } catch (IOException ignored) {
        }
        ElnNet.sendToPlayer(p, bos);
    }
}
