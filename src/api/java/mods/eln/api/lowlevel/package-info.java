/**
 * The low level of Electrical Age's API: EA's circuit parts, for the few mods the Machine API (mods.eln.api: a spec
 * in, volts and joules out) cannot serve, such as a device whose electrics are not a consumer or a producer of
 * power. Start from Circuit.create.
 * <p>
 * You own the consequences. Nothing here has ratings, a buffer, brownout or overvoltage protection unless you build
 * it (Watchdogs); a resistor you set to 0.01 ohm on a 3.2 kV line draws 1 GW and EA will solve it. A load you put on
 * two faces passes power through your block, and the line is cut when your block entity unloads. A circuit saves
 * nothing: build it again from your block entity's own data each time it loads. What the API does keep safe: calls
 * off the server thread throw; values are checked (finite, ohms positive); every voltage source has an internal
 * resistance (an ideal one makes two in parallel unsolvable); parts of one circuit can't be wired into another; a
 * circuit's parts join and leave EA's simulation together, with the block entity it is hosted at; and a link to an
 * EA port is re-checked every tick and says why it isn't connected (PortStatus).
 * <p>
 * Names given to parts label them in messages and the debug trace (eln.api.debug); they are never saved.
 */
package mods.eln.api.lowlevel;
