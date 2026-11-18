"""
六角形スキルボードを作る道具（全職業共通）。

使い方:
    python3 tools/gen_board.py tools/boards/warrior.json     # 1つだけ作る
    python3 tools/gen_board.py --all                         # tools/boards/ の全部を作る

設定ファイル（tools/boards/<職業>.json）の書き方:
{
  "job": "warrior",                 … data/jsn_craft/jsn_jobs/<job>.json の board を書きかえる
  "radius": 4,                      … 何周にするか（4周=61枚、7周=169枚）
  "costs": [1, 1, 2, 2],            … 周ごとのコスト。[1, 2] のように書くと交互
  "scale": [1, 1, 1.5, 2],          … 周ごとの強さの倍率（amount にかける）
  "center": { パネル },              … 真ん中のパネル
  "sectors": [                      … 区画は3つ（上・右下・左下の順）。4つや6つにもできる
    { "prefix": "atk", "pool": [
        { "name": "鋭刃", "icon": "minecraft:iron_sword", "weight": 3,
          "effects": [ { "attribute": "minecraft:generic.attack_damage", "amount": 0.5 } ] },
        …  weight の割合で並ぶ（3:2:2 なら 3/7 が鋭刃）
    ] }, …
  ],
  "specials": [                     … 決まった場所に置く特別なパネル
    { "sector": 0, "ring": 3, "offset": 0, "name": "背水の陣", "icon": "...", "cost": 3,
      "effects": [ { "grant": "jsn_craft:sword/board/last_stand" } ], "hidden": true }
      … offset は区画の真ん中からのずれ（3区画なら周kの区画には 2k 枚あり、-k 〜 k-1。4区画なら 1.5k 枚くらい）
      … "big": true で★パネル、"min_level": 40 で鍵パネル
  ]
}
パネルの effects は jsn-Craft のボードと同じ書き方（attribute / skill / grant）。
ふつうのパネルの amount は周の倍率がかかる（"scale": false のパネルや、amplifier はかからない）。
"""
import json, math, os, sys, glob

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JOBS = os.path.join(ROOT, "src/main/resources/data/jsn_craft/jsn_jobs")
ROMAN = ["I","II","III","IV","V","VI","VII","VIII","IX","X"]
DIRS = [(1,0),(1,-1),(0,-1),(-1,0),(-1,1),(0,1)]
NO_SCALE_FIELDS = {"amplifier"}


def pix(q, r): return (q + r/2.0, r)
def ring_of(q, r): return max(abs(q), abs(r), abs(-q-r))


def scaled(effects, factor):
    out = []
    for e in effects:
        e = dict(e)
        if "amount" in e and "grant" not in e and e.get("field") not in NO_SCALE_FIELDS:
            e["amount"] = round(e["amount"] * factor, 3)
        out.append(e)
    return out


class Picker:
    """weight の割合どおりに、なるべく均等に並べる（なめらかな重み付きラウンドロビン）"""
    def __init__(self, pool):
        self.pool = pool
        self.cur = [0] * len(pool)
        self.total = sum(p.get("weight", 1) for p in pool)

    def next(self):
        for i, p in enumerate(self.pool):
            self.cur[i] += p.get("weight", 1)
        i = max(range(len(self.pool)), key=lambda j: self.cur[j])
        self.cur[i] -= self.total
        return self.pool[i]


def build(spec_path):
    spec = json.load(open(spec_path, encoding="utf-8"))
    R = spec["radius"]
    costs, scale = spec["costs"], spec["scale"]
    assert len(costs) == R and len(scale) == R, "costs と scale は周の数だけ書いてね"
    sectors = spec["sectors"]
    assert len(sectors) in (3, 4, 6), "区画は3つ・4つ・6つのどれか"
    N = len(sectors)
    specials = {(s["sector"], s["ring"], s["offset"]): s for s in spec.get("specials", [])}
    used = set()

    cells = [(q, r) for q in range(-R, R+1) for r in range(-R, R+1) if ring_of(q, r) <= R]
    center = dict(spec["center"]); center["id"] = "center"; center["start"] = True
    nodes = {(0, 0): center}
    pickers = [Picker(sec["pool"]) for sec in sectors]

    for k in range(1, R+1):
        ring = [c for c in cells if ring_of(*c) == k]
        ring.sort(key=lambda c: (math.degrees(math.atan2(pix(*c)[1]*0.875, pix(*c)[0])) + 150) % 360)
        for s in range(N):
            chunk = ring[round(s*len(ring)/N):round((s+1)*len(ring)/N)]
            mid = len(chunk) // 2
            for i, c in enumerate(chunk):
                nid = f"{sectors[s]['prefix']}{k}_{i}"
                sp = specials.get((s, k, i - mid))
                if sp:
                    used.add((s, k, i - mid))
                    node = {key: v for key, v in sp.items() if key not in ("sector", "ring", "offset")}
                    node["id"] = nid
                    nodes[c] = node
                    continue
                p = pickers[s].next()
                cost = costs[k-1]
                if isinstance(cost, list): cost = cost[i % len(cost)]
                factor = scale[k-1] if p.get("scale", True) else 1
                name = p["name"] + (f" {ROMAN[k-1]}" if p.get("numbered", True) else "")
                nodes[c] = {"id": nid, "name": name, "icon": p["icon"], "cost": p.get("cost", cost),
                            "effects": scaled(p["effects"], factor)}

    missing = set(specials) - used
    assert not missing, f"置けなかった特別なパネルがあるよ（offsetの範囲外？）: {missing}"

    order = list(nodes.keys())
    index = {c: i for i, c in enumerate(order)}
    board = []
    for c in order:
        n = nodes[c]
        x, y = pix(*c)
        links = [nodes[(c[0]+dq, c[1]+dr)]["id"] for dq, dr in DIRS
                 if (c[0]+dq, c[1]+dr) in nodes and index[(c[0]+dq, c[1]+dr)] < index[c]]
        e = {"id": n["id"], "x": x, "y": y, "name": {"text": n["name"]}, "icon": n["icon"],
             "cost": n["cost"], "links": links, "effects": n["effects"]}
        for f in ("start", "hidden", "big"):
            if n.get(f): e[f] = True
        if n.get("min_level"): e["min_level"] = n["min_level"]
        board.append(e)

    job_path = os.path.join(JOBS, spec["job"] + ".json")
    d = json.load(open(job_path, encoding="utf-8"))
    d["board_style"] = "hex"
    d["board"] = board
    with open(job_path, "w", encoding="utf-8") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
        f.write("\n")
    sp_total = (d.get("max_level", 50) - 1) * d.get("skill_points_per_level", 1)
    total = sum(n["cost"] for n in board)
    print(f"{spec['job']}: {len(board)}枚 / 合計コスト {total} / 最大SP {sp_total}（{sp_total*100//total}%取れる）")


if __name__ == "__main__":
    paths = sorted(glob.glob(os.path.join(ROOT, "tools/boards/*.json"))) if "--all" in sys.argv else sys.argv[1:]
    for p in paths:
        build(p)
