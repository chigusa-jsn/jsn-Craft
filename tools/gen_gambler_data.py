"""ギャンブラー（賽振りの博徒 → 天運の大勝負師）の職業・スキル・ボードの設定を書き出す。
そのあと tools/gen_board.py tools/boards/gambler.json などでボードを作り、gen_skill_icons.py でアイコンを作る。"""
import json, os
R = os.path.join(os.path.dirname(__file__), "..")
D = f"{R}/src/main/resources/data/jsn_craft"
def wj(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f: json.dump(o, f, ensure_ascii=False, indent=2); f.write("\n")

XP = json.load(open(f"{D}/jsn_jobs/beast_tamer.json", encoding="utf-8"))["xp"]

def sk(job, sid, name, desc, typ, effect, **kw):
    o = {"name": {"text": name}, "description": {"text": desc}, "icon": f"jsn_craft:skill_icon/{job}/{sid}", "type": typ, "effect": effect}
    o.update(kw)
    wj(f"{D}/jsn_skills/{job}/{sid}.json", o)
    return sid

BASE = [
    (1, sk("gambler", "card_throw", "カード投げ", "刃のふちのカードを扇形に3枚投げる（1枚 攻撃力の0.8倍）。", "active", "gamble_card_throw",
           stamina=10, cooldown=3, power=0.8, radius=16)),
    (1, sk("gambler", "coin_toss", "コイントス", "コインを投げる。表なら8秒間 攻撃 +40%、裏なら体力 +4 と足が速くなる。", "active", "gamble_coin_toss",
           stamina=6, cooldown=10, power=0.4, bonus=4, duration=8)),
    (3, sk("gambler", "dice_shot", "ダイス撃ち", "さいころを投げつける。出た目 ×（攻撃力の0.35倍）のダメージ。6の目なら敵が動けなくなる。", "active", "gamble_dice_shot",
           stamina=12, cooldown=6, power=0.35, radius=12)),
    (5, sk("gambler", "lucky", "ツキ", "攻撃のたび 12% で「ラッキー！」、ダメージ +50%。運（幸運）が高いほど起こりやすい。", "passive", "gamble_lucky",
           chance=0.12, power=0.5)),
    (8, sk("gambler", "chip_guard", "チップの盾", "さいころ2つの合計に合わせて、守りのハート（衝撃吸収）を得る（12秒）。", "active", "gamble_chip_guard",
           stamina=12, cooldown=20, duration=12)),
    (10, sk("gambler", "roulette", "ルーレット", "まわりで盤を回す。赤なら燃やし、黒なら弱らせる（攻撃力の1.2倍）。0なら大当たりで3倍！", "active", "gamble_roulette",
            stamina=18, cooldown=12, power=1.2, radius=5)),
    (12, sk("gambler", "lucky_seven", "ラッキーセブン", "ふつうの攻撃7回ごとに、ダメージ +200%。", "passive", "gamble_lucky_seven", power=2.0)),
    (15, sk("gambler", "double_up", "ダブルアップ", "次の一撃が 55% で2倍、外れると半分になる（10秒以内）。", "active", "gamble_double_up",
            stamina=8, cooldown=15, duration=10, chance=0.55)),
    (18, sk("gambler", "poker_hand", "ポーカーハンド", "5枚引いて、役が強いほどねらった場所のまわりに大ダメージ（ワンペアで攻撃力の1倍、ロイヤルで10倍）。", "active",
            "gamble_poker_hand", stamina=20, cooldown=14, power=1.0, radius=4)),
    (20, sk("gambler", "all_in", "オールイン", "いまの体力の35%を賭けて、8秒間 ダメージ +100%。", "active", "gamble_all_in",
            stamina=10, cooldown=40, chance=0.35, power=1.0, duration=8)),
    (22, sk("gambler", "lucky_punch", "ラッキーパンチ", "目の前の敵をなぐる（攻撃力の1倍）。1%で一撃必殺！ 運（幸運）1につき +0.2%（最大5%）。ボスには効きにくい。", "active",
            "gamble_lucky_punch", stamina=8, cooldown=5, power=1.0, chance=0.01, radius=4)),
    (25, sk("gambler", "high_roll", "大勝負", "体力が減っているほど強くなる（体力ゼロに近いと ダメージ +60%）。", "passive", "gamble_high_roll", power=0.6)),
    (30, sk("gambler", "slot_burst", "スロットバースト", "3つの絵柄を回して爆発。そろえば4倍、7がそろえば7倍、2つで2倍（攻撃力の1.2倍が基本）。", "active",
            "gamble_slot_burst", stamina=25, cooldown=18, power=1.2, radius=4)),
]
UPPER = [
    (10, sk("high_roller", "jackpot_rain", "ジャックポット", "ねらった場所に金貨の雨を3秒降らせる（0.25秒ごとに攻撃力の0.5倍）。", "active", "gamble_jackpot_rain",
            stamina=30, cooldown=25, power=0.5, radius=6, duration=3)),
    (20, sk("high_roller", "fate_dice", "運命の賽", "さいころを2つ振り、合計 ×（攻撃力の0.6倍）を3回に分けて当てる。ゾロ目ならもう一度振れる。", "active",
            "gamble_fate_dice", stamina=25, cooldown=16, power=0.6, radius=16)),
    (35, sk("high_roller", "house_rules", "胴元の掟", "15秒間、まわり8ブロックの敵は与えるダメージ -12%、受けるダメージ +25%。", "active", "gamble_house_rules",
            stamina=30, cooldown=45, power=0.25, radius=8, duration=15)),
    (50, sk("high_roller", "royal_flush", "★ロイヤルフラッシュ", "必ずロイヤルストレートフラッシュ。ねらった場所のまわりに攻撃力の10倍。", "active", "gamble_royal_flush",
            stamina=40, cooldown=60, power=1.0, radius=5)),
    (70, sk("high_roller", "seven_heaven", "奥義 セブンス・ヘヴン", "まわりに7本の光の柱を立て（1本 攻撃力の2.5倍）、最後に全体へ5倍。", "active", "gamble_seven_heaven",
            stamina=60, cooldown=90, power=2.5, radius=9)),
]

def job(jid, name, color, tier, maxlv, sp, skills, requires=None):
    path = f"{D}/jsn_jobs/{jid}.json"
    old = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    o = {"name": {"text": name, "color": color}, "type": "combat", "tier": tier, "max_level": maxlv, "skill_points_per_level": sp}
    if requires: o["requires"] = requires
    o["xp"] = XP
    o["skills"] = [{"skill": f"jsn_craft:{jid}/{s}", "level": lv} for lv, s in skills]
    o["board_style"] = "hex"
    if "board" in old: o["board"] = old["board"]
    wj(path, o)

job("gambler", "賽振りの博徒", "light_purple", "basic", 50, 2, BASE)
job("high_roller", "天運の大勝負師", "gold", "advanced", 100, 3, UPPER, [{"job": "jsn_craft:gambler", "level": 30}])

# ---- ボード
def S(skill, field, amount, **kw): return dict({"skill": f"jsn_craft:{skill}", "field": field, "amount": amount}, **kw)
def A(attr, amount): return {"attribute": attr, "amount": amount}
LUCK, CRIT, CRITD, SKD, CDR = "minecraft:generic.luck", "jsn_craft:crit_chance", "jsn_craft:crit_damage", "jsn_craft:skill_damage", "jsn_craft:cooldown_reduction"
wj(f"{R}/tools/boards/gambler.json", {
    "job": "gambler", "radius": 4, "costs": [1, 1, 2, 2], "scale": [1, 1, 1.5, 2],
    "center": {"name": "博徒の心得", "icon": "jsn_craft:paper_cards", "cost": 1,
               "effects": [A(SKD, 0.03), A(LUCK, 0.5), A("jsn_craft:max_stamina", 10)]},
    "sectors": [
        {"prefix": "luk", "pool": [
            {"name": "強運", "icon": "minecraft:rabbit_foot", "weight": 3, "effects": [A(LUCK, 0.3)]},
            {"name": "ツキ修練", "icon": "minecraft:gold_nugget", "weight": 1, "effects": [S("gambler/lucky", "chance", 0.02)]},
            {"name": "コイン修練", "icon": "minecraft:gold_ingot", "weight": 1, "effects": [S("gambler/coin_toss", "power", 0.05)]},
            {"name": "会心", "icon": "minecraft:arrow", "weight": 2, "effects": [A(CRIT, 0.03)]}]},
        {"prefix": "crd", "pool": [
            {"name": "鋭い札", "icon": "minecraft:paper", "weight": 2, "effects": [A("minecraft:generic.attack_damage", 0.4)]},
            {"name": "カード投げ修練", "icon": "minecraft:paper", "weight": 1, "effects": [S("gambler/card_throw", "power_pct", 0.1)]},
            {"name": "ポーカー修練", "icon": "minecraft:map", "weight": 1, "effects": [S("gambler/poker_hand", "power_pct", 0.1)]},
            {"name": "早業", "icon": "minecraft:clock", "weight": 2, "effects": [A(CDR, 0.04)]},
            {"name": "大技", "icon": "minecraft:blaze_powder", "weight": 1, "effects": [A(SKD, 0.03)]}]},
        {"prefix": "nrv", "pool": [
            {"name": "気力", "icon": "minecraft:bread", "weight": 2, "effects": [A("jsn_craft:max_stamina", 10)]},
            {"name": "度胸", "icon": "minecraft:apple", "weight": 2, "effects": [A("minecraft:generic.max_health", 1.5)]},
            {"name": "呼吸法", "icon": "minecraft:honey_bottle", "weight": 2, "effects": [A("jsn_craft:stamina_regen", 0.5)]},
            {"name": "会心の一撃", "icon": "minecraft:redstone", "weight": 1, "effects": [A(CRITD, 0.08)]}]}],
    "specials": [
        {"sector": 0, "ring": 3, "offset": 0, "name": "イカサマ賽", "icon": "minecraft:bone_meal", "cost": 3, "effects": [A(LUCK, 1.5)]},
        {"sector": 0, "ring": 4, "offset": -1, "name": "★天性の強運", "icon": "minecraft:rabbit_foot", "cost": 5, "big": True, "min_level": 40,
         "effects": [S("gambler/lucky", "power", 0.25, scale=False), S("gambler/lucky", "chance", 0.05, scale=False)]},
        {"sector": 1, "ring": 3, "offset": 0, "name": "5枚投げ", "icon": "minecraft:paper", "cost": 3, "effects": [S("gambler/card_throw", "amplifier", 2)]},
        {"sector": 1, "ring": 4, "offset": -1, "name": "★賭場荒らし", "icon": "minecraft:gold_block", "cost": 5, "big": True, "min_level": 30,
         "effects": [S("gambler/slot_burst", "power_pct", 0.3, scale=False), S("gambler/slot_burst", "radius", 1, scale=False)]},
        {"sector": 2, "ring": 3, "offset": 0, "name": "ダブルアップ上手", "icon": "minecraft:emerald", "cost": 3, "effects": [S("gambler/double_up", "chance", 0.1)]},
        {"sector": 2, "ring": 4, "offset": -1, "name": "★勝負師の体", "icon": "minecraft:golden_chestplate", "cost": 5, "big": True, "min_level": 40,
         "effects": [A("minecraft:generic.max_health", 4), A("jsn_craft:damage_reduction", 0.03)]}]})
wj(f"{R}/tools/boards/high_roller.json", {
    "job": "high_roller", "radius": 7, "costs": [1, 1, 1, [1, 2], 2, 2, [2, 3]], "scale": [1.5, 1.5, 2, 2, 2.4, 2.4, 3],
    "center": {"name": "勝負師の心得", "icon": "jsn_craft:star_silver_cards", "cost": 1,
               "effects": [A(SKD, 0.06), A(LUCK, 1), A("minecraft:generic.max_health", 4), A("jsn_craft:max_stamina", 20)]},
    "sectors": [
        {"prefix": "luk", "pool": [
            {"name": "強運", "icon": "minecraft:rabbit_foot", "weight": 3, "effects": [A(LUCK, 0.3)]},
            {"name": "会心", "icon": "minecraft:arrow", "weight": 2, "effects": [A(CRIT, 0.03)]},
            {"name": "金貨修練", "icon": "minecraft:gold_ingot", "weight": 1, "effects": [S("high_roller/jackpot_rain", "power_pct", 0.1)]}]},
        {"prefix": "crd", "pool": [
            {"name": "鋭い札", "icon": "minecraft:paper", "weight": 2, "effects": [A("minecraft:generic.attack_damage", 0.5)]},
            {"name": "賽修練", "icon": "minecraft:bone_meal", "weight": 1, "effects": [S("high_roller/fate_dice", "power_pct", 0.1)]},
            {"name": "早業", "icon": "minecraft:clock", "weight": 2, "effects": [A(CDR, 0.04)]},
            {"name": "大技", "icon": "minecraft:blaze_powder", "weight": 2, "effects": [A(SKD, 0.04)]}]},
        {"prefix": "nrv", "pool": [
            {"name": "気力", "icon": "minecraft:bread", "weight": 2, "effects": [A("jsn_craft:max_stamina", 12)]},
            {"name": "度胸", "icon": "minecraft:apple", "weight": 2, "effects": [A("minecraft:generic.max_health", 2)]},
            {"name": "会心の一撃", "icon": "minecraft:redstone", "weight": 2, "effects": [A(CRITD, 0.08)]},
            {"name": "身軽", "icon": "minecraft:sugar", "weight": 1, "effects": [A("minecraft:generic.movement_speed", 0.005)]}]}],
    "specials": [
        {"sector": 0, "ring": 3, "offset": 0, "name": "胴元の器", "icon": "minecraft:gold_block", "cost": 3, "effects": [S("high_roller/house_rules", "radius", 3)]},
        {"sector": 0, "ring": 5, "offset": 0, "name": "★大当たり体質", "icon": "minecraft:nether_star", "cost": 5, "big": True, "min_level": 60, "effects": [A(LUCK, 3)]},
        {"sector": 0, "ring": 6, "offset": 0, "name": "★セブンス・ヘヴン・極", "icon": "minecraft:beacon", "cost": 5, "big": True, "min_level": 80,
         "effects": [S("high_roller/seven_heaven", "power_pct", 0.3, scale=False), S("high_roller/seven_heaven", "radius", 2, scale=False)]},
        {"sector": 1, "ring": 3, "offset": 0, "name": "長い雨", "icon": "minecraft:gold_nugget", "cost": 3, "effects": [S("high_roller/jackpot_rain", "duration", 2)]},
        {"sector": 1, "ring": 5, "offset": 0, "name": "★ロイヤル・極", "icon": "minecraft:diamond", "cost": 5, "big": True, "min_level": 60,
         "effects": [S("high_roller/royal_flush", "power_pct", 0.3, scale=False), S("high_roller/royal_flush", "cooldown", -15, scale=False)]},
        {"sector": 2, "ring": 3, "offset": 0, "name": "ラッキーセブン上手", "icon": "minecraft:clock", "cost": 3, "effects": [S("gambler/lucky_seven", "power", 1)]},
        {"sector": 2, "ring": 5, "offset": 0, "name": "★不死身の勝負師", "icon": "minecraft:totem_of_undying", "cost": 5, "big": True, "min_level": 60,
         "effects": [A("minecraft:generic.max_health", 6), A("jsn_craft:damage_reduction", 0.04)]}]})
print("gambler data ok")
