"""Reference sheets of the kit, each cell checked with Glyph.check():

    python3 -m kit.catalog ref/catalog.png                    # PRIMITIVES: every primitive
    python3 -m kit.catalog ref/conventions.png conventions    # CONVENTIONS: family tables of STYLE.md 8-9

Each cell: 6x zoom on the slot grey, then 1:1 (GUI scale 2) on slot grey and on the dark row, a
1.5x crop (GUI 3), and the label. A cell that breaks a rule is framed red and listed on stdout; a
warning is framed amber. A CONVENTIONS label is the item id it stands for; the function in this file
that draws it is the exact code to copy (or to call from families/<family>.py).
"""
import sys
from PIL import Image, ImageDraw, ImageFont

from . import *                               # noqa: F401,F403
from .symbols import *                        # noqa: F401,F403

PRIMITIVES = [
    ('cable_route lv', 'lv', lambda g: cable_route(g, g.hue)),
    ('thermal_cable', 'thermal', lambda g: thermal_cable(g, g.hue)),
    ('ground', None, lambda g: ground(g)),
    ('resistor', None, lambda g: resistor(g)),
    ('resistor small=True', None, lambda g: resistor(g, small=True)),
    ('resistor+adjust_arrow', None, lambda g: (resistor(g), adjust_arrow(g, 6, 25, 25, 6))),
    ('resistor+preset_mark', 'signal', lambda g: (resistor(g, g.hue), preset_mark(g, 9, 23, 21, 8))),
    ('resistor+nonlinear_slash', 'signal', lambda g: (resistor(g, g.hue), nonlinear_slash(g, 11, 23, 21, 7))),
    ('fuse hv', 'hv', lambda g: fuse(g, g.hue)),
    ('fuse blown=True', None, lambda g: fuse(g, blown=True)),
    ('capacitor polar=True', None, lambda g: capacitor(g, polar=True)),
    ('capacitor cells=2', None, lambda g: capacitor(g, cells=2)),
    ('battery', 'lv', lambda g: battery(g, g.hue)),
    ('battery cells=2', None, lambda g: battery(g, cells=2)),
    ('inductor', None, lambda g: inductor(g)),
    ('diode', None, lambda g: diode(g)),
    ('led', 'signal', lambda g: led(g, g.hue)),
    ('photodiode', 'signal', lambda g: photodiode(g, g.hue)),
    ('switch', 'vhv', lambda g: switch(g, g.hue)),
    ('relay', 'mv', lambda g: relay(g, g.hue)),
    ('button', 'signal', lambda g: button(g, g.hue)),
    ('breaker', None, lambda g: breaker(g)),
    ('lamp', 'mv', lambda g: lamp(g, g.hue)),
    ('lamp ring=RING_SMALL', 'mv', lambda g: lamp(g, g.hue, ring=RING_SMALL)),
    ("source 'V'", None, lambda g: source(g, 'V')),
    ("source 'I'", None, lambda g: source(g, 'I')),
    ("source 'AC'", 'signal', lambda g: source(g, 'AC', g.hue)),
    ("meter 'V'", 'signal', lambda g: meter(g, 'V', g.hue)),
    ("meter 'W'", 'lv', lambda g: meter(g, 'W', g.hue)),
    ("machine 'M'", None, lambda g: machine(g, 'M')),
    ("machine 'G'", 'hv', lambda g: machine(g, 'G', g.hue)),
    ('summing', 'signal', lambda g: summing(g, g.hue)),
    ('trace', 'signal', lambda g: trace(g, g.hue)),
    ('transformer', None, lambda g: transformer(g)),
    ('transformer y0=12', None, lambda g: transformer(g, y0=12)),
    ('transmitter', 'mv', lambda g: transmitter(g, g.hue)),
    ('receiver', 'mv', lambda g: receiver(g, g.hue)),
    ('repeater', 'signal', lambda g: repeater(g, g.hue)),
    ('bell', None, lambda g: bell(g)),
    ("gate 'and'", 'signal', lambda g: gate(g, 'and', g.hue)),
    ("gate 'and' inputs=2", 'signal', lambda g: gate(g, 'and', g.hue, inputs=2)),
    ("gate 'and' invert", 'signal', lambda g: gate(g, 'and', g.hue, invert=True)),
    ("gate 'or'", 'signal', lambda g: gate(g, 'or', g.hue)),
    ("gate 'or' invert", 'signal', lambda g: gate(g, 'or', g.hue, invert=True)),
    ("gate 'xor'", 'signal', lambda g: gate(g, 'xor', g.hue)),
    ("gate 'xor' invert", 'signal', lambda g: gate(g, 'xor', g.hue, invert=True)),
    ("gate 'buf' invert", 'signal', lambda g: gate(g, 'buf', g.hue, invert=True)),
    ('opamp', 'signal', lambda g: opamp(g, g.hue)),
    ('opamp inputs=1', 'signal', lambda g: opamp(g, g.hue, inputs=1)),
    ('opamp inputs=1 control', 'signal', lambda g: opamp(g, g.hue, inputs=1, control=True)),
    ('matrix', 'signal', lambda g: matrix(g, g.hue)),
    ('sine', 'signal', lambda g: sine(g, 4, 27, 16.0, 4.0, 1.5, g.hue)),
    ('sawtooth', 'signal', lambda g: sawtooth(g, 4, 27, 9, 21, 2, g.hue)),
    ('square', 'signal', lambda g: square(g, 4, 27, 9, 21, 1.5, g.hue)),
    ('edge', 'signal', lambda g: edge(g, 4, 27, 9, 21, g.hue)),
    ('hysteresis', 'signal', lambda g: hysteresis(g, 6, 9, 20, 14, g.hue)),
    ("response 'lowpass'", 'signal', lambda g: response(g, 'lowpass', g.hue)),
    ('feed_arrow + arrow', None, lambda g: (feed_arrow(g), arrow(g, 4, 22, 26, 22))),
    ('diag_arrow x4', None, lambda g: [diag_arrow(g, x, y, sx, sy) for x, y, sx, sy in ((5, 5, 1, -1), (17, 5, -1, -1), (5, 17, 1, 1), (17, 17, -1, 1))]),
    ('gear x2 + grains', 'lv', lambda g: (gear(g, 6, 8, g.hue, -1), gear(g, 18, 8, g.hue, 1), grains(g, ((11, 22), (16, 25), (21, 22))))),
    ('contact node + terminal', None, lambda g: (g.hline(4, 20, 15), g.vline(10, 4, 27), contact(g, 10, 15), terminal(g, 23, 15))),
    ('terminal d=8', None, lambda g: (terminal(g, 7, 15, d=8), g.hline(12, 19, 15), terminal(g, 23, 15, d=8))),
    ('heat_waves HEAT_TOP', None, lambda g: (heat_waves(g, *HEAT_TOP), resistor(g, y=MID_M))),
    ("mark 'C' + lamp RING_M", 'lv', lambda g: (mark(g, 'C'), lamp(g, g.hue, ring=RING_M))),
    ("mark 'Cu' + resistor MID_M", 'mv', lambda g: (mark(g, 'Cu'), resistor(g, g.hue, y=MID_M))),
    ("mark 'temperature' (t°)", None, lambda g: (mark(g, 'temperature'), resistor(g, y=MID_M))),
    ("heat_waves n=2 + mark", 'thermal', lambda g: (heat_waves(g, *HEAT_TOP, n=2), mark(g, 'fire'), resistor(g, g.hue, y=MID_M))),
]
for _name in PICTOGRAMS:
    PRIMITIVES.append((f"pictogram '{_name}'", None,
                       (lambda n: lambda g: (pictogram(g, n), machine(g, 'G', ring=RING_M)))(_name)))


from .conventions import *       # noqa: F401,F403


def _function_block(g):
    """signal_processor: function ring + INK 'f', three inputs left (rows 10 / 15 / 20), output right."""
    ring_letter(g, 'f', g.hue)
    for y in (10, 20):
        xe = next(x for x in range(LO, 16) if g.get(x, y) or g.get(x, y + 1))
        g.hline(LO, xe - 1, y)


def _fuse_holder(g):
    """electrical_fuse_holder: shortened INK fuse between two open terminal rings at the lead ends."""
    terminal(g, 6, MID)
    terminal(g, 24, MID)
    fuse(g, x0=11, x1=20, with_leads=False)
    g.hline(10, 10, MID)
    g.hline(21, 21, MID)


CONVENTIONS = [
    # bulbs (tier hue on the lamp)
    ('50v_incandescent_light_bulb', 'lv', lambda g: bulb(g)),
    ('50v_carbon_incandescent (C)', 'lv', lambda g: bulb(g, 'carbon')),
    ('50v_economic_light_bulb (E)', 'lv', lambda g: bulb(g, 'economic')),
    ('50v_led_bulb', 'lv', lambda g: led(g, g.hue)),
    ('50v_farming_lamp (F)', 'lv', lambda g: bulb(g, 'farming')),
    ('50v_emergency_lamp (!)', 'lv', lambda g: bulb(g, 'emergency')),
    ('small_50v_incandescent', 'lv', lambda g: bulb(g, small=True)),
    ('small_50v_carbon (C, small)', 'lv', lambda g: bulb(g, 'carbon', small=True)),
    ('200v_economic_light_bulb (E)', 'mv', lambda g: bulb(g, 'economic')),
    # lamp sockets (untiered)
    ('simple_lamp_socket', None, lambda g: socket(g, 'simple')),
    ('suspended_lamp_socket', None, lambda g: socket(g, 'suspended')),
    ('long_suspended_lamp_socket', None, lambda g: socket(g, 'long_suspended')),
    ('flat_lamp_socket', None, lambda g: socket(g, 'flat')),
    ('sconce_lamp_socket', None, lambda g: socket(g, 'sconce')),
    ('fluorescent_lamp_socket (E)', None, lambda g: socket(g, 'fluorescent')),
    ('lamp_socket_a (A)', None, lambda g: socket(g, 'socket_a')),
    ('robust_lamp_socket (R)', None, lambda g: socket(g, 'robust')),
    # heating corps
    ('50v_copper_heating_corp (Cu)', 'lv', lambda g: heating_corp(g, 'copper')),
    ('200v_iron_heating_corp (Fe)', 'mv', lambda g: heating_corp(g, 'iron')),
    ('50v_tungsten_heating_corp (W)', 'lv', lambda g: heating_corp(g, 'tungsten')),
    ('small_200v_copper (Cu, small)', 'mv', lambda g: heating_corp(g, 'copper', small=True)),
    # batteries, capacitors, chargers
    ('portable_battery', None, lambda g: battery_block(g)),
    ('portable_battery_pack', None, lambda g: battery_block(g, cells=2)),
    ('portable_capacitor', None, lambda g: capacitor(g)),
    ('portable_capacitor_pack', None, lambda g: capacitor(g, cells=2)),
    ('cost_oriented_battery ($)', 'lv', lambda g: battery_block(g, 'cost')),
    ('capacity_oriented_battery (C)', 'lv', lambda g: battery_block(g, 'capacity')),
    ('current_oriented_battery (A)', 'lv', lambda g: battery_block(g, 'current')),
    ('life_oriented_battery (L)', 'lv', lambda g: battery_block(g, 'life')),
    ('voltage_oriented_battery (V)', 'mv', lambda g: battery_block(g, 'voltage')),
    ('single_use_battery (1)', None, lambda g: battery_block(g, 'single_use')),
    ('50v_battery_charger (bolt)', 'lv', lambda g: battery_block(g, 'charge')),
    ('weak_50v_battery_charger', 'lv', lambda g: battery_block(g, 'charge', small=True)),
    # generators and motors
    ('50v_fuel_generator (drop)', 'lv', lambda g: generator(g, 'fuel')),
    ('gas_turbine (flame)', 'mv', lambda g: generator(g, 'gas')),
    ('steam_turbine (steam)', 'mv', lambda g: generator(g, 'steam')),
    ('water_turbine (water)', 'lv', lambda g: generator(g, 'water')),
    ('wind_turbine (wind)', 'lv', lambda g: generator(g, 'wind')),
    ('electrical_motor', 'lv', lambda g: machine(g, 'M', g.hue)),
    # sensors
    ('electrical_light_sensor', 'signal', lambda g: photodiode(g, g.hue)),
    ('electrical_daylight_sensor (sun)', 'signal', lambda g: sensor_diode(g, 'daylight')),
    ('electrical_fire_detector (flame)', 'signal', lambda g: sensor_diode(g, 'fire')),
    ('electrical_entity_sensor (person)', 'signal', lambda g: sensor_diode(g, 'entity')),
    ('electrical_weather_sensor (cloud)', 'signal', lambda g: (mark(g, 'weather'), resistor(g, g.hue, y=MID_M))),
    ('electrical_anemometer (wind)', 'signal', lambda g: (mark(g, 'wind'), meter(g, 'n', g.hue, ring=RING_M))),
    ('thermometer', 'signal', lambda g: meter(g, 'T', g.hue)),
    ('electrical_timer (clock)', 'signal', lambda g: (mark(g, 'timer'), switch(g, g.hue, mark=True))),
    # switches
    ('low_voltage_switch', 'lv', lambda g: switch(g, g.hue)),
    ('medium_voltage_relay', 'mv', lambda g: relay(g, g.hue)),
    ('signal_button', 'signal', lambda g: button(g, g.hue)),
    ('electrical_breaker', None, lambda g: breaker(g, g.hue)),
    # wireless
    ('low_power_transmitter_antenna', 'lv', lambda g: transmitter(g, g.hue)),
    ('low_power_receiver_antenna', 'lv', lambda g: receiver(g, g.hue)),
    ('wireless_signal_repeater', 'signal', lambda g: repeater(g, g.hue)),
    ('wireless_switch', 'signal', lambda g: (mark(g, 'wireless'), switch(g, g.hue, mark=True))),
    # alarms
    ('standard_alarm', 'signal', lambda g: bell(g, g.hue)),
    ('nuclear_alarm (!)', 'signal', lambda g: (mark(g, 'nuclear'), bell(g, g.hue, y=MID_M + 4))),
    # analog and sequential
    ('opamp', 'signal', lambda g: opamp(g, g.hue)),
    ('amplifier', 'signal', lambda g: opamp(g, g.hue, inputs=1)),
    ('voltage_controlled_amplifier', 'signal', lambda g: opamp(g, g.hue, inputs=1, control=True)),
    ('signal_source', 'signal', lambda g: source(g, 'V', g.hue)),
    ('oscillator_chip', 'signal', lambda g: oscillator(g, 'square', True)),
    ('vc_sine_oscillator', 'signal', lambda g: oscillator(g, 'sine', True)),
    ('vc_sawtooth_oscillator', 'signal', lambda g: oscillator(g, 'sawtooth', True)),
    ('lowpass_filter', 'signal', lambda g: response(g, 'lowpass', g.hue)),
    ('and_chip', 'signal', lambda g: gate(g, 'and', g.hue)),
    ('xnor_chip', 'signal', lambda g: gate(g, 'xor', g.hue, invert=True)),
    ('not_chip', 'signal', lambda g: gate(g, 'buf', g.hue, invert=True)),
    ('schmitt_trigger_chip', 'signal', lambda g: hysteresis(g, 4, 9, 24, 14, g.hue)),
    ('on_off_regulator_1 (1)', 'signal', lambda g: (mark(g, 'one'), hysteresis(g, 4, 15, 18, 12, g.hue))),
    ('on_off_regulator_10 (10)', 'signal', lambda g: (mark(g, 'ten'), hysteresis(g, 4, 15, 18, 12, g.hue))),
    ('configurable_summing_unit', 'signal', lambda g: summing(g, g.hue)),
    ('sample_and_hold', 'signal', sample_hold),
    ('d_flip_flop_chip (D)', 'signal', lambda g: (mark(g, 'd'), edge(g, 4, 27, 15, 25, g.hue))),
    ('jk_flip_flop_chip (JK)', 'signal', lambda g: (mark(g, 'jk'), edge(g, 4, 27, 15, 25, g.hue))),
    ('pal_chip', 'signal', lambda g: matrix(g, g.hue)),
    ('pid_regulator', 'signal', lambda g: regulator(g, 'PID')),
    ('analogic_regulator', 'signal', lambda g: regulator(g, 'P')),
    ('data_logger', 'signal', lambda g: trace(g, g.hue)),
    ('modern_data_logger (M)', 'signal', lambda g: (mark(g, 'M'), trace(g, g.hue, ring=RING_M))),
    # rows of STYLE 8.4-8.6 / 9 not covered above
    ('10a_diode (10)', None, lambda g: (mark(g, '10'), diode(g, y=MID_M, h=10))),
    ('signal_diode', 'signal', lambda g: diode(g, g.hue)),
    ('advanced_electrical_motor (+)', 'mv', lambda g: (mark(g, 'advanced'), machine(g, 'M', g.hue, ring=RING_M))),
    ('energy_meter', 'lv', lambda g: meter(g, 'E', g.hue)),
    ('advanced_energy_meter (+)', 'lv', lambda g: (mark(g, 'advanced'), meter(g, 'E', g.hue, ring=RING_M))),
    ('electrical_fire_buzzer (flame)', 'signal', lambda g: (mark(g, 'fire'), bell(g, g.hue, y=MID_M + 4))),
    ('signal_switch_with_led (light)', 'signal', lambda g: (mark(g, 'led'), switch(g, g.hue, mark=True))),
    ('2x3_solar_panel (2)', 'lv', lambda g: (mark(g, '2'), diode(g, g.hue, x0=6, w=10, h=10, y=MID_M))),
    ('small_rotating_solar_panel (sun)', 'lv', lambda g: sensor_diode(g, 'daylight')),
    ('signal_trimmer', 'signal', lambda g: (resistor(g, g.hue), preset_mark(g, 9, 23, 21, 8))),
    ('signal_processor', 'signal', _function_block),
    ('signal_20h_inductor', 'signal', lambda g: inductor(g, g.hue)),
    ('rheostat', None, lambda g: (resistor(g, small=True), adjust_arrow(g, 6, 25, 25, 6))),
    ('large_rheostat', None, lambda g: (resistor(g), adjust_arrow(g, 6, 25, 25, 6))),
    ('power_capacitor_block (B)', None, lambda g: (mark(g, 'B'), capacitor(g, x=8, h=14, y=MID_M))),
    ('power_inductor_block (B)', None, lambda g: (mark(g, 'B'), inductor(g, y=MID_M))),
    ('25a_diode (25)', None, lambda g: (mark(g, '25'), diode(g, y=MID_M, h=10))),
    ('thermistor (t°)', None, lambda g: (mark(g, 'temperature'), resistor(g, y=MID_M), nonlinear_slash(g, 9, 26, 21, 14))),
    ('electrical_fuse_holder', None, _fuse_holder),
    ('hub', None, lambda g: (g.hline(LO, HI, MID), g.vline(MID, LO, HI), contact(g, MID, MID))),
    ('lead_fuse_for_hv_cables', 'hv', lambda g: fuse(g, g.hue)),
    ('blown_lead_fuse', None, lambda g: fuse(g, blown=True)),
    ('electrical_furnace', None, lambda g: (heat_waves(g, *HEAT_TOP), resistor(g, g.hue, y=MID_M))),
    ('copper_thermal_cable', 'thermal', lambda g: thermal_cable(g, g.hue)),
    ('stone_heat_furnace (flame)', 'thermal', lambda g: (heat_waves(g, *HEAT_TOP, n=2), mark(g, 'fire'), resistor(g, g.hue, y=MID_M))),
    ('fuel_heat_furnace (drop)', 'thermal', lambda g: (heat_waves(g, *HEAT_TOP, n=2), mark(g, 'fuel'), resistor(g, g.hue, y=MID_M))),
]


def label_font(size=11):
    import subprocess
    try:
        path = subprocess.run(['fc-match', '-f', '%{file}', 'sans'], capture_output=True, text=True).stdout.strip()
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()


def render_sheet(demos, out, per=8, Z=6):
    """Render [(label, tier, fn)] to `out`; returns [(label, error_or_None, warnings)]."""
    cell = 32 * Z
    rows = (len(demos) + per - 1) // per
    rh = cell + 40 + 34
    im = Image.new('RGBA', (per * (cell + 10) + 10, rows * rh + 10), '#555')
    dr = ImageDraw.Draw(im)
    fnt = label_font(12)
    results = []
    for i, (name, tier, fn) in enumerate(demos):
        g = Glyph(tier=tier)
        err, warns = None, []
        try:
            fn(g)
            warns = g.check(name)['warnings']
        except RuleBreak as e:
            err, warns = str(e), e.warnings
        except Exception as e:                              # a broken demo still renders what it drew
            err = f'{name}: {type(e).__name__}: {e}'
        results.append((name, err, warns))
        ic = g.render()
        x = 10 + (i % per) * (cell + 10)
        y = 10 + (i // per) * rh
        im.paste(Image.new('RGBA', (cell, cell), '#8b8b8b'), (x, y))
        im.alpha_composite(ic.resize((cell, cell), Image.NEAREST), (x, y))
        if err or warns:
            dr.rectangle([x - 3, y - 3, x + cell + 2, y + cell + 2], outline='#ff4040' if err else '#ffb030', width=3)
        sy = y + cell + 4
        dr.rectangle([x, sy, x + 33, sy + 33], fill='#8b8b8b')
        im.alpha_composite(ic, (x + 1, sy + 1))
        dr.rectangle([x + 38, sy, x + 71, sy + 33], fill='#1e1e24')
        im.alpha_composite(ic, (x + 39, sy + 1))
        dr.rectangle([x + 76, sy, x + 76 + 49, sy + 49 - 16], fill='#8b8b8b')
        im.alpha_composite(ic.resize((48, 48), Image.NEAREST).crop((0, 8, 48, 40)), (x + 77, sy + 1))
        dr.text((x + 2, sy + 36), name, fill='#eee', font=fnt)
    im.save(out)
    return results


def main(argv):
    out = argv[0] if argv else 'catalog.png'
    which = argv[1] if len(argv) > 1 else 'primitives'
    demos = CONVENTIONS if which == 'conventions' else PRIMITIVES
    res = render_sheet(demos, out)
    bad = 0
    for name, err, warns in res:
        if err:
            bad += 1
            print('RULE BREAK', err)
        for w in warns:
            print('WARN', name + ':', w)
    print(f'{len(demos)} cells, {bad} rule breaks -> {out}')
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
