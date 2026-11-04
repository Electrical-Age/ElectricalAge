package mods.eln.api.lowlevel;

/**
 * Where on a block's face a port is: EA splits each face into four cable positions (its LRDU: left, right, up,
 * down as seen on that face; on a side face DOWN is the edge toward the floor, so a cable lying on the floor meets
 * its east neighbour at (EAST, DOWN)). ANY takes the first of LEFT, RIGHT, UP, DOWN with a port of the link's kind:
 * right for blocks with one port per face; name the position where a face has several.
 */
public enum PortPosition {
    ANY, LEFT, RIGHT, UP, DOWN
}
