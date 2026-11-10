package mods.eln.fluidcore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A network written out as text and read back, to replay a step that failed in the game in a test. Doubles go out
 * as their exact bits (hex), so the replay starts from the same state.
 * <pre>
 * dt BITS
 * species wa                                   (water and air, the fast path) or, per species:
 * species KEY GROUP KIND rho cpL cpV M tb lb
 * node V z H fixed U heat m0 m1 ...
 * edge a b fa fb g kind boost exchange conductance [gBack [qMax h0 speed eta]]
 * </pre>
 */
public final class NetworkDump {
    private NetworkDump() {
    }

    public static String write(Network net, double dt) {
        StringBuilder s = new StringBuilder();
        s.append("dt ").append(h(dt)).append('\n');
        Species[] sp = net.nodes.isEmpty() ? null : net.nodes.get(0).species;
        if (sp == null) s.append("species wa\n");
        else for (Species x : sp)
            s.append(String.format(Locale.ROOT, "species %s %s %s %s %s %s %s %s %s%n", x.key, x.group, x.kind, h(x.rho), h(x.cpL), h(x.cpV),
                h(x.molarMass), h(x.tb), h(x.lb)));
        java.util.Map<Network.Node, Integer> idx = new java.util.IdentityHashMap<>();
        for (Network.Node n : net.nodes) {
            idx.put(n, idx.size());
            s.append("node ").append(h(n.V)).append(' ').append(h(n.z)).append(' ').append(h(n.H)).append(' ').append(n.fixed)
                .append(' ').append(h(n.U)).append(' ').append(h(n.heat));
            for (double m : n.m) s.append(' ').append(h(m));
            s.append('\n');
        }
        for (Network.Edge e : net.edges)
            s.append("edge ").append(idx.get(e.a)).append(' ').append(idx.get(e.b)).append(' ').append(h(e.fa)).append(' ').append(h(e.fb))
                .append(' ').append(h(e.g)).append(' ').append(e.kind).append(' ').append(h(e.boost)).append(' ').append(h(e.exchange))
                .append(' ').append(h(e.conductance)).append(' ').append(h(e.gBack)).append(' ').append(h(e.qMax)).append(' ').append(h(e.h0))
                .append(' ').append(h(e.speed)).append(' ').append(h(e.eta)).append(' ').append(h(e.band)).append('\n');
        return s.toString();
    }

    /** The network a dump holds; its step length goes into dt[0]. */
    public static Network read(String text, double[] dt) {
        Network net = new Network();
        List<Species> sp = new ArrayList<>();
        boolean wa = false;
        List<Network.Node> nodes = new ArrayList<>();
        for (String line : text.split("\n")) {
            String[] w = line.trim().split(" ");
            switch (w[0]) {
                case "dt" -> dt[0] = d(w[1]);
                case "species" -> {
                    if (w[1].equals("wa")) wa = true;
                    else if (w[1].equals(Species.WATER.key)) sp.add(Species.WATER);
                    else if (w[1].equals(Species.AIR.key)) sp.add(Species.AIR);
                    else {
                        Species.Kind k = Species.Kind.valueOf(w[3]);
                        sp.add(switch (k) {
                            case CURVE -> Species.curve(w[1], w[2], d(w[4]), d(w[5]), d(w[6]), d(w[7]), d(w[8]), d(w[9]));
                            case NONE -> Species.none(w[1], w[2], d(w[4]), d(w[5]), d(w[7]));
                            case GAS -> Species.gas(w[1], d(w[6]), d(w[7]));
                            case WATER -> Species.WATER;
                        });
                    }
                }
                case "node" -> {
                    Network.Node n = new Network.Node(d(w[1]), d(w[2]), d(w[3]), Boolean.parseBoolean(w[4]), wa ? null : sp.toArray(new Species[0]));
                    n.U = d(w[5]);
                    n.heat = d(w[6]);
                    for (int j = 0; j < n.m.length; j++) n.m[j] = d(w[7 + j]);
                    n.update();
                    nodes.add(net.add(n));
                }
                case "edge" -> {
                    Network.Edge e = net.connect(nodes.get(Integer.parseInt(w[1])), d(w[3]), nodes.get(Integer.parseInt(w[2])), d(w[4]), d(w[5]));
                    e.kind = Network.Kind.valueOf(w[6]);
                    e.boost = d(w[7]);
                    e.exchange = d(w[8]);
                    e.conductance = d(w[9]);
                    if (w.length > 10) e.gBack = d(w[10]);
                    if (w.length > 14) {
                        e.qMax = d(w[11]);
                        e.h0 = d(w[12]);
                        e.speed = d(w[13]);
                        e.eta = d(w[14]);
                    }
                    if (w.length > 15) e.band = d(w[15]);
                }
                default -> {
                }
            }
        }
        return net;
    }

    private static String h(double x) {
        return Double.toHexString(x);
    }

    private static double d(String s) {
        return Double.parseDouble(s);
    }
}
