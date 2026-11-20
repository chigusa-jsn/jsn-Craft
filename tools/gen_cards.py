"""カード：一覧（CardList.java）・絵・モデル・バインダーの枠・名前など"""
import json, os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..")
R = f"{ROOT}/src/main/resources"; A = f"{R}/assets/jsn_craft"; D = f"{R}/data/jsn_craft"
J = f"{ROOT}/src/main/java/com/chigusa/jsncraft/card"
def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def save(img, path): os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: json.dump(obj, f, ensure_ascii=False, indent=2); f.write("\n")

# (モンスター, 能力)
CARDS = [
 # ノーマル
 [("chicken", "speed"), ("pig", "health"), ("cow", "health"), ("sheep", "armor"), ("rabbit", "speed"), ("cod", "stamina"), ("salmon", "stamina"),
  ("squid", "mana"), ("bat", "crit"), ("zombie", "attack"), ("skeleton", "crit"), ("spider", "speed"), ("creeper", "attack"), ("slime", "regen"),
  ("husk", "armor"), ("drowned", "mana"), ("silverfish", "fortune"), ("frog", "luck"), ("bee", "steal"), ("fox", "luck")],
 # レア
 [("wolf", "attack"), ("cat", "luck"), ("parrot", "xp"), ("turtle", "armor"), ("axolotl", "regen"), ("goat", "kbres"), ("llama", "stamina"),
  ("horse", "speed"), ("enderman", "critdmg"), ("witch", "magic"), ("pillager", "crit"), ("blaze", "skill"), ("magma_cube", "guard"),
  ("ghast", "magic"), ("phantom", "cdr"), ("guardian", "heal")],
 # スーパーレア
 [("iron_golem", "guard"), ("snow_golem", "heal"), ("villager", "xp"), ("piglin_brute", "attack"), ("evoker", "magic"), ("vindicator", "critdmg"),
  ("ravager", "health"), ("shulker", "armor"), ("allay", "luck"), ("sniffer", "fortune"), ("strider", "stamina"), ("breeze", "speed")],
 # ウルトラレア
 [("wither_skeleton", "steal"), ("elder_guardian", "heal"), ("mooshroom", "regen"), ("panda", "luck"), ("polar_bear", "health"),
  ("glow_squid", "mana"), ("hoglin", "boss"), ("piglin", "fortune")],
 # レジェンド
 [("warden", "guard"), ("ender_dragon", "boss"), ("wither", "skill"), ("illusioner", "cdr")],
]
lines = []
for r, lst in enumerate(CARDS):
    for mob, stat in lst:
        lines.append(f'            new Card("{mob}", "minecraft:{mob}", {r}, "{stat}")')
java = f'''package com.chigusa.jsncraft.card;

import java.util.List;

/** カードの一覧（tools/gen_cards.py で作る。手で直さない） */
public final class CardList {{
    public record Card(String id, String entity, int rarity, String stat) {{}}

    public static final List<Card> ALL = List.of(
{",\n".join(lines)});

    private CardList() {{}}
}}
'''
os.makedirs(J, exist_ok=True)
open(f"{J}/CardList.java", "w", encoding="utf-8").write(java)

# ---------- 絵 ----------
FR = ["#b0b0b0", "#50a0f0", "#c060f0", "#f0b030", "#ff5070"]
for r, c in enumerate(FR):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle([3, 1, 12, 14], fill=hx(c)); d.rectangle([4, 2, 11, 13], fill=hx("#20202a"))
    d.rectangle([5, 3, 10, 9], fill=hx("#4a6a8a")); d.rectangle([6, 5, 9, 9], fill=hx("#303040"))
    d.rectangle([5, 11, 10, 12], fill=hx("#f0f0f0") if r < 3 else hx(c))
    if r >= 3: d.point([(4, 2), (11, 13)], fill=hx("#ffffff"))
    save(im, f"{A}/textures/item/card_{r}.png")
    wj(f"{A}/models/item/card_{r}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"jsn_craft:item/card_{r}"}})
wj(f"{A}/models/item/card.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "jsn_craft:item/card_0"},
    "overrides": [{"predicate": {"jsn_craft:card_rarity": r / 4}, "model": f"jsn_craft:item/card_{r}"} for r in range(1, 5)]})
# バインダー
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
d.rectangle([2, 1, 13, 14], fill=hx("#3a2a6a")); d.rectangle([2, 1, 4, 14], fill=hx("#2a1a4a"))
for y in (3, 7, 11): d.rectangle([2, y, 4, y + 1], fill=hx("#c0c0c8"))
d.rectangle([6, 3, 11, 8], fill=hx("#f0b030")); d.rectangle([7, 4, 10, 7], fill=hx("#20202a")); d.rectangle([6, 10, 11, 12], fill=hx("#9080c0"))
save(im, f"{A}/textures/item/card_binder.png")
wj(f"{A}/models/item/card_binder.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "jsn_craft:item/card_binder"}})
# アルバム
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
d.rectangle([1, 2, 14, 13], fill=hx("#8a3a2a")); d.rectangle([7, 2, 8, 13], fill=hx("#5a2a1a"))
d.rectangle([2, 4, 6, 8], fill=hx("#f0e0c0")); d.rectangle([9, 4, 13, 8], fill=hx("#f0e0c0"))
d.rectangle([2, 9, 6, 12], fill=hx("#50a0f0")); d.rectangle([9, 9, 13, 12], fill=hx("#ff5070")); d.point((12, 3), fill=hx("#f0c040"))
save(im, f"{A}/textures/item/card_album.png")
wj(f"{A}/models/item/card_album.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "jsn_craft:item/card_album"}})
# Curios の枠のアイコン
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
d.rectangle([4, 2, 11, 13], outline=(255, 255, 255, 90)); d.rectangle([6, 4, 9, 8], outline=(255, 255, 255, 70))
save(im, f"{A}/textures/slot/card_binder.png")
wj(f"{D}/curios/slots/card_binder.json", {"size": 1, "order": 160, "icon": "jsn_craft:slot/card_binder"})
p = f"{D}/curios/entities/jsn_craft.json"
ent = json.load(open(p)) if os.path.exists(p) else None
if ent is None:
    import glob
    p = glob.glob(f"{D}/curios/entities/*.json")[0]; ent = json.load(open(p))
if "card_binder" not in ent["slots"]: ent["slots"].append("card_binder")
wj(p, ent)
wj(f"{R}/data/curios/tags/item/card_binder.json", {"values": ["jsn_craft:card_binder"]})

# カードマシン（ブロック）
def tex(name, draw):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0)); draw(ImageDraw.Draw(im)); save(im, f"{A}/textures/block/gacha/{name}.png")
def cm_front(d):
    d.rectangle([0, 0, 31, 31], fill=hx("#2a3a8a")); d.rectangle([0, 0, 31, 2], fill=hx("#5a7af0"))
    d.rectangle([3, 4, 28, 9], fill=hx("#f0f0ff")); d.text((5, 3), "CARDS", fill=hx("#2a3a8a"))
    for i, c in enumerate(FR):
        x = 3 + (i % 3) * 9; y = 12 + (i // 3) * 9
        d.rectangle([x, y, x + 6, y + 7], fill=hx(c)); d.rectangle([x + 1, y + 1, x + 5, y + 6], fill=hx("#20202a"))
    d.rectangle([22, 21, 28, 24], fill=hx("#f0d040")); d.rectangle([4, 27, 27, 30], fill=hx("#101020"))
def cm_side(d):
    d.rectangle([0, 0, 31, 31], fill=hx("#2a3a8a")); d.rectangle([0, 0, 31, 31], outline=hx("#5a7af0"), width=2)
tex("card_front", cm_front); tex("card_side", cm_side)
def face(t): return {"texture": "#" + t, "uv": [0, 0, 16, 16]}
wj(f"{A}/models/block/card_machine.json", {"parent": "minecraft:block/block", "textures": {"front": "jsn_craft:block/gacha/card_front", "side": "jsn_craft:block/gacha/card_side",
    "particle": "jsn_craft:block/gacha/card_side"}, "elements": [{"from": [1, 0, 2], "to": [15, 16, 15], "faces": {"north": face("front"), "south": face("side"),
    "east": face("side"), "west": face("side"), "up": face("side"), "down": face("side")}}]})
wj(f"{A}/blockstates/card_machine.json", {"variants": {f"facing={f}": ({"model": "jsn_craft:block/card_machine"} | ({"y": y} if y else {}))
                                                       for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
wj(f"{A}/models/item/card_machine.json", {"parent": "jsn_craft:block/card_machine"})
wj(f"{D}/loot_table/blocks/card_machine.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "jsn_craft:card_machine"}],
                                                                                    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
wj(f"{D}/recipe/card_machine.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PPP", "BCB", "III"],
    "key": {"P": {"item": "minecraft:paper"}, "B": {"item": "minecraft:blue_dye"}, "C": {"item": "minecraft:chest"}, "I": {"item": "minecraft:iron_ingot"}},
    "result": {"id": "jsn_craft:card_machine", "count": 1}})
wj(f"{D}/recipe/card_binder.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["LGL", "LPL", "LGL"],
    "key": {"L": {"item": "minecraft:leather"}, "G": {"item": "minecraft:gold_ingot"}, "P": {"item": "minecraft:paper"}}, "result": {"id": "jsn_craft:card_binder", "count": 1}})
wj(f"{D}/recipe/card_album.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["LPL", "PBP", "LPL"],
    "key": {"L": {"item": "minecraft:leather"}, "P": {"item": "minecraft:paper"}, "B": {"item": "minecraft:book"}}, "result": {"id": "jsn_craft:card_album", "count": 1}})
pk = f"{R}/data/minecraft/tags/block/mineable/pickaxe.json"; dd = json.load(open(pk, encoding="utf-8"))
if "jsn_craft:card_machine" not in dd["values"]: dd["values"].append("jsn_craft:card_machine")
wj(pk, dd)
NAMES = {"ja_jp": {"block.jsn_craft.card_machine": "カードマシン", "item.jsn_craft.card": "カード", "item.jsn_craft.card_binder": "装備用バインダー",
                   "item.jsn_craft.card_album": "カードアルバム", "curios.identifier.card_binder": "カードバインダー"},
         "en_us": {"block.jsn_craft.card_machine": "Card Machine", "item.jsn_craft.card": "Card", "item.jsn_craft.card_binder": "Card Binder",
                   "item.jsn_craft.card_album": "Card Album", "curios.identifier.card_binder": "Card Binder"}}
for lang, add in NAMES.items():
    p = f"{A}/lang/{lang}.json"; dd = json.load(open(p, encoding="utf-8")); dd.update(add); wj(p, dd)
print("cards", sum(len(x) for x in CARDS))
