#!/usr/bin/env python3
"""NovaTV: generates the navigation sounds (original, synthesized) as small WAV files.
Usage: python3 tools/novatv/gen_sounds.py  -> common/src/main/res/raw/novatv_sound_01..15.wav
The OK button plays the same sound a bit lower (NovaUiSounds)."""
import math, os, random, struct, wave

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'common', 'src', 'main', 'res', 'raw')


def env(t, attack, tau):
    return min(1.0, t / attack) * math.exp(-t / tau)


def render(fn, dur):
    n = int(SR * dur)
    out = [fn(i / SR) for i in range(n)]
    fade = int(SR * 0.006)
    for i in range(fade):  # no click at the end
        out[n - 1 - i] *= i / fade
    return out


def modal(f, modes, attack=0.001):
    # modes: (ratio, amp, tau)
    return lambda t: sum(a * math.sin(2 * math.pi * f * r * t) * env(t, attack, tau) for r, a, tau in modes)


def sweep(f0, f1, k, tau, attack=0.002):
    def fn(t):
        # f(t) = f1 + (f0 - f1) e^(-t/k): phase is its integral
        ph = 2 * math.pi * (f1 * t + (f0 - f1) * k * (1 - math.exp(-t / k)))
        return math.sin(ph) * env(t, attack, tau)
    return fn


def noise(tau, smooth, seed=1):
    rnd = random.Random(seed)
    state = [0.0, 0.0]

    def fn(t):
        x = rnd.uniform(-1, 1)
        state[0] += smooth * (x - state[0])  # two one-pole low-passes: soft, no hiss
        state[1] += smooth * (state[0] - state[1])
        return state[1] * env(t, 0.0008, tau)
    return fn


def mix(*parts):
    return lambda t: sum(g * p(t) for g, p in parts)


def delayed(fn, d):
    return lambda t: fn(t - d) if t >= d else 0.0


SOUNDS = [
    # 1 wooden tock (default)
    (mix((1, modal(620, [(1, 1, .030), (2.32, .45, .012), (4.05, .22, .006), (6.3, .1, .003)])), (.35, noise(.0012, .25))), .09),
    # 2 soft pop
    (sweep(520, 300, .012, .030, .003), .09),
    # 3 glass tick
    (modal(2400, [(1, 1, .012), (2.7, .25, .005)]), .06),
    # 4 marimba
    (modal(784, [(1, 1, .09), (3.9, .25, .02), (9.2, .06, .008)], .002), .2),
    # 5 soft click
    (noise(.004, .35, 3), .03),
    # 6 water drop
    (sweep(500, 1300, .015, .035, .002), .1),
    # 7 felt key
    (modal(523, [(1, 1, .07), (2, .35, .04), (3, .12, .02)], .006), .18),
    # 8 small bell
    (modal(1318, [(1, 1, .16), (2.76, .3, .06), (5.4, .1, .02)], .002), .3),
    # 9 bubble
    (sweep(280, 520, .02, .05, .004), .12),
    # 10 deep thump
    (mix((1, sweep(240, 150, .015, .04, .002)), (.2, noise(.003, .08, 5))), .1),
    # 11 tap
    (modal(1050, [(1, 1, .014), (2, .2, .006)]), .05),
    # 12 kalimba
    (modal(1046, [(1, 1, .12), (5.4, .2, .015), (11.2, .05, .005)], .0015), .25),
    # 13 air
    ((lambda n: lambda t: n(t) * min(1.0, t / .015))(noise(.03, .06, 7)), .08),
    # 14 soft blip
    (lambda t: (math.sin(2 * math.pi * 1500 * t) + .2 * math.sin(2 * math.pi * 4500 * t)) * env(t, .002, .015), .06),
    # 15 double tick
    (mix((1, modal(1400, [(1, 1, .008)])), (.7, delayed(modal(1800, [(1, 1, .008)]), .045))), .1),
]


def loudness(samples):
    # RMS of the first 40 ms: what the ear notices in a short UI sound
    head = samples[:int(SR * .04)]
    return math.sqrt(sum(x * x for x in head) / len(head))


def write(path, samples, target):
    gain = target / loudness(samples)
    peak = max(abs(x) for x in samples) * gain
    if peak > 0.95:
        gain *= 0.95 / peak
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b''.join(struct.pack('<h', int(max(-1, min(1, x * gain)) * 32767)) for x in samples))


def main():
    os.makedirs(OUT, exist_ok=True)
    rendered = [render(fn, dur) for fn, dur in SOUNDS]
    # Sound 1 (the wooden tock) keeps the level the volume settings were tuned on: peak 0.6
    first = rendered[0]
    target = loudness(first) * 0.6 / max(abs(x) for x in first)
    for i, samples in enumerate(rendered, 1):
        write(os.path.join(OUT, 'novatv_sound_%02d.wav' % i), samples, target)
    print('sounds:', len(SOUNDS))


if __name__ == '__main__':
    main()
