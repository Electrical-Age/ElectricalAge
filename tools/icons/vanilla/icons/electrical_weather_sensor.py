"""electrical_weather_sensor - electricalweathersensor.obj: white square wall box whose face is a
grid of dark-green cells, two of them lit yellow. The model's 4 x 4 grid is drawn 3 x 3 with 2 px
cells (1 px cells at pitch 2 read as dithering at GUI 2); the front is 11 wide so the grid sits
with a 1 px margin on every side. See families/sensors.py."""
from families.sensors import weather_sensor


def draw():
    return weather_sensor()
