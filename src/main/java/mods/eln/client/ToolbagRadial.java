package mods.eln.client;

import mods.eln.client.gui.RadialMenu;
import mods.eln.network.ElnNet;
import mods.eln.node.voxel.ToolbagItem;
import mods.eln.node.voxel.ToolbagItem.Kind;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Toolbag's radial (packetToolbagRadial from ToolbagItem.openRadial): the inner ring the kinds it holds by pipe or
 * cable voltage (and kind), the next the cross-sections of the power cable picked, the outer their colours. A pick that
 * leaves no choice on the rings beyond takes the kind (packetToolbagChoice), as does the centre; Esc keeps the old one.
 * With a route on, the inner ring also has Undo (the last waypoint) and Cancel.
 */
final class ToolbagRadial {
    private ToolbagRadial() {
    }

    static void open(DataInputStream in) throws IOException {
        boolean routing = in.readBoolean();
        int[] sel = {in.readByte(), in.readByte(), in.readByte(), in.readByte()};
        int n = in.readShort();
        Map<Kind, Integer> kinds = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            Kind k = new Kind(in.readByte() == 1, in.readByte(), in.readByte(), in.readByte());
            kinds.put(k, in.readInt());
        }
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) return;
        mc.gui.setScreen(create(routing, new Kind(sel[0] == 1, sel[1], sel[2], sel[3]), kinds));
    }

    /** A first-ring key's place: pipes and non-power cables by tier, power tiers by voltage (between 48 V's index 1 and
     *  11 kV's 4: 1 kV, index 7, sorts as 3.25). */
    static double order(Kind k) {
        if (k.pipe() || !mods.eln.node.voxel.CableSpec.power(k.tier())) return k.tier();
        return 1 + mods.eln.node.voxel.CableSpec.rank(k.tier()) * 0.75;
    }

    static RadialMenu create(boolean routing, Kind sel, Map<Kind, Integer> kinds) {
        // the first ring's keys: pipes, then cables, each by tier
        List<Kind> firsts = new ArrayList<>();
        for (Kind k : kinds.keySet()) {
            Kind f = new Kind(k.pipe(), k.tier(), -1, -1);
            if (!firsts.contains(f)) firsts.add(f);
        }
        // (cables by tier, power tiers in voltage order: the 1 kV tier is index 7)
        firsts.sort((a, b) -> a.pipe() != b.pipe() ? (a.pipe() ? -1 : 1) : Double.compare(order(a), order(b)));
        Kind[] cur = {kinds.containsKey(sel) ? sel : kinds.isEmpty() ? null : kinds.keySet().iterator().next()};
        boolean[] act = {false};   // (Undo or Cancel sent: nothing to commit)
        RadialMenu[] menu = new RadialMenu[1];
        List<RadialMenu.Segment> ringA = new ArrayList<>(), ringB = new ArrayList<>(), ringC = new ArrayList<>();
        Runnable[] refill = new Runnable[1];
        refill[0] = () -> {
            ringB.clear();
            ringC.clear();
            if (cur[0] == null || cur[0].mm2() < 0) return;
            List<Integer> sizes = new ArrayList<>(), colours = new ArrayList<>();
            for (Kind k : kinds.keySet()) {
                if (k.pipe() != cur[0].pipe() || k.tier() != cur[0].tier()) continue;
                if (!sizes.contains(k.mm2())) sizes.add(k.mm2());
                if (k.mm2() == cur[0].mm2() && !colours.contains(k.colour())) colours.add(k.colour());
            }
            sizes.sort(null);
            for (int m : sizes) {
                Kind label = new Kind(false, cur[0].tier(), m, 0);
                ringB.add(new RadialMenu.Segment(Component.literal(label.size()), () -> cur[0] != null && cur[0].mm2() == m, () -> {
                    cur[0] = pick(kinds, cur[0].tier(), m, cur[0].colour());
                    refill[0].run();
                    if (ringC.size() <= 1) menu[0].commitAndClose();
                }));
            }
            for (int c : colours) {
                Kind label = new Kind(false, cur[0].tier(), 0, c);
                ringC.add(new RadialMenu.Segment(Component.literal(label.colourName()), () -> cur[0] != null && cur[0].colour() == c, () -> {
                    cur[0] = pick(kinds, cur[0].tier(), cur[0].mm2(), c);
                    menu[0].commitAndClose();
                }));
            }
        };
        for (Kind f : firsts)
            ringA.add(new RadialMenu.Segment(Component.literal(f.first()), () -> cur[0] != null && cur[0].pipe() == f.pipe() && cur[0].tier() == f.tier(), () -> {
                Kind keep = cur[0];
                cur[0] = null;
                for (Kind k : kinds.keySet())
                    if (k.pipe() == f.pipe() && k.tier() == f.tier() && (cur[0] == null || keep != null && k.mm2() == keep.mm2() && k.colour() == keep.colour()))
                        cur[0] = k;
                refill[0].run();
                if (ringB.size() <= 1 && ringC.size() <= 1) menu[0].commitAndClose();
            }));
        if (routing) {
            ringA.add(new RadialMenu.Segment(Component.literal(mods.eln.i18n.I18N.tr("Undo")), () -> false, () -> send(ToolbagItem.UNDO, null, act, menu)));
            ringA.add(new RadialMenu.Segment(Component.literal(mods.eln.i18n.I18N.tr("Cancel")), () -> false, () -> send(ToolbagItem.CANCEL, null, act, menu)));
        }
        refill[0].run();
        menu[0] = new RadialMenu(Component.translatable("gui.eln.toolbag.radial"), List.of(ringA, ringB, ringC),
            () -> cur[0] == null ? Component.literal("-") : Component.literal(String.format("%s %.0f %s", cur[0].pipe() || cur[0].mm2() < 0 ? cur[0].first()
                : cur[0].size(), kinds.getOrDefault(cur[0], 0) / 16.0, mods.eln.i18n.I18N.tr("bl"))),
            ClientKeys.WRENCH, () -> {
                if (act[0] || cur[0] == null) return;
                ElnNet.sendToServer(ToolbagItem.choicePacket(ToolbagItem.CHOOSE, new int[]{cur[0].pipe() ? 1 : 0, cur[0].tier(), cur[0].mm2(), cur[0].colour()}));
            }, true);
        return menu[0];
    }

    /** The held kind of that cable tier nearest the wish: that size and colour, else that size, else any. */
    static Kind pick(Map<Kind, Integer> kinds, int tier, int mm2, int colour) {
        Kind best = null;
        int score = -1;
        for (Kind k : kinds.keySet()) {
            if (k.pipe() || k.tier() != tier) continue;
            int s = (k.mm2() == mm2 ? 2 : 0) + (k.colour() == colour ? 1 : 0);
            if (s > score) {
                score = s;
                best = k;
            }
        }
        return best;
    }

    static void send(int action, int[] kind, boolean[] act, RadialMenu[] menu) {
        act[0] = true;
        ElnNet.sendToServer(ToolbagItem.choicePacket(action, kind));
        menu[0].commitAndClose();
    }
}
