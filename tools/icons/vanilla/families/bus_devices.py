"""Family helper: the RS485 bus devices (Baughn 2026-11-05), 16 px.

addressable_button, addressable_light and floor_sensor are six-node signal devices that reuse a
sibling's model unchanged (Wp9cContent.registerElectricalManagerSignal... ids 2, 3, 6):

    addressable_button  ledswitch.obj, white plate (AddressableButtonDescriptor)  = signal_button
    addressable_light   led.obj (AddressableLightDescriptor)                       = led_vumeter
    floor_sensor        proximitysensor.obj, Detector part (FloorSensorDescriptor) = electrical_entity_sensor

Their item draws are the sibling's item draws (button off: LED red; light at power 0: LED dark red;
sensor without its halo), so the body of each icon IS the sibling's icon.  What differs in the world
is the bus: BusDeviceRender.drawPin() draws a violet chamfered pin (SixNodeElementRender.BUS_RGB
0.62/0.38/0.80, 3.2/16 wide, 1/16 high) from the body to the block edge on the device's front side,
the bus cable's own colour; the devices take no signal cable at all (getCableRender null).

So the shared "on the bus" cue is that violet pin, drawn the way bus_io_module (the other bus
device) draws its bus pin: a violet tab leaving the top edge of the body, centred,
cols 5..10, rows 0..2 (bus_io_module's width), lit violet face with dark ends.  No tier colour is added: none of the
three models has a tier-tinted part (the siblings' tier_cue is none); signal shows in the
schematic set's hue.
"""
from kit.color import h

#: the bus cable / pin violet (bus_io_module's ramp): dark, mid, lit
VIOLET = (h('#4a2a63'), h('#6b3d8c'), h('#8e62b0'))

PIN_X0, PIN_X1 = 5, 10      # pin columns (6 px, as bus_io_module's: a 4 px pin was too small at GUI 2)


def bus_pin(cv, bottom=2):
    """The violet bus pin leaving the top of the body: rows 0..bottom, cols PIN_X0..PIN_X1.
    Lit top face, mid lower row, dark chamfered ends (the pin is chamfered) and dark right end."""
    for y in range(0, bottom + 1):
        cv.hline(PIN_X0, PIN_X1, y, VIOLET[1])
    for y in range(0, bottom):
        cv.hline(PIN_X0 + 1, PIN_X1 - 1, y, VIOLET[2])
    cv.vline(PIN_X0, 0, bottom, VIOLET[0])
    cv.vline(PIN_X1, 0, bottom, VIOLET[0])
    return cv


def addressable_button():
    """signal_button's ledswitch (white plate, red LED, red halo rays) + the bus pin."""
    from kit import MARK_RED
    from families.signal_io import led_switch
    return bus_pin(led_switch('white', MARK_RED, glow=MARK_RED))


def addressable_light():
    """led_vumeter's LED plate + the bus pin.  The item draw (power 0) tints the LED a dark
    red (0.25, 0.05, 0.05); the icon keeps led_vumeter's red, as lamps are drawn lit (STYLE 7b)."""
    from families.meters import led_vumeter
    return bus_pin(led_vumeter())


def floor_sensor():
    """electrical_entity_sensor's proximity sensor, end-on + the bus pin over its top rim."""
    from families.sensors import entity_sensor
    return bus_pin(entity_sensor())
