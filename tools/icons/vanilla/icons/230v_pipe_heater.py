"""230v_pipe_heater - pipeheater200v (src/cadmodels PipeHeater), MV: from its front, the pipe line across the picture between steel flanges, the heated section wound with glowing copper turns in two halves between the blue end collars, the small steel terminal box in front with its brass conduit, on the skid. Drawing in families/steam_turbines.py."""
from families.steam_turbines import pipe_heater


def draw():
    return pipe_heater()
