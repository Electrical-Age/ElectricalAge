#!/usr/bin/env python3
"""Synthesize EA computer floppy drive sounds (todo #541, nero 1573294363776127080).

Usage: floppy-synth.py OUTDIR   -> OUTDIR/floppy_{spin,step,seek,insert,eject}.wav (mono 44.1 kHz)
Then: for f in OUTDIR/*.wav: ffmpeg -i f -c:a libvorbis -q:a 4 f.ogg
floppy_spin is seamlessly periodic (built in the frequency domain), so it loops without a seam.
Everything is synthesized here: no samples, no licence questions.
"""
import sys, wave
import numpy as np

SR = 44100
rng = np.random.default_rng(541)


def save(path, x, peak):
    x = x / (np.max(np.abs(x)) + 1e-12) * peak
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def band_noise(n, lo, hi, tilt=0.0, periodic=True):
    """Noise with a flat-ish band lo..hi Hz (soft edges); irfft makes it exactly periodic over n samples."""
    f = np.fft.rfftfreq(n, 1 / SR)
    mag = 1 / (1 + ((lo / np.maximum(f, 1)) ** 4)) / (1 + (f / hi) ** 4)
    mag *= np.maximum(f, 1) ** tilt
    ph = rng.uniform(0, 2 * np.pi, len(f))
    x = np.fft.irfft(mag * np.exp(1j * ph), n)
    return x / np.std(x)


def resonator(n, freq, decay_s, t0=0):
    t = np.arange(n) / SR - t0
    env = np.where(t >= 0, np.exp(-np.maximum(t, 0) / decay_s), 0)
    return np.sin(2 * np.pi * freq * np.maximum(t, 0)) * env


def click(n, t0, strength=1.0, bright=1.0):
    """A small plastic/metal tick: a few damped modes plus a burst of noise."""
    x = np.zeros(n)
    for fr, dec, a in ((1150, 0.006, 1.0), (2900, 0.0035, 0.6 * bright), (5200, 0.002, 0.3 * bright), (420, 0.010, 0.5)):
        x += a * resonator(n, fr * rng.uniform(0.97, 1.03), dec, t0)
    t = np.arange(n) / SR - t0
    burst = rng.normal(0, 1, n) * np.where(t >= 0, np.exp(-np.maximum(t, 0) / 0.0012), 0)
    return strength * (x + 0.5 * burst)


def spin():
    # 2.4 s loop; the spindle turns at 300 rpm (5 Hz): 12 turns, so its wobble is periodic too.
    n = int(SR * 2.4)
    t = np.arange(n) / SR
    whir = band_noise(n, 180, 900, tilt=-0.3) * 0.35
    air = band_noise(n, 1500, 5000) * 0.05
    # motor tones on exact loop-periodic frequencies (multiples of 1/2.4 Hz)
    def tone(f, a):
        k = round(f * 2.4) / 2.4
        return a * np.sin(2 * np.pi * k * t + rng.uniform(0, 6.28))
    hum = tone(150, 0.30) + tone(300, 0.18) + tone(450, 0.06) + tone(1200, 0.03)
    wobble = 1 + 0.18 * np.sin(2 * np.pi * 5 * t) + 0.06 * np.sin(2 * np.pi * 10 * t + 1)
    # a faint once-per-turn rub of the disk in its jacket
    rub = band_noise(n, 2000, 6000) * (0.5 + 0.5 * np.cos(2 * np.pi * 5 * t)) ** 6 * 0.08
    return (whir + air + hum) * wobble + rub


def step():
    n = int(SR * 0.06)
    return click(n, 0.002, 1.0, bright=0.8)


def seek():
    # a seek across several tracks: steps every ~6 ms, a soft buzz, not a clatter
    n = int(SR * 0.16)
    x = np.zeros(n)
    t0 = 0.002
    for i in range(18):
        x += click(n, t0, rng.uniform(0.55, 0.75), bright=0.6)
        t0 += 0.0062 * rng.uniform(0.95, 1.05)
    return x


def rustle(n, t0, t1, level):
    """The disk's jacket sliding in the slot: low-heavy friction noise with a grainy, uneven amplitude."""
    t = np.arange(n) / SR
    env = np.clip((t - t0) / 0.02, 0, 1) * np.clip((t1 - t) / 0.04, 0, 1)
    grain = np.abs(band_noise(n, 15, 60)) ** 1.5          # slow random swells (the hand's push)
    grain /= grain.max()
    return band_noise(n, 120, 3000, tilt=-0.4, periodic=False) * env * (0.4 + grain) * level


def clack(n, t0, strength, centroid=2500, ring=0.2):
    """A latch snapping home in a plastic housing (nero's reference, 2026-11-20: centroid ~2.5 kHz, ~0.2 s to
    -20 dB): a bright snap plus the case's body modes ringing on."""
    x = click(n, t0, 1.0, bright=1.2)
    for fr, a, dec in ((centroid * 0.9, 0.5, ring / 2.5), (centroid * 1.6, 0.3, ring / 3), (centroid * 0.45, 0.4, ring / 2.5),
                       (650, 0.15, ring / 4), (330, 0.18, ring / 3), (170, 0.2, ring / 3)):
        x += a * resonator(n, fr * rng.uniform(0.97, 1.03), dec, t0)
    t = np.arange(n) / SR - t0
    x += 0.3 * band_noise(n, 1500, 7000, periodic=False) * np.where(t >= 0, np.exp(-np.maximum(t, 0) / (ring / 4)), 0)
    return strength * x


def insert():
    # (in the reference the hand takes 2 s; in the game the disk just goes in, so the slide is short)
    n = int(SR * 0.75)
    return (rustle(n, 0.0, 0.36, 0.08) + click(n, 0.03, 0.25, bright=0.6)
            + clack(n, 0.36, 1.0, centroid=2700, ring=0.21) + 0.25 * clack(n, 0.46, 1.0, centroid=1300, ring=0.1))


def eject():
    # reference: a dull press of the button (centroid ~430 Hz), 60 ms later the spring's bright clack
    # (~2.3 kHz, 0.25 s), then the disk rustling out
    n = int(SR * 0.75)
    press = click(n, 0.005, 0.35, bright=0.2) + 0.5 * resonator(n, 200, 0.03, 0.005)
    return (press + clack(n, 0.065, 1.0, centroid=2300, ring=0.25)
            + rustle(n, 0.09, 0.5, 0.07) + click(n, 0.42, 0.2, bright=0.5))


def main():
    out = sys.argv[1]
    save(f"{out}/floppy_spin.wav", spin(), 0.6)
    save(f"{out}/floppy_step.wav", step(), 0.7)
    save(f"{out}/floppy_seek.wav", seek(), 0.7)
    save(f"{out}/floppy_insert.wav", insert(), 0.8)
    save(f"{out}/floppy_eject.wav", eject(), 0.8)


if __name__ == "__main__":
    main()
