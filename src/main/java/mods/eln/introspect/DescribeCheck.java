package mods.eln.introspect;

import mods.eln.misc.Dimensions;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * The GameTest harness's describe pass: every node and describable entity in a test's bounds is described, and each
 * report must pass
 * {@link Report#problems()} and carry no describe_failed fault. Runs after a case's measure, like the persistence pass.
 */
public final class DescribeCheck {
    private DescribeCheck() {
    }

    public interface Checker {
        void check(String what, boolean ok, String detail);
    }

    public static void run(ServerLevel level, AABB bounds, Checker checker) {
        int ok = 0, total = 0;
        for (NodeBase n : new ArrayList<>(NodeManager.instance.getNodes())) {
            if (n.isDestructing() || n.coordonate.dimention != Dimensions.id(level)) continue;
            if (!bounds.contains(n.coordonate.x + .5, n.coordonate.y + .5, n.coordonate.z + .5)) continue;
            List<String> bad = new ArrayList<>();
            try {
                for (Report r : Describe.at(level, n.coordonate.pos())) {
                    String kind = r.id().get(Keys.KIND) instanceof Value.Text t ? t.v() : "?";
                    for (String p : r.problems()) bad.add(kind + ": " + p);
                    for (Report.Fault f : r.faults()) if (f.code().equals("describe_failed")) bad.add(kind + ": " + f.text());
                    r.toJson();
                }
            } catch (RuntimeException e) {
                bad.add(e.toString());
            }
            total++;
            if (bad.isEmpty()) ok++;
            else checker.check("describe at " + n.coordonate, false, String.join("; ", bad));
        }
        for (var e : level.getEntities((net.minecraft.world.entity.Entity) null, bounds, x -> x instanceof Describable)) {
            List<String> bad = new ArrayList<>();
            try {
                Report r = Describe.entity(e);
                bad.addAll(r.problems());
                for (Report.Fault f : r.faults()) if (f.code().equals("describe_failed")) bad.add(f.text());
                r.toJson();
            } catch (RuntimeException ex) {
                bad.add(ex.toString());
            }
            total++;
            if (bad.isEmpty()) ok++;
            else checker.check("describe " + e, false, String.join("; ", bad));
        }
        if (total > 0) checker.check("describe: every node and entity gives a valid report", ok == total, ok + "/" + total);
    }
}
