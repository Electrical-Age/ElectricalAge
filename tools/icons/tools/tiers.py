"""Shared voltage-tier vocabulary for both icon sets (vanilla and schematic).

The manifest (manifest/manifest.json) spells a tier with one of the CANONICAL keys only:

    'signal' | 'lv' | 'mv' | 'hv' | 'vhv' | 'thermal' | None      (None = untiered; JSON null)

`normalize_tier()` additionally accepts the spellings that appear in item ids, Java code and older
notes (e.g. 'low_voltage', '50V', 'medium_power', '3.2kV'), so a kit never breaks on a synonym.
Bare integers are NOT accepted (is 5 the signal tier or 5 volts?); voltages are accepted only as
the four nominal values 50/200/800/3200, with or without a 'v'.

Antennas: low/medium/high *power* map to LV/MV/HV (VoltageLevelColor.fromVoltage of their nominal U).

Import from a kit:
    sys.path.insert(0, <eln-icons>/tools); from tiers import normalize_tier, CANONICAL
"""

CANONICAL = ('signal', 'lv', 'mv', 'hv', 'vhv', 'thermal')
VOLTAGE_TIERS = ('signal', 'lv', 'mv', 'hv', 'vhv')        # low to high; 'thermal' is not a voltage

ALIASES = {
    'signal': 'signal', 'sig': 'signal', 'signal_voltage': 'signal',
    'lv': 'lv', 'low': 'lv', 'low_voltage': 'lv', '50v': 'lv', 'low_power': 'lv',
    'mv': 'mv', 'medium': 'mv', 'medium_voltage': 'mv', '200v': 'mv', 'medium_power': 'mv',
    'hv': 'hv', 'high': 'hv', 'high_voltage': 'hv', '800v': 'hv', 'high_power': 'hv',
    'vhv': 'vhv', 'very_high': 'vhv', 'very_high_voltage': 'vhv', '3200v': 'vhv', '3.2kv': 'vhv',
    'thermal': 'thermal',
}
_NUMERIC = {50: 'lv', 200: 'mv', 800: 'hv', 3200: 'vhv'}
_UNTIERED = ('', 'none', 'null', 'untiered', 'neutral')


def _key(s):
    return s.strip().lower().replace(' ', '_').replace('-', '_')


def normalize_tier(t):
    """Any accepted spelling -> one of CANONICAL, or None for an untiered item.

    Raises KeyError (with the list of canonical keys) for anything else, including bare 1..5."""
    if t is None:
        return None
    if isinstance(t, bool):
        raise KeyError(f'unknown tier {t!r}; use one of {CANONICAL} or None')
    if isinstance(t, (int, float)):
        if t in _NUMERIC:
            return _NUMERIC[int(t)]
        raise KeyError(f'unknown tier {t!r}: bare numbers are ambiguous; use one of {CANONICAL} or None')
    k = _key(str(t))
    if k in _UNTIERED:
        return None
    k2 = k.replace('_', '')
    for cand in (k, k2):
        if cand in ALIASES:
            return ALIASES[cand]
    # 'lowvoltage', 'veryhighvoltage', 'signalvoltage', ... (underscores dropped)
    flat = {a.replace('_', ''): v for a, v in ALIASES.items()}
    if k2 in flat:
        return flat[k2]
    raise KeyError(f'unknown tier {t!r}; use one of {CANONICAL} or None')


def is_canonical(t):
    """True when t is spelled exactly as the manifest must spell it (canonical key or None)."""
    return t is None or t in CANONICAL


if __name__ == '__main__':
    for s in ('signal', 'LV', 'low_power', '50V', 3200, '3.2kV', 'VeryHighVoltage', None, 'thermal'):
        print(repr(s), '->', normalize_tier(s))
    for bad in (1, 5, 'low_powr'):
        try:
            normalize_tier(bad)
        except KeyError as e:
            print(repr(bad), '-> KeyError', e)
