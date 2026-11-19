"""
武器を振る音（風を切る音）を作る。ノイズを、高い音から低い音へすべらせるフィルターに通して、ふくらんで消える音量をつける。
    python3 tools/gen_swing_sounds.py   → assets/jsn_craft/sounds/swing/*.ogg（各3種類）
"""
import os, subprocess, tempfile
import numpy as np
from scipy.signal import butter, sosfilt
from scipy.io import wavfile

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsn_craft/sounds/swing")

def sweep_noise(dur, f0, f1, q=1.6, seed=0, steps=48):
    """f0 → f1 へ中心の高さがすべるノイズ（短い区間ごとにバンドパスをかけてつなぐ）"""
    rng = np.random.default_rng(seed)
    n = int(SR * dur); x = rng.standard_normal(n + 2048)
    out = np.zeros(n); seg = n // steps + 1
    win = np.hanning(seg * 2)
    for i in range(steps):
        t = i / (steps - 1)
        fc = f0 * (f1 / f0) ** t
        lo, hi = fc / (1 + 1 / q), min(fc * (1 + 1 / q), SR / 2 - 100)
        sos = butter(2, [lo, hi], btype="band", fs=SR, output="sos")
        a = max(0, i * seg - seg // 2); b = min(n, a + seg * 2)
        y = sosfilt(sos, x[a:b + 2048])[2048 - 0: 2048 + (b - a)] if False else sosfilt(sos, x[a:b])
        out[a:b] += y * win[: b - a]
    return out

def env(n, attack=0.45, power=2.0):
    """ふくらんで、しぼむ音量（ピークは attack の位置）"""
    t = np.linspace(0, 1, n)
    up = np.clip(t / attack, 0, 1) ** power
    down = np.clip((1 - t) / (1 - attack), 0, 1) ** (power * 0.8)
    return np.where(t < attack, up, down)

def ring(dur, freq, decay, seed):
    """刃の「シャン」という細い響き"""
    n = int(SR * dur); t = np.arange(n) / SR
    rng = np.random.default_rng(seed)
    s = sum(np.sin(2 * np.pi * freq * k * (1 + rng.uniform(-0.003, 0.003)) * t) / k for k in (1, 2.76, 5.4))
    return s * np.exp(-t / decay)

def metal(dur, base, seed, decay=0.12):
    """金属の「キン」：倍音がずれた（金属らしい）響き。はじめが強く、すっと消える"""
    n = int(SR * dur); t = np.arange(n) / SR
    rng = np.random.default_rng(seed)
    out = np.zeros(n)
    for ratio, amp, dk in ((1.0, 1.0, 1.0), (1.59, 0.8, 0.8), (2.43, 0.6, 0.6), (3.17, 0.45, 0.5), (4.62, 0.3, 0.35)):
        f = base * ratio * (1 + rng.uniform(-0.01, 0.01))
        out += amp * np.sin(2 * np.pi * f * t + rng.uniform(0, 6.28)) * np.exp(-t / (decay * dk))
    out *= 1 + 0.15 * np.sin(2 * np.pi * 7 * t)   # 少しゆれる
    return out

def scrape(dur, seed):
    """刃がこすれる「ジャッ」：高いノイズを短く"""
    n = int(SR * dur); rng = np.random.default_rng(seed)
    x = sosfilt(butter(4, [2500, 9000], btype="band", fs=SR, output="sos"), rng.standard_normal(n))
    t = np.arange(n) / SR
    return x * np.exp(-t / 0.018) * np.clip(t / 0.002, 0, 1)

def place(n, sig, at):
    out = np.zeros(n); a = int(SR * at); m = min(len(sig), n - a)
    out[a:a + m] = sig[:m]; return out

def norm(x, peak=0.85):
    return x / (np.max(np.abs(x)) + 1e-9) * peak

def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        wavfile.write(f.name, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", f.name, "-c:a", "libvorbis", "-q:a", "5", os.path.join(OUT, name + ".ogg")], check=True)
        os.unlink(f.name)

def make(kind, v):
    s = v * 17 + 3
    j = 1 + (v - 1) * 0.06   # 3種類で少しずつ高さを変える
    if kind == "light":      # 短剣・拳：短く高い「ヒュッ」＋小さな「チャキ」
        d = 0.22; n = int(SR * d)
        w = norm(sweep_noise(0.15, 3200 * j, 1400 * j, 2.2, s) * env(int(SR * 0.15), 0.35)) * 0.7
        x = place(n, w, 0) + place(n, norm(scrape(0.04, s)) * 0.5, 0.05) + place(n, norm(metal(0.15, 3800 * j, s, 0.04)) * 0.35, 0.055)
    elif kind == "sword":    # 剣：「ジャキン」…風を切る音＋こすれる「ジャッ」＋金属の「キン」
        d = 0.42; n = int(SR * d)
        w = norm(sweep_noise(0.22, 2600 * j, 900 * j, 1.8, s) * env(int(SR * 0.22), 0.5)) * 0.55
        x = place(n, w, 0) + place(n, norm(scrape(0.08, s)) * 0.9, 0.09) + place(n, norm(metal(0.33, 2300 * j, s, 0.10)) * 0.75, 0.10)
    elif kind == "katana":   # 刀：鋭い「シャキン」…細く高い金属音が長めに残る
        d = 0.45; n = int(SR * d)
        w = norm(sweep_noise(0.18, 3200 * j, 1200 * j, 3.0, s) * env(int(SR * 0.18), 0.5, 2.6)) * 0.5
        x = place(n, w, 0) + place(n, norm(scrape(0.06, s)) * 0.7, 0.07) + place(n, norm(metal(0.38, 3300 * j, s, 0.14)) * 0.8, 0.075)
    elif kind == "heavy":    # 斧・大技：低く重い「ブォン」
        d = 0.45; x = sweep_noise(d, 1100 * j, 260 * j, 1.2, s) * env(int(SR * d), 0.5, 1.6)
    elif kind == "thrust":   # 槍：短く空気を裂く「シュッ」
        d = 0.20; x = sweep_noise(d, 1900 * j, 1300 * j, 1.4, s) * env(int(SR * d), 0.3, 1.4)
    elif kind == "whip":     # 鞭：しなる音のあとに「パシッ」
        d = 0.32; n = int(SR * d)
        x = norm(sweep_noise(d, 900 * j, 2600 * j, 1.5, s) * env(n, 0.75, 2.0)) * 0.6
        crack = np.zeros(n); c0 = int(n * 0.72); m = int(SR * 0.05)
        rng = np.random.default_rng(s)
        burst = rng.standard_normal(m) * np.exp(-np.arange(m) / (SR * 0.006))
        crack[c0:c0 + m] = sosfilt(butter(2, 1500, btype="high", fs=SR, output="sos"), burst)[: n - c0]
        x = x + norm(crack)
    else:
        raise ValueError(kind)
    return norm(x)

KINDS = ["light", "sword", "katana", "heavy", "thrust", "whip"]
if __name__ == "__main__":
    for k in KINDS:
        for v in (1, 2, 3):
            save(f"{k}_{v}", make(k, v))
    print("ok", len(KINDS) * 3)
