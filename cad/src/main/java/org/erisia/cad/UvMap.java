package org.erisia.cad;

/** Texture coordinates for a face as it is made: u v per corner, in the target's convention. */
@FunctionalInterface
public interface UvMap {
    float[] map(Face f);
}
