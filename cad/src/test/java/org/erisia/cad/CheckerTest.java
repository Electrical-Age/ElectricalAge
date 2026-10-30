package org.erisia.cad;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckerTest {
    static List<String> rules(Model m) {
        return Checker.check(m).stream().map(Checker.Issue::rule).toList();
    }

    @Test
    void sameFacingCoplanarFacesOfDifferentLookZFight() {
        Model m = new Model();
        m.add(new Box(0, 0, 0, 4, 2, 4)).material("brass");
        m.add(new Box(1, 1, 1, 3, 2, 3)).material("red");   // tops in one plane, overlapping
        assertEquals(List.of("z-fight"), rules(m));
    }

    @Test
    void opposingFacesInOnePlaneDoNot() {
        Model m = new Model();
        m.add(new Box(0, 0, 0, 4, 2, 4)).material("brass");
        m.add(new Box(1, 2, 1, 3, 3, 3)).material("red");   // stacked: the body's top meets the cap's bottom
        assertEquals(List.of(), rules(m));
    }

    @Test
    void facesDrawnAlikeAreExempt() {
        UvMap planar = f -> {
            float[] uv = new float[2 * f.n];
            for (int i = 0; i < f.n; i++) {
                uv[2 * i] = (float) f.xyz[3 * i] / 16;
                uv[2 * i + 1] = (float) f.xyz[3 * i + 2] / 16;
            }
            return uv;
        };
        Model m = new Model();
        m.add(new Box(0, 0, 0, 4, 2, 4)).material("iron").uv(planar);
        m.add(new Box(1, 1, 1, 3, 2, 3)).material("iron").uv(planar);
        assertEquals(List.of(), rules(m));
        m.solids.get(1).argb(0xFF808080);   // another colour: visible
        assertEquals(List.of("z-fight"), rules(m));
    }

    @Test
    void aLayeredDecalSitsClear() {
        Model m = new Model();
        m.add(new Box(0, 0, 0, 4, 2, 4)).material("case");
        m.add(new Box(1, 1, 1, 3, 2, 3)).material("stripe").layer(1);
        assertEquals(List.of(), rules(m));
    }

    @Test
    void thinSolidsAreDegenerate() {
        Model m = new Model();
        m.add(new Box(0, 0, 0, 4, 0.03, 4));
        m.add(Frustum.cylinder(1, 0, 0, 0.01, 0, 4)).named("wire");
        assertEquals(2, rules(m).stream().filter("degenerate"::equals).count());
    }

    @Test
    void relations() {
        Model m = new Model();
        Solid pipe = m.add(Frustum.cylinder(0, 8, 8, 0.5, 0, 16)).named("pipe");
        Solid onTop = m.add(new Box(7, 8.5, 7, 9, 10, 9)).named("on top");
        Solid floating = m.add(new Box(10, 9.5, 7, 12, 10, 9)).named("floating");
        Solid sunk = m.add(new Box(7.5, 8, 7.5, 8.5, 10, 8.5)).named("sunk");
        Solid hit = m.add(new Box(6, 8, 6, 10, 12, 10)).ghost();
        m.attach(onTop, pipe, 0, 0, 0);   // (touching only: a box on a round pipe is held along a line)
        assertEquals(List.of(), Checker.relations(m));
        m.abut(floating, pipe);
        m.attach(sunk, pipe, 0.6);
        m.attach(sunk, pipe, 0.2);
        m.disjoint(onTop, floating);
        m.disjoint(onTop, sunk);
        m.within(onTop, hit);
        m.within(pipe, hit);
        m.ratio("cap over pipe radius", 3, 0.5, 2);
        List<String> got = Checker.relations(m).stream().map(Checker.Issue::rule).toList();
        assertEquals(List.of("attach", "attach", "disjoint", "within", "proportion"), got, Checker.relations(m).toString());
    }

    @Test
    void frustumFacesPointOutward() {
        Frustum f = Frustum.cylinder(2, 1, 2, 3, -1, 5);
        for (Face face : f.faces()) {
            double cx = 0, cy = 0, cz = 0;
            for (int i = 0; i < face.n; i++) {
                cx += face.xyz[3 * i] / face.n;
                cy += face.xyz[3 * i + 1] / face.n;
                cz += face.xyz[3 * i + 2] / face.n;
            }
            double ox = cx - 1, oy = cy - 2, oz = cz - 2;
            assertTrue(ox * face.nx + oy * face.ny + oz * face.nz > 0, "outward: " + face);
        }
        assertEquals(36, f.faces().size());
    }

    @Test
    void sweepPiecesRunIntoBends() {
        List<Frustum> p = Sweep.along(new double[]{0, 8, 8, 8, 8, 8, 8, 16, 8}, 1, 0.7);
        assertEquals(2, p.size());
        assertEquals(8.7, p.get(0).a1, 1e-9);
        assertEquals(7.3, p.get(1).a0, 1e-9);
        // slanted: placed on its line
        List<Frustum> s = Sweep.along(new double[]{0, 0, 0, 3, 4, 0}, 0.5, 0.7);
        Model one = new Model();
        one.add(s.get(0));
        double[] b = one.bounds(x -> true);
        assertEquals(3.5, b[3], 0.2);
        assertEquals(4.3, b[4], 0.2);
    }

    static List<String> attach(Solid a, Solid b, double maxSink) {
        Model m = new Model();
        m.add(a);
        m.add(b);
        m.attach(a, b, maxSink);
        return Checker.relations(m).stream().map(Checker.Issue::toString).toList();
    }

    @Test
    void attachWantsACrossSectionNotATouch() {
        Box base = new Box(0, 0, 0, 10, 2, 10);
        // a box on a box's face: held all over
        assertEquals(List.of(), attach(new Box(2, 2, 2, 6, 5, 6).named("on"), base, 0));
        // a box hanging half over the edge: 50 %, just enough
        assertEquals(List.of(), attach(new Box(8, 2, 2, 12, 5, 6).named("half"), base, 0));
        // three quarters over the edge: 25 %
        List<String> quarter = attach(new Box(9, 2, 2, 13, 5, 6).named("quarter"), base, 0);
        assertEquals(1, quarter.size());
        assertTrue(quarter.get(0).contains("barely attached") && quarter.get(0).contains("26 %"), quarter.toString());   // (b grown by EPS)
        // touching at a corner only
        List<String> corner = attach(new Box(10, 2, 10, 13, 5, 13).named("corner"), base, 0);
        assertTrue(corner.size() == 1 && corner.get(0).contains("barely attached"), corner.toString());
        // a cylinder lying on the box: a line of contact, a few percent
        Frustum log = Frustum.cylinder(0, 3, 5, 1, 1, 9);   // along x, centre y 3 (radius 1 down to the top at 2), z 5
        List<String> lying = attach(log, base, 0);
        assertTrue(lying.size() == 1 && lying.get(0).contains("barely attached"), lying.toString());
        // ... sunk to its axis it is held over its whole width
        assertEquals(List.of(), attach(Frustum.cylinder(0, 2, 5, 1, 1, 9), base, 1));
        // an absolute minimum area
        Model m = new Model();
        Solid small = m.add(new Box(2, 2, 2, 3, 3, 3));
        m.add(base);
        m.attach(small, base, 0, 0.5, 2);
        assertEquals(1, Checker.relations(m).size(), Checker.relations(m).toString());
    }
}
