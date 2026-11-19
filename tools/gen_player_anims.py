"""
武器の振り・技のときの体の動き（Player Animator 用、GeckoLib 形式の JSON）を作る。
    python3 tools/gen_player_anims.py
角度は度。腕：x=-90 で前へまっすぐ、-180 で真上。y＋で右へ、z＋で右腕が外へ（左腕は z－が外）。
胴（torso）：x＋で前かがみ、y でひねり。body は体ぜんぶ（position の y＋で上へ、1=1/16ブロック）。
手に持った武器（right_item / left_item）：x＋で刃が後ろへ倒れる（振りかぶり）、x−で前へ（打ち込み）。z は腕をひねる（±90 で刃が横に寝る）。
手首は人の手で曲がる範囲（x は ±45 くらいまで）にする。曲げすぎると柄が腕にめりこんで「持てていない」ように見える。
時間は tick（1/20秒）で書く。
"""
import json, os

R0 = [-18, 0, 0]      # 右手に物を持っている、ふだんの腕
L0 = [-18, 0, 0]
Z = [0, 0, 0]
ANIMS = {}

def anim(name, length, bones, loop=False):
    out = {}
    for bone, chans in bones.items():
        b = {}
        for ch, frames in chans.items():
            d = {}
            for f in frames:
                t, v = f[0], f[1]
                ease = f[2] if len(f) > 2 else "easeInOutSine"
                d[f"{t / 20:.3f}".rstrip("0").rstrip(".") if t else "0.0"] = {"vector": v, "easing": ease}
            b[ch] = d
        out[bone] = b
    ANIMS[name] = {"animation_length": length / 20, "loop": loop, "bones": out}

def rot(*frames):
    return {"rotation": list(frames)}

# ================= 剣（3連撃） =================
# 体ごとひねって振る（body の y＋で右を向く）、一歩踏み込む（足の x−で前へ、body の z−で前へ）、
# 振り抜いたところで一瞬止める（決め）。手首（right_item）で刃を返す。
anim("sword_slash_1", 14, {  # 右から左へ、体ごと横なぎ
    "body": {"rotation": [(0, Z), (4, [0, 35, 0], "easeOutQuad"), (7, [0, -40, 0], "easeOutQuart"), (10, [0, -45, 0]), (14, Z)],
             "position": [(0, Z), (4, [0, 0, 1]), (7, [0, -1, -3], "easeOutQuart"), (10, [0, -1, -3]), (14, Z)]},
    "right_arm": rot((0, R0), (4, [-80, 75, 45], "easeOutQuad"), (7, [-85, -75, -15], "easeOutQuart"), (10, [-80, -80, -20]), (14, R0)),
    "left_arm": rot((0, L0), (4, [-30, 0, -30]), (7, [-20, 0, -45], "easeOutQuart"), (10, [-20, 0, -45]), (14, L0)),
    "right_item": rot((0, Z), (4, [25, 0, 90], "easeOutQuad"), (7, [-20, 0, 90], "easeOutQuart"), (10, [-20, 0, 90]), (14, Z)),
    "left_leg": rot((0, Z), (4, [0, 0, 0]), (7, [-28, 0, 0], "easeOutQuart"), (10, [-28, 0, 0]), (14, Z)),
    "right_leg": rot((0, Z), (7, [18, 0, 0], "easeOutQuart"), (10, [18, 0, 0]), (14, Z)),
})
anim("sword_slash_2", 15, {  # 大きく振りかぶって、斜めに斬り下ろす
    "body": {"rotation": [(0, Z), (4, [-6, 30, 0], "easeOutQuad"), (8, [8, -35, 0], "easeInQuart"), (11, [8, -38, 0]), (15, Z)],
             "position": [(0, Z), (4, [0, 1, 1]), (8, [0, -2, -4], "easeInQuart"), (11, [0, -2, -4]), (15, Z)]},
    "right_arm": rot((0, R0), (4, [-185, 40, 25], "easeOutQuad"), (8, [-30, -50, -10], "easeInQuart"), (11, [-25, -55, -12]), (15, R0)),
    "left_arm": rot((0, L0), (4, [-60, -20, -20]), (8, [-15, 10, -40], "easeInQuart"), (11, [-15, 10, -40]), (15, L0)),
    "right_item": rot((0, Z), (4, [40, 0, 0], "easeOutQuad"), (8, [-40, 0, 0], "easeInQuart"), (11, [-40, 0, 0]), (15, Z)),
    "right_leg": rot((0, Z), (8, [-30, 0, 0], "easeInQuart"), (11, [-30, 0, 0]), (15, Z)),
    "left_leg": rot((0, Z), (8, [20, 0, 0], "easeInQuart"), (11, [20, 0, 0]), (15, Z)),
})
anim("sword_slash_3", 18, {  # しめの一撃：しゃがんでから、跳び上がりながら斬り上げる
    "body": {"rotation": [(0, Z), (5, [10, -35, 0], "easeOutQuad"), (9, [-8, 45, 0], "easeOutQuart"), (13, [-8, 48, 0]), (18, Z)],
             "position": [(0, Z), (5, [0, -3, 0], "easeOutQuad"), (9, [0, 4, -3], "easeOutQuart"), (13, [0, 3, -3]), (18, Z)]},
    "right_arm": rot((0, R0), (5, [-5, -50, -20], "easeOutQuad"), (9, [-175, 45, 30], "easeOutQuart"), (13, [-178, 45, 30]), (18, R0)),
    "left_arm": rot((0, L0), (5, [-50, 10, -20]), (9, [-10, 0, -55], "easeOutQuart"), (13, [-10, 0, -55]), (18, L0)),
    "right_item": rot((0, Z), (5, [-40, 0, 0], "easeOutQuad"), (9, [30, 0, 0], "easeOutQuart"), (13, [30, 0, 0]), (18, Z)),
    "torso": rot((0, Z), (5, [15, 0, 0]), (9, [-8, 0, 0], "easeOutQuart"), (13, [-8, 0, 0]), (18, Z)),
    "right_leg": rot((0, Z), (5, [-25, 0, 0]), (9, [10, 0, 0], "easeOutQuart"), (13, [10, 0, 0]), (18, Z)),
    "left_leg": rot((0, Z), (5, [-25, 0, 0]), (9, [-20, 0, 0], "easeOutQuart"), (13, [-20, 0, 0]), (18, Z)),
})
anim("sword_skill", 22, {    # 回転斬り：ためて、腕を横に伸ばし、刃を水平に寝かせたままぐるっと一回転
    "body": {"rotation": [(0, Z), (4, [0, 45, 0], "easeOutQuad"), (13, [0, -330, 0], "easeOutQuart"), (17, [0, -345, 0]), (22, [0, -360, 0])],
             "position": [(0, Z), (4, [0, -2, 0]), (13, [0, -1, 0]), (17, [0, -1, 0]), (22, Z)]},
    "right_arm": rot((0, R0), (4, [-85, 80, 0], "easeOutQuad"), (13, [-90, 75, 0]), (17, [-90, 75, 0]), (22, R0)),
    "left_arm": rot((0, L0), (4, [-40, 0, -50]), (13, [-20, 0, -70]), (17, [-20, 0, -70]), (22, L0)),
    "right_item": rot((0, Z), (4, [10, 0, 90]), (13, [0, 0, 90]), (17, [0, 0, 90]), (22, Z)),
    "right_leg": rot((0, Z), (4, [-20, 0, 15]), (13, [-15, 0, 15]), (17, [-15, 0, 15]), (22, Z)),
    "left_leg": rot((0, Z), (4, [15, 0, -15]), (13, [15, 0, -15]), (17, [15, 0, -15]), (22, Z)),
})

# ================= 斧 =================
anim("axe_chop_1", 12, {     # 両手で振りかぶって叩きつける
    "right_arm": rot((0, R0), (4, [-178, 5, 8], "easeOutQuad"), (7, [-35, 0, 0], "easeInQuart"), (12, R0)),
    "left_arm": rot((0, L0), (4, [-172, -15, -8], "easeOutQuad"), (7, [-40, -25, 0], "easeInQuart"), (12, L0)),
    "torso": rot((0, Z), (4, [-10, 0, 0]), (7, [22, 0, 0], "easeInQuart"), (12, Z)),
    "right_item": rot((0, Z), (4, [75, 0, 0], "easeOutQuad"), (7, [-35, 0, 0], "easeInQuart"), (12, Z)),
})
anim("axe_chop_2", 12, {     # 横に大きくなぎ払う
    "right_arm": rot((0, R0), (4, [-85, 70, 25], "easeOutQuad"), (7, [-88, -75, -10], "easeInQuart"), (12, R0)),
    "left_arm": rot((0, L0), (4, [-80, 45, -5], "easeOutQuad"), (7, [-85, -85, -20], "easeInQuart"), (12, L0)),
    "torso": rot((0, Z), (4, [0, 22, 0]), (7, [6, -28, 0], "easeInQuart"), (12, Z)),
    "right_item": rot((0, Z), (4, [30, 0, 0], "easeOutQuad"), (7, [-25, 0, 0], "easeInQuart"), (12, Z)),
})
anim("axe_skill", 17, {      # 跳び上がって叩きつける
    "body": {"position": [(0, Z), (6, [0, 7, 0], "easeOutQuad"), (10, Z, "easeInQuad"), (17, Z)]},
    "right_arm": rot((0, R0), (6, [-185, 5, 8], "easeOutQuad"), (10, [-30, 0, 0], "easeInQuart"), (17, R0)),
    "left_arm": rot((0, L0), (6, [-180, -10, -8], "easeOutQuad"), (10, [-35, -20, 0], "easeInQuart"), (17, L0)),
    "torso": rot((0, Z), (6, [-12, 0, 0]), (10, [28, 0, 0], "easeInQuart"), (17, Z)),
    "right_leg": rot((0, Z), (6, [-30, 0, 0]), (10, [0, 0, 0]), (17, Z)),
    "left_leg": rot((0, Z), (6, [-10, 0, 0]), (10, [0, 0, 0]), (17, Z)),
    "right_item": rot((0, Z), (6, [80, 0, 0], "easeOutQuad"), (10, [-40, 0, 0], "easeInQuart"), (17, Z)),
})

# ================= 槍 =================
anim("spear_thrust", 10, {   # 引いて突く
    "right_arm": rot((0, R0), (3, [-70, 20, 8], "easeOutQuad"), (5, [-95, -5, 0], "easeOutExpo"), (10, R0)),
    "left_arm": rot((0, L0), (3, [-65, -30, -5]), (5, [-88, -40, 0], "easeOutExpo"), (10, L0)),
    "torso": rot((0, Z), (3, [0, 22, 0]), (5, [8, -18, 0], "easeOutExpo"), (10, Z)),
})
anim("spear_sweep", 12, {    # 柄で横になぎ払う
    "right_arm": rot((0, R0), (4, [-60, 65, 25], "easeOutQuad"), (7, [-70, -70, -10], "easeInQuart"), (12, R0)),
    "left_arm": rot((0, L0), (4, [-55, 40, -5]), (7, [-65, -80, -15], "easeInQuart"), (12, L0)),
    "torso": rot((0, Z), (4, [0, 22, 0]), (7, [5, -25, 0], "easeInQuart"), (12, Z)),
})
anim("spear_skill", 18, {    # くるっと回して突き出す
    "body": rot((0, Z), (9, [0, 360, 0], "easeInOutQuad"), (18, [0, 360, 0])),
    "right_arm": rot((0, R0), (3, [-100, 50, 30]), (9, [-100, 50, 30]), (11, [-70, 20, 8]), (13, [-98, -5, 0], "easeOutExpo"), (18, R0)),
    "left_arm": rot((0, L0), (3, [-40, 0, -40]), (9, [-40, 0, -40]), (11, [-65, -30, -5]), (13, [-90, -40, 0], "easeOutExpo"), (18, L0)),
    "torso": rot((0, Z), (11, [0, 22, 0]), (13, [10, -18, 0], "easeOutExpo"), (18, Z)),
})

# ================= 短剣（逆手の二刀流） =================
anim("dagger_right", 7, {
    "right_arm": rot((0, R0), (2, [-105, 45, 35], "easeOutQuad"), (4, [-60, -55, -10], "easeOutExpo"), (7, R0)),
    "torso": rot((0, Z), (2, [0, 12, 0]), (4, [5, -15, 0], "easeOutExpo"), (7, Z)),
    "right_item": rot((0, Z), (2, [40, 0, 0], "easeOutQuad"), (4, [-30, 0, 0], "easeOutExpo"), (7, Z)),
})
anim("dagger_left", 7, {
    "left_arm": rot((0, L0), (2, [-105, -45, -35], "easeOutQuad"), (4, [-60, 55, 10], "easeOutExpo"), (7, L0)),
    "torso": rot((0, Z), (2, [0, -12, 0]), (4, [5, 15, 0], "easeOutExpo"), (7, Z)),
    "left_item": rot((0, Z), (2, [40, 0, 0], "easeOutQuad"), (4, [-30, 0, 0], "easeOutExpo"), (7, Z)),
})
anim("dagger_skill", 12, {   # 両手を交差させて、×の字に斬る
    "right_arm": rot((0, R0), (3, [-155, -30, -20], "easeOutQuad"), (6, [-45, 50, 35], "easeOutExpo"), (12, R0)),
    "left_arm": rot((0, L0), (3, [-155, 30, 20], "easeOutQuad"), (6, [-45, -50, -35], "easeOutExpo"), (12, L0)),
    "torso": rot((0, Z), (3, [-8, 0, 0]), (6, [15, 0, 0], "easeOutExpo"), (12, Z)),
})

# ================= 拳 =================
anim("fist_right", 6, {
    "right_arm": rot((0, R0), (1, [-55, 20, 5], "easeOutQuad"), (3, [-95, -12, 0], "easeOutExpo"), (6, R0)),
    "torso": rot((0, Z), (1, [0, 8, 0]), (3, [4, -18, 0], "easeOutExpo"), (6, Z)),
})
anim("fist_left", 6, {
    "left_arm": rot((0, L0), (1, [-55, -20, -5], "easeOutQuad"), (3, [-95, 12, 0], "easeOutExpo"), (6, L0)),
    "torso": rot((0, Z), (1, [0, -8, 0]), (3, [4, 18, 0], "easeOutExpo"), (6, Z)),
})
anim("fist_skill", 12, {     # しゃがんでアッパー
    "body": {"position": [(0, Z), (3, [0, -2, 0]), (6, [0, 5, 0], "easeOutExpo"), (12, Z)]},
    "right_arm": rot((0, R0), (3, [-15, 0, 20], "easeOutQuad"), (6, [-178, -10, 0], "easeOutExpo"), (12, R0)),
    "left_arm": rot((0, L0), (3, [-70, 20, -10]), (6, [-30, 0, -20]), (12, L0)),
    "torso": rot((0, Z), (3, [18, 12, 0]), (6, [-12, -12, 0], "easeOutExpo"), (12, Z)),
})

# ================= 鞭 =================
anim("whip_crack", 10, {     # 振りかぶって前へ打ちつける
    "right_arm": rot((0, R0), (3, [-195, 25, 15], "easeOutQuad"), (5, [-70, -10, 0], "easeOutExpo"), (10, R0)),
    "torso": rot((0, Z), (3, [-8, 12, 0]), (5, [10, -12, 0], "easeOutExpo"), (10, Z)),
    "right_item": rot((0, Z), (3, [85, 0, 0], "easeOutQuad"), (5, [-45, 0, 0], "easeOutExpo"), (10, Z)),
})
anim("whip_skill", 16, {     # 頭の上で回してから打つ
    "right_arm": rot((0, R0), (2, [-170, 30, 10]), (4, [-170, -30, -5]), (6, [-170, 30, 10]), (8, [-170, -30, -5]),
                     (11, [-65, -10, 0], "easeOutExpo"), (16, R0)),
    "torso": rot((0, Z), (8, [-6, 0, 0]), (11, [12, -10, 0], "easeOutExpo"), (16, Z)),
    "right_item": rot((0, Z), (8, [60, 0, 0]), (11, [-45, 0, 0], "easeOutExpo"), (16, Z)),
})

# ================= 杖・魔法 =================
anim("staff_strike", 10, {
    "right_arm": rot((0, R0), (3, [-160, 15, 10], "easeOutQuad"), (6, [-50, -20, 0], "easeInQuad"), (10, R0)),
    "torso": rot((0, Z), (3, [-5, 5, 0]), (6, [10, -8, 0], "easeInQuad"), (10, Z)),
    "right_item": rot((0, Z), (3, [55, 0, 0], "easeOutQuad"), (6, [-25, 0, 0], "easeInQuad"), (10, Z)),
})
anim("cast", 14, {           # 杖を掲げてから前へ突き出す
    "right_arm": rot((0, R0), (3, [-150, 0, 10], "easeOutQuad"), (8, [-150, 0, 10]), (10, [-95, 0, 0], "easeOutExpo"), (14, R0)),
    "left_arm": rot((0, L0), (3, [-80, 20, -15], "easeOutQuad"), (8, [-80, 20, -15]), (10, [-90, 10, -5], "easeOutExpo"), (14, L0)),
    "torso": rot((0, Z), (3, [-5, 0, 0]), (10, [6, 0, 0], "easeOutExpo"), (14, Z)),
})
anim("cast_charge", 60, {    # 詠唱中：杖を掲げ、左手をかざす
    "right_arm": rot((0, R0), (5, [-155, 5, 12], "easeOutQuad"), (30, [-150, -5, 12]), (60, [-155, 5, 12])),
    "left_arm": rot((0, L0), (5, [-75, 35, -20], "easeOutQuad"), (30, [-70, 30, -22]), (60, [-75, 35, -20])),
    "torso": rot((0, Z), (5, [-4, 0, 0]), (60, [-4, 0, 0])),
})

# ================= 盾 =================
anim("shield_bash", 10, {    # 両腕で前へ押し出す
    "right_arm": rot((0, R0), (3, [-60, 10, 10], "easeOutQuad"), (5, [-95, -10, 0], "easeOutExpo"), (10, R0)),
    "left_arm": rot((0, L0), (3, [-60, -10, -10], "easeOutQuad"), (5, [-95, 15, 0], "easeOutExpo"), (10, L0)),
    "torso": rot((0, Z), (3, [-6, 0, 0]), (5, [15, 0, 0], "easeOutExpo"), (10, Z)),
})

if __name__ == "__main__":
    out = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsn_craft/player_animations/weapons.json")
    with open(out, "w") as f:
        json.dump({"format_version": "1.8.0", "animations": ANIMS}, f, indent=1)
    print(len(ANIMS), "animations:", ", ".join(ANIMS))
