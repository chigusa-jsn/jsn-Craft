"""遊び人（気ままな遊び人 → 天下の大遊び人）：生活ジョブの職業・スキル・ボード設定を書き出す。"""
import json, os
R = os.path.join(os.path.dirname(__file__), "..")
D = f"{R}/src/main/resources/data/jsn_craft"
def wj(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f: json.dump(o, f, ensure_ascii=False, indent=2); f.write("\n")
def sk(job, sid, name, desc, typ, effect, **kw):
    o = {"name": {"text": name}, "description": {"text": desc}, "icon": f"jsn_craft:skill_icon/{job}/{sid}", "type": typ, "effect": effect}
    o.update(kw); wj(f"{D}/jsn_skills/{job}/{sid}.json", o); return sid

BASE = [
    (1, sk("gadabout", "playful", "遊び心", "いつも運（幸運）+1。", "passive", "gad_luck_1", power=1)),
    (5, sk("gadabout", "casino_sense", "勝負勘", "カジノで少しツイてる：スロット・パチスロ・パチンコが3%当たりやすく、テーブルで負けたチップの3%がもどる。",
           "passive", "gad_casino_1", power=0.03)),
    (10, sk("gadabout", "finder", "拾い物上手", "ブロックを壊すと、1%で金塊・エメラルド・ダイヤを拾う。運が高いほど拾いやすい。", "passive", "gad_finder", chance=0.01)),
    (12, sk("gadabout", "whistle", "口笛", "まわり16ブロックの仲間と自分の運を +2（60秒）。", "active", "gad_whistle",
            stamina=10, cooldown=120, power=2, radius=16, duration=60)),
    (15, sk("gadabout", "lucky_cat", "招き猫", "いつも運（幸運）+1。", "passive", "gad_luck_2", power=1)),
    (20, sk("gadabout", "gambler_sense", "大勝負の勘", "カジノの勝負勘がさらに +3%。", "passive", "gad_casino_2", power=0.03)),
    (25, sk("gadabout", "lottery", "宝くじ", "チップ10枚で宝くじを1枚。1等3000枚(1/1000)・2等300枚(1/100)・3等30枚(1/10)・4等10枚(1/3)。運が高いと2回引ける。",
            "active", "gad_lottery", cooldown=600, bonus=10)),
    (30, sk("gadabout", "lucky_body", "幸運体質", "いつも運（幸運）+2。", "passive", "gad_luck_3", power=2)),
]
UPPER = [
    (10, sk("grand_gadabout", "lucky_star", "幸運の星", "いつも運（幸運）+2。", "passive", "gad_luck_4", power=2)),
    (30, sk("grand_gadabout", "house_favorite", "カジノの常連", "カジノの勝負勘がさらに +4%。", "passive", "gad_casino_3", power=0.04)),
    (50, sk("grand_gadabout", "big_lottery", "一攫千金", "チップ100枚で特別な宝くじ。1等10万枚(1/1000)・2等2000枚(1/50)・3等200枚(1/5)・4等60枚(1/2)。",
            "active", "gad_big_lottery", cooldown=1800, bonus=100)),
    (70, sk("grand_gadabout", "heaven_luck", "天運", "5分間、運（幸運）+10。", "active", "gad_heaven", cooldown=3600, power=10, duration=300)),
]
XP = [{"action": "casino_jackpot", "target": "*", "xp": 40}, {"action": "casino_play", "target": "*", "xp": 2}]
def job(jid, name, color, tier, maxlv, sp, skills, requires=None):
    path = f"{D}/jsn_jobs/{jid}.json"
    old = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    o = {"name": {"text": name, "color": color}, "type": "life", "tier": tier, "max_level": maxlv, "skill_points_per_level": sp}
    if requires: o["requires"] = requires
    o["xp"] = XP
    o["skills"] = [{"skill": f"jsn_craft:{jid}/{s}", "level": lv} for lv, s in skills]
    o["board_style"] = "hex"
    if "board" in old: o["board"] = old["board"]
    wj(path, o)
job("gadabout", "気ままな遊び人", "aqua", "basic", 50, 2, BASE)
job("grand_gadabout", "天下の大遊び人", "gold", "advanced", 100, 3, UPPER, [{"job": "jsn_craft:gadabout", "level": 30}])

def S(skill, field, amount, **kw): return dict({"skill": f"jsn_craft:{skill}", "field": field, "amount": amount}, **kw)
def A(attr, amount): return {"attribute": attr, "amount": amount}
LUCK, XPB = "minecraft:generic.luck", "jsn_craft:job_xp_bonus"
wj(f"{R}/tools/boards/gadabout.json", {
    "job": "gadabout", "radius": 4, "costs": [1, 1, 2, 2], "scale": [1, 1, 1.5, 2],
    "center": {"name": "遊び人の心得", "icon": "minecraft:rabbit_foot", "cost": 1, "effects": [A(LUCK, 1), A(XPB, 0.03)]},
    "sectors": [
        {"prefix": "luk", "pool": [{"name": "強運", "icon": "minecraft:rabbit_foot", "weight": 3, "effects": [A(LUCK, 0.4)]},
                                   {"name": "ツイてる", "icon": "minecraft:gold_nugget", "weight": 1, "effects": [S("gadabout/casino_sense", "power", 0.005)]}]},
        {"prefix": "fun", "pool": [{"name": "遊びの達人", "icon": "minecraft:book", "weight": 2, "effects": [A(XPB, 0.03)]},
                                   {"name": "拾い物", "icon": "minecraft:emerald", "weight": 1, "effects": [S("gadabout/finder", "chance", 0.002)]},
                                   {"name": "身軽", "icon": "minecraft:sugar", "weight": 1, "effects": [A("minecraft:generic.movement_speed", 0.004)]}]},
        {"prefix": "lzy", "pool": [{"name": "のんき", "icon": "minecraft:bread", "weight": 2, "effects": [A("jsn_craft:max_stamina", 8)]},
                                   {"name": "丈夫", "icon": "minecraft:apple", "weight": 2, "effects": [A("minecraft:generic.max_health", 1)]},
                                   {"name": "昼寝", "icon": "minecraft:honey_bottle", "weight": 1, "effects": [A("jsn_craft:stamina_regen", 0.4)]}]}],
    "specials": [
        {"sector": 0, "ring": 3, "offset": 0, "name": "四つ葉", "icon": "minecraft:lily_of_the_valley", "cost": 3, "effects": [A(LUCK, 2)]},
        {"sector": 0, "ring": 4, "offset": -1, "name": "★天性の遊び人", "icon": "minecraft:nether_star", "cost": 5, "big": True, "min_level": 40,
         "effects": [S("gadabout/casino_sense", "power", 0.02, scale=False)]},
        {"sector": 1, "ring": 3, "offset": 0, "name": "大きな口笛", "icon": "minecraft:goat_horn", "cost": 3, "effects": [S("gadabout/whistle", "power", 1)]},
        {"sector": 1, "ring": 4, "offset": -1, "name": "★宝の目", "icon": "minecraft:diamond", "cost": 5, "big": True, "min_level": 30,
         "effects": [S("gadabout/finder", "chance", 0.01, scale=False)]},
        {"sector": 2, "ring": 3, "offset": 0, "name": "毎日宝くじ", "icon": "minecraft:paper", "cost": 3, "effects": [S("gadabout/lottery", "cooldown", -120)]},
        {"sector": 2, "ring": 4, "offset": -1, "name": "★お気楽な体", "icon": "minecraft:golden_apple", "cost": 5, "big": True, "min_level": 40,
         "effects": [A("minecraft:generic.max_health", 4), A("jsn_craft:max_stamina", 20)]}]})
wj(f"{R}/tools/boards/grand_gadabout.json", {
    "job": "grand_gadabout", "radius": 7, "costs": [1, 1, 1, [1, 2], 2, 2, [2, 3]], "scale": [1.5, 1.5, 2, 2, 2.4, 2.4, 3],
    "center": {"name": "大遊び人の心得", "icon": "minecraft:nether_star", "cost": 1, "effects": [A(LUCK, 2), A(XPB, 0.06)]},
    "sectors": [
        {"prefix": "luk", "pool": [{"name": "強運", "icon": "minecraft:rabbit_foot", "weight": 3, "effects": [A(LUCK, 0.4)]},
                                   {"name": "ツイてる", "icon": "minecraft:gold_nugget", "weight": 1, "effects": [S("grand_gadabout/house_favorite", "power", 0.004)]}]},
        {"prefix": "fun", "pool": [{"name": "遊びの達人", "icon": "minecraft:book", "weight": 2, "effects": [A(XPB, 0.03)]},
                                   {"name": "拾い物", "icon": "minecraft:emerald", "weight": 1, "effects": [S("gadabout/finder", "chance", 0.002)]},
                                   {"name": "身軽", "icon": "minecraft:sugar", "weight": 1, "effects": [A("minecraft:generic.movement_speed", 0.004)]}]},
        {"prefix": "lzy", "pool": [{"name": "のんき", "icon": "minecraft:bread", "weight": 2, "effects": [A("jsn_craft:max_stamina", 10)]},
                                   {"name": "丈夫", "icon": "minecraft:apple", "weight": 2, "effects": [A("minecraft:generic.max_health", 1.5)]},
                                   {"name": "昼寝", "icon": "minecraft:honey_bottle", "weight": 1, "effects": [A("jsn_craft:stamina_regen", 0.5)]}]}],
    "specials": [
        {"sector": 0, "ring": 3, "offset": 0, "name": "七つ葉", "icon": "minecraft:lily_of_the_valley", "cost": 3, "effects": [A(LUCK, 3)]},
        {"sector": 0, "ring": 5, "offset": 0, "name": "★天運・極", "icon": "minecraft:beacon", "cost": 5, "big": True, "min_level": 60,
         "effects": [S("grand_gadabout/heaven_luck", "duration", 300, scale=False)]},
        {"sector": 0, "ring": 6, "offset": 0, "name": "★カジノの主", "icon": "minecraft:gold_block", "cost": 5, "big": True, "min_level": 80,
         "effects": [S("grand_gadabout/house_favorite", "power", 0.02, scale=False)]},
        {"sector": 1, "ring": 3, "offset": 0, "name": "宝くじ好き", "icon": "minecraft:paper", "cost": 3, "effects": [S("grand_gadabout/big_lottery", "cooldown", -300)]},
        {"sector": 1, "ring": 5, "offset": 0, "name": "★宝の山", "icon": "minecraft:diamond_block", "cost": 5, "big": True, "min_level": 60,
         "effects": [S("gadabout/finder", "chance", 0.01, scale=False)]},
        {"sector": 2, "ring": 3, "offset": 0, "name": "みんなで口笛", "icon": "minecraft:goat_horn", "cost": 3, "effects": [S("gadabout/whistle", "duration", 60)]},
        {"sector": 2, "ring": 5, "offset": 0, "name": "★遊び人の体", "icon": "minecraft:golden_apple", "cost": 5, "big": True, "min_level": 60,
         "effects": [A("minecraft:generic.max_health", 6), A("jsn_craft:max_stamina", 30)]}]})
print("gadabout data ok")
