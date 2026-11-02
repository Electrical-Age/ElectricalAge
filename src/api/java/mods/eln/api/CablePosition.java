package mods.eln.api;

/**
 * Where on a face a signal's cable meets the block: anywhere on it (ANY, the default), or only a cable lying on the
 * floor or the ceiling of the block beside that face. Two signals may share a face at different positions, e.g. an
 * input on the floor and an output on the ceiling of a flank whose other faces are taken. FLOOR and CEILING are for
 * the side faces (FRONT, BACK, LEFT, RIGHT); on a block facing up or down they are the down and up of EA's frame for
 * that face.
 */
public enum CablePosition {
    ANY, FLOOR, CEILING
}
