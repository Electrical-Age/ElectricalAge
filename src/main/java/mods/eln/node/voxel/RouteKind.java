package mods.eln.node.voxel;

/**
 * What a route is laid with, as the router sees it (Baughn 2026-11-11: one data-driven router for cables and pipes; the
 * pipe/cable distinction is data). A SURFACE kind lies on a face plane against a solid support (cables); a FREE kind
 * runs through the middle of blocks, crossing face planes but never running in one, and pays extra where nothing is
 * within `rest` voxels of it (pipes). Sizes and costs in world voxels.
 *
 * @param id           stable name, e.g. "cable/Low Voltage Cable" or "pipe/iron"
 * @param placement    SURFACE or FREE
 * @param width        across the run (a FREE kind's bore)
 * @param height       off its face (a FREE kind: its bore)
 * @param lattice      step length
 * @param bend         extra cost per turn and per corner
 * @param blockEntered extra cost per block entered
 * @param clearance    air kept round a SURFACE run (across and above it)
 * @param margin       how far outside a leg's box the search may wander
 * @param unsupported  extra cost of a FREE step resting against nothing
 * @param rest         how near (voxels) something must be for a FREE step to rest against it
 * @param airTiers     a SURFACE kind's air allowance: {max contiguous voxels off any support, extra cost per lattice step}
 *                     pairs, ascending; a step beyond the last tier is refused. Empty: always on a support (Baughn
 *                     2026-11-11: cables only, pipes keep their soft support)
 */
public record RouteKind(String id, Placement placement, int width, int height, int lattice, int bend, int blockEntered, int clearance,
                        int margin, int unsupported, int rest, int[][] airTiers) {
    public enum Placement { SURFACE, FREE }

    /** No air allowance. */
    static final int[][] NO_AIR = {};

    /** The most contiguous air voxels allowed (0: none). */
    public int airLimit() {
        return airTiers.length == 0 ? 0 : airTiers[airTiers.length - 1][0];
    }

    /** The extra cost of a step that leaves `air` voxels off a support, or -1 if it is over the limit. */
    public int airCost(int air) {
        for (int[] t : airTiers) if (air <= t[0]) return t[1];
        return -1;
    }

    /** This kind with another air allowance. */
    public RouteKind withAir(int[][] tiers) {
        return new RouteKind(id, placement, width, height, lattice, bend, blockEntered, clearance, margin, unsupported, rest, tiers);
    }

    /**
     * A cable's air allowance (Baughn 2026-11-11, D5): up to 0.6 block off any support at a medium cost, up to 1.2 blocks
     * at a high one, never further (10 and 20 voxels on the 2-voxel lattice; +4 and +16 per step against a base step of
     * 2, so a short hop costs ~3x floor routing and a long one ~9x, and floors, walls and ceilings win where they reach).
     */
    static final int[][] CABLE_AIR = {{10, 4}, {20, 16}};

    /** A cable of this cross-section, with today's cable routing constants and the cable air allowance. */
    public static RouteKind cable(String id, int width, int height) {
        return new RouteKind(id, Placement.SURFACE, width, height, CableRouter.LATTICE, CableRouter.BEND, CableRouter.BLOCK_ENTERED,
            CableRouter.CLEARANCE, CableRouter.MARGIN, 0, 0, CABLE_AIR);
    }

    /** A cable tier with a spec (null: the preset), as the kit lays it. */
    public static RouteKind cable(int tier, CableSpec spec) {
        CablePart.Tier t = CablePlacer.tierOf(tier, spec);
        return cable("cable/" + t.name(), t.width(), t.height());
    }

    /** A pipe of this bore, with today's pipe routing constants. */
    public static RouteKind pipe(String id, int bore) {
        return new RouteKind(id, Placement.FREE, bore, bore, PipeRouter.LATTICE, PipeRouter.BEND, PipeRouter.BLOCK_ENTERED, 0,
            PipeRouter.MARGIN, PipeRouter.UNSUPPORTED, PipeRouter.REST, NO_AIR);
    }

    /** A pipe tier (PipePart.GRID, TIER_NAME). */
    public static RouteKind pipe(int tier) {
        return pipe("pipe/" + (tier == 0 ? "iron" : tier == 1 ? "alloy" : "large_alloy"), PipePart.GRID[tier]);
    }
}
