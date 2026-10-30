package org.erisia.cad;

/** What the author means about solids, checked for every parameter set (the checker never moves anything to fix it). */
public sealed interface Relation {
    /**
     * a touches b's surface (abut, maxSink 0) or sits sunk into it by at most maxSink px (a fitting's root inside its
     * pipe): never floating clear of it, never buried deeper.
     */
    record Attach(Solid a, Solid b, double maxSink) implements Relation {
    }

    /** a and b don't clip each other (touching is allowed). */
    record Disjoint(Solid a, Solid b) implements Relation {
    }

    /** inner lies inside outer (convex: a hitbox, a clearance box). */
    record Within(Solid inner, Solid outer) implements Relation {
    }

    /** A named measure (a fitting's radius over its pipe's) stays within [min, max]. */
    record Ratio(String what, double value, double min, double max) implements Relation {
    }
}
