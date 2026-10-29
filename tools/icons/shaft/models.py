"""Which model (and which parts) each shaft-family icon is rendered from."""
import os
from render3d import load_parts, rot_y180

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
MODEL = os.path.join(REPO, "src/main/resources/assets/eln/model")

# item id: (model dir, parts, extra copies {part: [fn]})
DEVICES = {
    "steam_turbine": ("shaftsteamhp", ["main", "rotor", "valve"], None),
    "lp_steam_turbine": ("shaftsteamlp", ["main", "rotor", "valve"], None),
    "gas_turbine": ("shaftgas", ["main", "rotor", "valve"], None),
    "generator": ("shaftgen4kw", ["main", "rotor"], None),
    "8kw_generator": ("shaftgen8kw", ["main", "rotor"], None),
    "200v_shaft_motor": ("shaftmotor", ["main", "rotor"], None),
    "joint": ("shaftjoint", ["main", "rotor"], None),
    "joint_hub": ("shafthub", ["main", "rotor", "rotor_x"], None),
    "flywheel": ("shaftflywheel", ["main", "rotor"], None),
    "tachometer": ("shafttacho", ["main", "rotor", "needle"], None),
    "plant_steam_condenser": ("plantcondenser", ["main"], None),
    "counterflow_heat_exchanger": ("heatexchanger", ["shell", "flange"], {"flange": [rot_y180]}),
    "iron_tank": ("tankiron", ["shell", "ports", "glass"], None),
    "alloy_tank": ("tankalloy", ["shell", "ports", "glass"], None),
    "200v_inline_pump": ("inlinepump200v", ["main", "rotor"], None),
}


def load(item):
    d, parts, xf = DEVICES[item]
    return load_parts(os.path.join(MODEL, d, d + ".obj"), parts, xf)
