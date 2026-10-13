"""200v_battery_charger - batterychargera.obj (see families/batteries.py)."""
from families.batteries import charger_for


def draw():
    return charger_for('200v_battery_charger', 'mv')
