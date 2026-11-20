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


def insert():
    n = int(SR * 0.45)
    t = np.arange(n) / SR
    slide_env = np.clip((t - 0.0) / 0.03, 0, 1) * np.exp(-np.maximum(t - 0.05, 0) / 0.03) * (t < 0.16)
    slide = band_noise(n, 900, 5000, periodic=False) * slide_env * 0.25
    latch = click(n, 0.15, 1.6, bright=1.2) + 0.6 * resonator(n, 230, 0.02, 0.15)   # disk seats, shutter opens
    head = click(n, 0.30, 0.8, bright=0.7) + 0.4 * resonator(n, 180, 0.015, 0.30)  # head load pads close
    return slide + latch + head


def eject():
    n = int(SR * 0.40)
    t = np.arange(n) / SR
    button = click(n, 0.005, 0.9, bright=0.8)
    thunk = click(n, 0.06, 1.5, bright=1.0) + 0.9 * resonator(n, 160, 0.03, 0.06)   # spring throws the disk out
    slide_env = np.clip((t - 0.065) / 0.01, 0, 1) * np.exp(-np.maximum(t - 0.07, 0) / 0.05) * (t > 0.065)
    slide = band_noise(n, 900, 5000, periodic=False) * slide_env * 0.25
    return button + thunk + slide


def main():
    out = sys.argv[1]
    save(f"{out}/floppy_spin.wav", spin(), 0.6)
    save(f"{out}/floppy_step.wav", step(), 0.7)
    save(f"{out}/floppy_seek.wav", seek(), 0.7)
    save(f"{out}/floppy_insert.wav", insert(), 0.8)
    save(f"{out}/floppy_eject.wav", eject(), 0.8)


if __name__ == "__main__":
    main()
