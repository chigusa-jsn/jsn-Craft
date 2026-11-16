"""
蒼天の剣聖（剣士の上位職）の六角形スキルボードを作るスクリプト。
真ん中から外へ7周（169枚）。上＝攻め、右下＝技、左下＝守り。外側ほど強くて高い。
使い方: python3 tools/gen_saint_board.py  （sword_saint.json の board を書きかえる）
"""
import json, math, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JOB = os.path.join(ROOT, "src/main/resources/data/jsn_craft/jsn_jobs/sword_saint.json")
RADIUS = 7

ATK='minecraft:generic.attack_damage'; ASPD='minecraft:generic.attack_speed'
CRIT='jsn_craft:crit_chance'; CDMG='jsn_craft:crit_damage'; BOSS='jsn_craft:boss_damage'; LS='jsn_craft:lifesteal'
HP='minecraft:generic.max_health'; ARM='minecraft:generic.armor'; KB='minecraft:generic.knockback_resistance'
HPR='jsn_craft:hp_regen'; DR='jsn_craft:damage_reduction'; SPD='minecraft:generic.movement_speed'
MST='jsn_craft:max_stamina'; STR='jsn_craft:stamina_regen'
SKD='jsn_craft:skill_damage'; CDR='jsn_craft:cooldown_reduction'; SCR='jsn_craft:stamina_cost_reduction'

def r3(v): return round(v, 3)
def A(attr, amt, op=None):
    d={"attribute":attr,"amount":r3(amt)}
    if op: d["operation"]=op
    return d
def S(skill, field, amt): return {"skill":"jsn_craft:sword/"+skill,"field":field,"amount":r3(amt)}
def G(name): return {"grant":"jsn_craft:sword_saint/board/"+name}

ROMAN = ["I","II","III","IV","V","VI","VII"]
# 周ごとの強さの倍率（外側ほど強い）
SCALE = [1.0, 1.0, 1.3, 1.3, 1.6, 1.6, 2.0]
# 周ごとのコスト（タプルは交互）
COST = [1, 1, 1, (1,2), 2, 2, (2,3)]

# 区画ごとの「ふつうのパネル」。周ごとに順番に使い回す
ATTACK_POOL = [
    ("剛力","minecraft:iron_sword",lambda s:[A(ATK,0.75*s)]),
    ("必殺","minecraft:spectral_arrow",lambda s:[A(CRIT,0.03*s)]),
    ("痛撃","minecraft:blaze_powder",lambda s:[A(CDMG,0.10*s)]),
    ("神速","minecraft:feather",lambda s:[A(ASPD,0.1*s)]),
    ("ボス殺し","minecraft:wither_skeleton_skull",lambda s:[A(BOSS,0.05*s)]),
    ("血刃","minecraft:fermented_spider_eye",lambda s:[A(LS,0.01*s)]),
]
TECH_POOL = [
    ("闘気","minecraft:bread",lambda s:[A(MST,15*s)]),
    ("斬り払い・覚醒","minecraft:iron_sword",lambda s:[S("sweep","power_pct",0.1*s)]),
    ("練気","minecraft:honey_bottle",lambda s:[A(STR,0.5*s)]),
    ("旋風斬・覚醒","minecraft:breeze_rod",lambda s:[S("whirlwind","power_pct",0.1*s)]),
    ("極技","minecraft:glowstone_dust",lambda s:[A(SKD,0.04*s)]),
    ("蒼閃・覚醒","minecraft:diamond_sword",lambda s:[S("azure_flash","power_pct",0.1*s)]),
    ("瞬身","minecraft:clock",lambda s:[A(CDR,0.03*s)]),
    ("居合・覚醒","minecraft:netherite_sword",lambda s:[S("iai","power_pct",0.1*s)]),
    ("省気","minecraft:leather",lambda s:[A(SCR,0.03*s)]),
    ("受け流し・覚醒","minecraft:shield",lambda s:[S("parry","power_pct",0.2*s)]),
]
GUARD_POOL = [
    ("金剛体","minecraft:golden_apple",lambda s:[A(HP,3*s)]),
    ("鋼の肌","minecraft:iron_block",lambda s:[A(ARM,1.5*s)]),
    ("守護","minecraft:diamond_chestplate",lambda s:[A(DR,0.02*s)]),
    ("不動","minecraft:iron_boots",lambda s:[A(KB,0.05*s)]),
    ("再生","minecraft:glistering_melon_slice",lambda s:[A(HPR,0.1*s)]),
    ("疾駆","minecraft:leather_boots",lambda s:[A(SPD,0.02*s,"add_multiplied_base")]),
]
POOLS = [ATTACK_POOL, TECH_POOL, GUARD_POOL]
PREFIX = ["atk","tec","grd"]

# 特別なパネル：(区画, 周, 真ん中からのずれ) → 中身
def SP(name, icon, cost, effects, **f): return dict(name=name, icon=icon, cost=cost, effects=effects, **f)
SPECIAL = {
    (0,3,0): SP("追撃","minecraft:prismarine_shard",3,[G("pursuit")],hidden=True),
    (0,5,0): SP("★剛剣・覚醒","minecraft:netherite_sword",6,[A(ATK,0.1,"add_multiplied_total"),A(CDMG,0.2)],big=True,min_level=60),
    (0,6,-2): SP("炎刃","minecraft:blaze_powder",4,[G("ignite")],hidden=True),
    (0,6,2): SP("凍刃","minecraft:blue_ice",4,[G("frost")],hidden=True),
    (0,7,-2): SP("★紅蓮の蒼閃","minecraft:fire_charge",7,[S("azure_flash","power_pct",0.5),S("azure_flash","radius",4)],big=True,min_level=80),
    (0,7,2): SP("★剣聖の極み","minecraft:nether_star",8,[A(ATK,0.15,"add_multiplied_total"),A(CRIT,0.1)],big=True,min_level=90),
    (1,4,0): SP("居合・真打","minecraft:netherite_sword",4,[S("iai","radius",3),S("iai","power_pct",0.3)],hidden=True),
    (1,5,0): SP("★旋風斬・覚醒","minecraft:wind_charge",6,[S("whirlwind","amplifier",1)],big=True,min_level=60),
    (1,6,0): SP("★技の極み","minecraft:glowstone",7,[A(SKD,0.15)],big=True,min_level=80),
    (1,7,0): SP("★無尽","minecraft:heart_of_the_sea",8,[A(SCR,0.1),A(STR,1.5)],big=True,min_level=90),
    (2,3,0): SP("守護の陣","minecraft:beacon",3,[G("guard_aura")],hidden=True),
    (2,5,0): SP("★金剛","minecraft:diamond_chestplate",6,[A(DR,0.05),A(HP,8)],big=True,min_level=60),
    (2,6,0): SP("光の盾","minecraft:shield",4,[G("barrier")],hidden=True),
    (2,7,-2): SP("★鼓舞の陣","minecraft:goat_horn",7,[G("rally_aura")],big=True,min_level=80),
    (2,7,2): SP("★不動明王","minecraft:netherite_chestplate",8,[A(KB,0.3),A(ARM,5),A(HPR,0.5)],big=True,min_level=90),
}

def pix(q, r): return (q + r/2.0, r)
def ring_of(q, r): return max(abs(q), abs(r), abs(-q-r))

cells = [(q, r) for q in range(-RADIUS,RADIUS+1) for r in range(-RADIUS,RADIUS+1) if ring_of(q,r) <= RADIUS]
nodes = {(0,0): dict(id="center", name="剣聖の心得", icon="minecraft:diamond_sword", cost=1,
                     effects=[A(ATK,1.0),A(HP,2)], start=True)}
counters = [0,0,0]
for k in range(1, RADIUS+1):
    ring = [c for c in cells if ring_of(*c) == k]
    def ang(c):
        x, y = pix(*c)
        return (math.degrees(math.atan2(y*0.875, x)) + 150) % 360
    ring.sort(key=ang)
    for s in range(3):
        chunk = ring[s*2*k:(s+1)*2*k]
        mid = len(chunk)//2
        for i, c in enumerate(chunk):
            sp = SPECIAL.get((s, k, i-mid))
            if sp:
                nodes[c] = dict(id=f"{PREFIX[s]}{k}_{i}", **sp)
                continue
            pool = POOLS[s]
            name, icon, fx = pool[counters[s] % len(pool)]
            counters[s] += 1
            cost = COST[k-1]
            if isinstance(cost, tuple): cost = cost[i % 2]
            nodes[c] = dict(id=f"{PREFIX[s]}{k}_{i}", name=f"{name} {ROMAN[k-1]}", icon=icon, cost=cost, effects=fx(SCALE[k-1]))

DIRS = [(1,0),(1,-1),(0,-1),(-1,0),(-1,1),(0,1)]
order = list(nodes.keys())
index = {c:i for i,c in enumerate(order)}
board = []
for c in order:
    n = nodes[c]
    x, y = pix(*c)
    links = [nodes[(c[0]+dq, c[1]+dr)]["id"] for dq,dr in DIRS if (c[0]+dq, c[1]+dr) in nodes and index[(c[0]+dq, c[1]+dr)] < index[c]]
    e = {"id": n["id"], "x": x, "y": y, "name": {"text": n["name"]}, "icon": n["icon"], "cost": n["cost"], "links": links, "effects": n["effects"]}
    for f in ("start","hidden","big"):
        if n.get(f): e[f] = True
    if n.get("min_level"): e["min_level"] = n["min_level"]
    board.append(e)

d = json.load(open(JOB, encoding="utf-8"))
d["board_style"] = "hex"
d["board"] = board
with open(JOB, "w", encoding="utf-8") as f:
    json.dump(d, f, ensure_ascii=False, indent=2)
    f.write("\n")
print(f"{len(board)} 枚 / 合計コスト {sum(n['cost'] for n in board)}（Lv.100でSP 297）")
