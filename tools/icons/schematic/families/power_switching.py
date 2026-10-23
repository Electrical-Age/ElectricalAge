"""power_switching family: in-line switches, relays and the breaker (STYLE.md section 8.6).

Shared system: the IEC make contact (kit `switch`) is the family's backbone. Its WHOLE contact
(leads, both contact dots, blade) carries the tier hue; tier siblings differ only by colour
(contract decision 3). Variants differ by structure, never by thickness:

    power_switch   {lv,mv,hv,vhv}_voltage_switch          plain make contact, standard layout
    power_relay    {signal,lv,mv,hv,vhv}_relay            contact + INK coil + dashed link (kit relay)
    led_switch     signal_switch_with_led                 mark layout: 'led' emission mark + contact
    cable_switch   signal_cable_switch                    contact cut into signal_cable's routed step
    power_breaker  electrical_breaker (untiered, INK)     kit breaker(): contact + breaker cross

cable_switch is STYLE 8.6's accepted glyph for signal_cable_switch (proposed by this family): the
signal_cable glyph (kit cable_route: left end up to the top edge, run, right end down to the bottom edge)
with the make contact cut into the run, so it reads as "the signal cable, cut by a switch" and differs in
structure from signal_switch's plain contact. The earlier proposal (both cable ends turned down) is kept
as cable_switch_legs_down() for reference only.

power_breaker was a family shim; its geometry is now the kit's breaker() (kit 2026-10-12).
"""
from kit import LO, HI, MID_M, switch, relay, mark, breaker

__all__ = ['power_switch', 'power_relay', 'led_switch', 'cable_switch', 'cable_switch_legs_down',
           'power_breaker']


def power_switch(g):
    """Tiered in-line switch: the kit make contact, whole contact in the tier hue."""
    switch(g, g.hue)


def power_relay(g):
    """Tiered relay: kit relay, contact in the tier hue, coil and dashed link INK."""
    relay(g, g.hue)


def led_switch(g):
    """Switch with an indicator LED: 'led' (emission arrow) mark top right + mark-layout contact."""
    mark(g, 'led')
    switch(g, g.hue, mark=True)


# cable_switch geometry: the contact sits on MID_M (run rows 19-20) so the blade rises clear of both
# cable ends; pivot dot cols 10..15, fixed contact dot cols 20..25 (gap 4), blade tip above it.
CS_Y = MID_M
CS_PIVOT = 12
CS_TIP = (19, 5)
CS_X1 = 20


def cable_switch(g):
    """In-line switch on a signal cable (STYLE 8.6, accepted): kit cable_route's step with the
    make contact cut into the run. Left end up to the top edge (cols 4-5, like signal_cable), right end
    down to the bottom edge (cols 26-27). All in the tier hue, like cable_route. The left end lies
    outside the stack-count zone, so the cable cue survives a stack count."""
    c = g.hue
    switch(g, c, y=CS_Y, pivot=CS_PIVOT, tip=CS_TIP, x1=CS_X1)
    g.vline(LO, LO, CS_Y + 1, c)          # left cable end up to the top edge
    g.vline(HI - 1, CS_Y, HI, c)          # right cable end down to the bottom edge


def cable_switch_legs_down(g):
    """Alternative for the owner (first proposal): the make contact on a cable run whose two ends both
    turn down to the bottom edge. Passes check(); reads a little like a lever on two posts."""
    c = g.hue
    switch(g, c, y=MID_M, pivot=9, tip=(19, 5), x1=19)
    g.vline(LO, MID_M, HI, c)
    g.vline(HI - 1, MID_M, HI, c)


def power_breaker(g):
    """Circuit breaker (IEC: make contact with the breaker cross on the fixed contact). Untiered -> INK.
    = kit breaker(g, g.hue)."""
    breaker(g, g.hue)
