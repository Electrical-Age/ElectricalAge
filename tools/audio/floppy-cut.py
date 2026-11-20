#!/usr/bin/env python3
"""Cut EA floppy spin/step/seek from the CC0 recording "Floppy drive sounds.ogg" (Wikimedia Commons, AlepouTheFox,
CC0; a 3.5" drive formatting). Usage: floppy-cut.py IN.wav (mono 44.1k) OUTDIR
Steps in it: every 1.2 s from 5.54 s. Spin: the whir between two steps, made into a seamless loop by crossfade."""
import sys, wave
import numpy as np
SR = 44100
w = wave.open(sys.argv[1]); x = np.frombuffer(w.readframes(w.getnframes()), np.int16) / 32768; out = sys.argv[2]
def s(t): return int(t * SR)
def save(name, y, peak):
    y = y / np.max(np.abs(y)) * peak
    with wave.open(f"{out}/{name}.wav", "wb") as o:
        o.setnchannels(1); o.setsampwidth(2); o.setframerate(SR); o.writeframes((y * 32767).astype(np.int16).tobytes())
def fade(y, fi, fo):
    y = y.copy(); a, b = s(fi), s(fo)
    if a: y[:a] *= np.linspace(0, 1, a)
    if b: y[-b:] *= np.linspace(1, 0, b)
    return y
# a step: onset found by the envelope near 5.54 s
seg = x[s(5.50):s(5.62)]
on = s(5.50) + int(np.argmax(np.abs(seg) > 0.5 * np.abs(seg).max())) - s(0.004)
step = fade(x[on:on + s(0.09)], 0.002, 0.04)
save("floppy_step", step, 0.7)
# the spin loop: 0.9 s between steps, its last 0.15 s crossfaded into its start
L, X = s(0.90), s(0.15)
a = x[s(5.75):s(5.75) + L + X].copy()
loop = a[:L].copy(); r = np.linspace(0, 1, X)
loop[:X] = a[:X] * np.sqrt(r) + a[L:L + X] * np.sqrt(1 - r)
save("floppy_spin", loop, 0.6)
# a seek: the real step every ~6 ms (a 3.5" drive steps in 3-6 ms), 14 steps
rng = np.random.default_rng(541); seek = np.zeros(s(0.18)); t = 0
for i in range(14):
    k = s(t); seek[k:k + len(step)] += step[:len(seek) - k] * rng.uniform(0.6, 0.8); t += 0.0062 * rng.uniform(0.95, 1.05)
save("floppy_seek", fade(seek, 0, 0.05), 0.7)
