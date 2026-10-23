"""Central per-item decisions of the vanilla set (sizes, tier cues, variant axes).

Contract decision 4: an icon is 32x32 only when decided centrally, never by the artist.  The
production source of these facts is ``manifest/manifest.json``; until it carries them, this
module is the fallback build.py uses, and it is the PROPOSAL the manifest should copy.
Where the manifest has a value it wins (build.py warns when the two disagree).

    vanilla_size(item_id)  -> 16 | 32
    tier_of(item_id)       -> 'signal'|'lv'|'mv'|'hv'|'vhv'|'thermal'|None   (from the id)
    tier_cue(item_id)      -> 'body'|'plate'|'stub'|'band'|'jacket'|None
    quality(item_id)       -> 0|1|2 or None;  QUALITY_TRIM[level] -> ramp or None
    is_small(item_id)      -> bool;  SMALL_BOX / MEDIUM_BOX / BODY_BOX
    material_of(item_id)   -> palette name of the element material (heating corps, cables...)

See STYLE.md section 7 (tiers) and 7b (other variant axes).
"""
import re

from .chip import CHIP_FACES
from . import palette as P

#: Items drawn at 32x32 (decided per family): every logic and analog chip.
VANILLA_32 = frozenset(CHIP_FACES)


def vanilla_size(item_id):
    """16, or 32 for the families in VANILLA_32."""
    return 32 if item_id in VANILLA_32 else 16


# ------------------------------------------------------------------------------ tiers
_TIER_PATTERNS = [
    (r'^(weak_)?50v_|^small_50v_|_50v_|^small_active_thermal_dissipator$', 'lv'),
    (r'^200v_|^small_200v_', 'mv'),
    (r'^800v_', 'hv'),
    (r'^signal_(cable|switch|switch_with_led|relay|cable_switch)$', 'signal'),
    (r'^low_voltage_|_for_low_voltage_|^low_power_|lvublock$', 'lv'),
    (r'^medium_voltage_|_for_medium_voltage_|^medium_power_|mvublock$', 'mv'),
    (r'^high_voltage_|_for_high_voltage_|^high_power_|hvublock$', 'hv'),
    (r'^very_high_voltage_|_for_very_high_voltage_', 'vhv'),
    (r'thermal_cable$', 'thermal'),
]


def tier_of(item_id):
    """The voltage tier an item id implies, or None.  Only for items whose tier siblings must
    be told apart; the manifest's ``tier`` column wins when present."""
    for pat, t in _TIER_PATTERNS:
        if re.search(pat, item_id):
            return t
    return None


#: Primary tier cue per tiered family (``kit.tier`` docstring).  Body = the in-world texture is
#: tier-coloured over most of the body (green 50 V / blue 200 V: measured on the model textures).
_CUE_RULES = [
    (r'_(macerator|compressor|magnetizer|plate_machine|fuel_generator)$', 'body'),
    (r'_cable$', 'jacket'),
    (r'_(switch|switch_with_led|cable_switch|relay)$', 'plate'),
    (r'_antenna$', 'plate'),
    (r'_(battery_charger|power_socket|emergency_lamp|defence_turret)$', 'plate'),
    (r'^lead_fuse_for_', 'band'),
    (r'_(turbine|active_thermal_dissipator)$', 'band'),      # small coloured accents in-world
    (r'ublock$', 'band'),
    (r'_heating_corp$', 'stub'),
    (r'(_light_bulb|_led_bulb|_farming_lamp)$', 'band'),     # the bulb's base collar
    (r'_egg_incubator$', 'plate'),
]


def tier_cue(item_id):
    """'body' | 'plate' | 'stub' | 'band' | 'jacket' for a tiered item, else None."""
    if tier_of(item_id) is None:
        return None
    for pat, cue in _CUE_RULES:
        if re.search(pat, item_id):
            return cue
    return 'plate'


# ------------------------------------------------------------------------------ other axes
#: Quality ladder, the same everywhere: 0 plain (body material only), 1 copper trim,
#: 2 gold trim.  Two-level families use 0 and 2.
QUALITY_TRIM = {0: None, 1: P.VCOPPER, 2: P.GOLD}
QUALITY = {
    'cheap_ferromagnetic_core': 0, 'average_ferromagnetic_core': 1, 'optimal_ferromagnetic_core': 2,
    'cheap_electrical_drill': 0, 'average_electrical_drill': 1, 'fast_electrical_drill': 2,
    'basic_magnet': 0, 'advanced_magnet': 2,
    'cheap_chip': 0, 'advanced_chip': 2,
    'machine_block': 0, 'advanced_machine_block': 2,
    'electrical_motor': 0, 'advanced_electrical_motor': 2,
    'energy_meter': 0, 'advanced_energy_meter': 2,
    'small_flashlight': 0, 'improved_flashlight': 2,
    'weak_50v_battery_charger': 0, '50v_battery_charger': 2,
}


def quality(item_id):
    return QUALITY.get(item_id)


#: Body boxes at 16 px (x, y, w, h); x2 at 32.  A 'small_' variant is the same drawing scaled
#: into SMALL_BOX (bottom-aligned on row 14, centred), never a different design.
BODY_BOX = (1, 1, 14, 14)
SMALL_BOX = (3, 5, 10, 10)
#: The middle size of a three-size family (small/medium/big fuel burner): 12x12 on the baseline.
MEDIUM_BOX = (2, 3, 12, 12)


def is_small(item_id):
    return item_id.startswith('small_')


_MATERIAL = [(r'copper', 'VCOPPER'), (r'iron', 'IRON'), (r'tungsten', 'TUNGSTEN'),
             (r'carbon', 'CARBON'), (r'lead', 'LEAD'), (r'gold', 'GOLD'), (r'silicon', 'SILICON'),
             (r'alloy', 'ALLOY'), (r'ferrite', 'FERRITE'), (r'coal', 'CARBON'),
             (r'cinnabar', 'CINNABAR')]


def material_of(item_id):
    """Palette name of the material an id names ('50v_tungsten_heating_corp' -> 'TUNGSTEN')."""
    for pat, name in _MATERIAL:
        if re.search(pat, item_id):
            return name
    return None
