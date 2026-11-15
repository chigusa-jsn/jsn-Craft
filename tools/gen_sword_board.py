"""
蒼刃の剣士の六角形スキルボードを作るスクリプト。
真ん中から外へ4周（61枚）。上＝攻め、右下＝技、左下＝守り。
使い方: python3 tools/gen_sword_board.py  （warrior.json の board を書きかえる）
"""
import json, math, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JOB = os.path.join(ROOT, "src/main/resources/data/jsn_craft/jsn_jobs/warrior.json")

ATK='minecraft:generic.attack_damage'; ASPD='minecraft:generic.attack_speed'
CRIT='jsn_craft:crit_chance'; CDMG='jsn_craft:crit_damage'
HP='minecraft:generic.max_health'; ARM='minecraft:generic.armor'; KB='minecraft:generic.knockback_resistance'
MST='jsn_craft:max_stamina'; STR='jsn_craft:stamina_regen'
SKD='jsn_craft:skill_damage'; CDR='jsn_craft:cooldown_reduction'; SCR='jsn_craft:stamina_cost_reduction'
DR='jsn_craft:damage_reduction'; LS='jsn_craft:lifesteal'; XP='jsn_craft:job_xp_bonus'
SPD='minecraft:generic.movement_speed'

def A(attr, amt, op=None):
    d={"attribute":attr,"amount":amt}
    if op: d["operation"]=op
    return d
def S(skill, field, amt): return {"skill":"jsn_craft:sword/"+skill,"field":field,"amount":amt}
def G(name): return {"grant":"jsn_craft:sword/board/"+name}
def P(name, icon, cost, effects, **flags): return dict(name=name, icon=icon, cost=cost, effects=effects, **flags)

CENTER = P("剣の心得","minecraft:wooden_sword",1,[A(ATK,0.5)])

# 区画ごと・周ごとのパネル（1周目2枚、2周目4枚、3周目6枚、4周目8枚）。並び順は時計回り
ATTACK = [
  [P("鋭刃","minecraft:stone_sword",1,[A(ATK,0.5)]), P("急所狙い","minecraft:spectral_arrow",1,[A(CRIT,0.03)])],
  [P("鋭刃 II","minecraft:iron_sword",1,[A(ATK,0.5)]), P("会心 I","minecraft:arrow",1,[A(CRIT,0.03)]),
   P("速剣","minecraft:feather",1,[A(ASPD,0.1)]), P("会心撃 I","minecraft:blaze_powder",2,[A(CDMG,0.1)])],
  [P("鋭刃 III","minecraft:iron_sword",2,[A(ATK,1.0)]), P("会心 II","minecraft:arrow",2,[A(CRIT,0.04)]),
   P("背水の陣","minecraft:redstone",3,[G("last_stand")],hidden=True), P("会心撃 II","minecraft:blaze_powder",2,[A(CDMG,0.15)]),
   P("速剣 II","minecraft:feather",2,[A(ASPD,0.15)]), P("剣気の極意","minecraft:glowstone_dust",2,[S("spirit","chance",0.8)])],
  [P("鋭刃 IV","minecraft:diamond_sword",2,[A(ATK,1.0)]), P("会心 III","minecraft:spectral_arrow",2,[A(CRIT,0.05)]),
   P("会心撃 III","minecraft:blaze_rod",3,[A(CDMG,0.2)]),
   P("★剛剣","minecraft:netherite_sword",5,[A(ATK,0.1,"add_multiplied_total"),A(CDMG,0.1)],big=True,min_level=40),
   P("連撃","minecraft:flint",3,[G("combo")],hidden=True), P("速剣 III","minecraft:sugar",3,[A(ASPD,0.2)]),
   P("鋭刃 V","minecraft:diamond_sword",3,[A(ATK,1.5)],min_level=35), P("吸血の刃","minecraft:fermented_spider_eye",3,[A(LS,0.03)])],
]
TECH = [
  [P("気力","minecraft:bread",1,[A(MST,10)]), P("斬り払い修練","minecraft:iron_sword",1,[S("sweep","power_pct",0.1)])],
  [P("呼吸法","minecraft:honey_bottle",1,[A(STR,0.5)]), P("省力","minecraft:leather",1,[A(SCR,0.05)]),
   P("踏み込み修練","minecraft:feather",1,[S("lunge","cooldown",-1)]), P("気力 II","minecraft:bread",1,[A(MST,10)])],
  [P("技の冴え","minecraft:glowstone_dust",2,[A(SKD,0.05)]), P("旋風斬修練","minecraft:breeze_rod",2,[S("whirlwind","power_pct",0.15)]),
   P("戦意高揚","minecraft:glistering_melon_slice",3,[G("battle_high")],hidden=True), P("蒼閃修練","minecraft:diamond_sword",2,[S("azure_flash","power_pct",0.2)]),
   P("早業","minecraft:clock",2,[A(CDR,0.05)]), P("居合修練","minecraft:netherite_sword",2,[S("iai","cast_time",-0.2)])],
  [P("斬り払い奥伝","minecraft:iron_sword",2,[S("sweep","radius",1)]), P("呼吸法 II","minecraft:honey_bottle",2,[A(STR,0.5)]),
   P("技の冴え II","minecraft:glowstone",3,[A(SKD,0.1)]),
   P("★旋風斬・極","minecraft:wind_charge",4,[S("whirlwind","amplifier",1)],big=True,min_level=30),
   P("★居合・極","minecraft:netherite_sword",4,[S("iai","power_pct",0.3)],big=True,min_level=40),
   P("早業 II","minecraft:clock",3,[A(CDR,0.05)]), P("蒼閃奥伝","minecraft:diamond_sword",2,[S("azure_flash","radius",4)]),
   P("省力 II","minecraft:leather",2,[A(SCR,0.05)])],
]
GUARD = [
  [P("頑健","minecraft:apple",1,[A(HP,2)]), P("鉄の肌","minecraft:iron_nugget",1,[A(ARM,1)])],
  [P("頑健 II","minecraft:apple",1,[A(HP,2)]), P("鉄の肌 II","minecraft:iron_ingot",1,[A(ARM,1)]),
   P("踏ん張り","minecraft:iron_boots",1,[A(KB,0.05)]), P("受け流し修練","minecraft:shield",2,[S("parry","duration",0.3)])],
  [P("頑健 III","minecraft:golden_apple",2,[A(HP,4)]), P("守りの心得","minecraft:iron_chestplate",2,[A(DR,0.03)]),
   P("見切り修練","minecraft:phantom_membrane",2,[S("foresight","chance",0.05)]), P("鉄の肌 III","minecraft:iron_block",2,[A(ARM,2)]),
   P("軽身","minecraft:leather_boots",2,[A(SPD,0.03,"add_multiplied_base")]), P("受け流し・返し","minecraft:shield",2,[S("parry","power_pct",0.5)])],
  [P("頑健 IV","minecraft:golden_apple",2,[A(HP,4)]), P("守りの心得 II","minecraft:diamond_chestplate",3,[A(DR,0.04)]),
   P("踏ん張り II","minecraft:iron_boots",2,[A(KB,0.1)]),
   P("★不屈","minecraft:totem_of_undying",4,[G("endure")],big=True,min_level=35),
   P("★鉄壁","minecraft:netherite_chestplate",5,[A(HP,6),A(DR,0.03)],big=True,min_level=40),
   P("見切り奥伝","minecraft:phantom_membrane",3,[S("foresight","chance",0.05)]), P("鉄の肌 IV","minecraft:diamond_block",2,[A(ARM,2)]),
   P("修練の証","minecraft:experience_bottle",1,[A(XP,0.05)])],
]
SECTORS = [ATTACK, TECH, GUARD]
PREFIX = ["atk", "tec", "grd"]

def pix(q, r): return (q + r/2.0, r)
def ring_of(q, r): return max(abs(q), abs(r), abs(-q-r))

cells = [(q, r) for q in range(-4,5) for r in range(-4,5) if ring_of(q,r) <= 4]
nodes = {}
center_id = "center"
nodes[(0,0)] = dict(id=center_id, **CENTER, start=True)
for k in range(1,5):
    ring = [c for c in cells if ring_of(*c) == k]
    def ang(c):
        x, y = pix(*c)
        a = math.degrees(math.atan2(y*0.875, x))   # 画面の下向きが +y
        return (a + 150) % 360                       # 左上(-150度)から時計回り
    ring.sort(key=ang)
    for s in range(3):
        chunk = ring[s*2*k:(s+1)*2*k]
        items = SECTORS[s][k-1]
        assert len(items) == len(chunk), (s, k, len(items), len(chunk))
        for i, (c, item) in enumerate(zip(chunk, items)):
            nodes[c] = dict(id=f"{PREFIX[s]}{k}_{i}", **item)

DIRS = [(1,0),(1,-1),(0,-1),(-1,0),(-1,1),(0,1)]
order = list(nodes.keys())
board = []
for c in order:
    n = nodes[c]
    x, y = pix(*c)
    links = []
    for dq, dr in DIRS:
        nb = (c[0]+dq, c[1]+dr)
        if nb in nodes and order.index(nb) < order.index(c):
            links.append(nodes[nb]["id"])
    entry = {"id": n["id"], "x": x, "y": y, "name": {"text": n["name"]}, "icon": n["icon"], "cost": n["cost"],
             "links": links, "effects": n["effects"]}
    if n.get("start"): entry["start"] = True
    if n.get("hidden"): entry["hidden"] = True
    if n.get("big"): entry["big"] = True
    if n.get("min_level"): entry["min_level"] = n["min_level"]
    board.append(entry)

d = json.load(open(JOB, encoding="utf-8"))
d["board_style"] = "hex"
d["board"] = board
with open(JOB, "w", encoding="utf-8") as f:
    json.dump(d, f, ensure_ascii=False, indent=2)
    f.write("\n")
total = sum(n["cost"] for n in board)
print(f"{len(board)} 枚 / 合計コスト {total}（SPは最大49）")
for s,name in enumerate(["攻め","技","守り"]):
    print(name, sum(p["cost"] for ring in SECTORS[s] for p in ring))
