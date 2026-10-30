package org.erisia.cad;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Solids and the relations among them. Emit it into a target; check it in tests. */
public final class Model {
    public final List<Solid> solids = new ArrayList<>();
    public final List<Relation> relations = new ArrayList<>();
    public final List<Part> parts = new ArrayList<>();

    public <S extends Solid> S add(S s) {
        solids.add(s);
        return s;
    }

    public Part part(String name, Part parent, double ox, double oy, double oz, double rx, double ry, double rz) {
        Part p = new Part(name, parent, ox, oy, oz, rx, ry, rz);
        parts.add(p);
        return p;
    }

    public Part part(String name) {
        for (Part p : parts) if (p.name.equals(name)) return p;
        return null;
    }

    public Solid solid(String name) {
        for (Solid s : solids) if (s.name.equals(name)) return s;
        return null;
    }

    public Model attach(Solid a, Solid b, double maxSink) {
        relations.add(new Relation.Attach(a, b, maxSink));
        return this;
    }

    public Model abut(Solid a, Solid b) {
        return attach(a, b, 0);
    }

    public Model disjoint(Solid a, Solid b) {
        relations.add(new Relation.Disjoint(a, b));
        return this;
    }

    public Model within(Solid inner, Solid outer) {
        relations.add(new Relation.Within(inner, outer));
        return this;
    }

    public Model ratio(String what, double value, double min, double max) {
        relations.add(new Relation.Ratio(what, value, min, max));
        return this;
    }

    /** Every drawn face (ghosts left out), in model space. */
    public void emit(Solid.FaceSink sink) {
        for (Solid s : solids) if (!s.ghost) s.emit(sink);
    }

    public List<Face> faces() {
        List<Face> out = new ArrayList<>();
        emit(out::add);
        return out;
    }

    /** The bounds {x0 y0 z0 x1 y1 z1} of the drawn solids that pass (model space), or null if none. */
    public double[] bounds(Predicate<Solid> which) {
        double[] r = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        boolean any = false;
        for (Solid s : solids) {
            if (s.ghost || !which.test(s)) continue;
            for (Face f : s.faces())
                for (int i = 0; i < f.n; i++)
                    for (int k = 0; k < 3; k++) {
                        r[k] = Math.min(r[k], f.xyz[3 * i + k]);
                        r[k + 3] = Math.max(r[k + 3], f.xyz[3 * i + k]);
                        any = true;
                    }
        }
        return any ? r : null;
    }
}
