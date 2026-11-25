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

# ================= 剣の技（技ごとの動き） =================
anim("sword_sweep", 15, {    # 斬り払い：低くかまえて、体ごと大きくなぎ払う
    "body": {"rotation": [(0, Z), (4, [0, 50, 0], "easeOutQuad"), (8, [0, -60, 0], "easeOutQuart"), (11, [0, -62, 0]), (15, Z)],
             "position": [(0, Z), (4, [0, -2, 1]), (8, [0, -3, -3], "easeOutQuart"), (11, [0, -3, -3]), (15, Z)]},
    "right_arm": rot((0, R0), (4, [-75, 80, 30], "easeOutQuad"), (8, [-80, -80, -10], "easeOutQuart"), (11, [-78, -82, -12]), (15, R0)),
    "left_arm": rot((0, L0), (4, [-30, 0, -30]), (8, [-20, 0, -55], "easeOutQuart"), (11, [-20, 0, -55]), (15, L0)),
    "right_item": rot((0, Z), (4, [20, 0, 90], "easeOutQuad"), (8, [-15, 0, 90], "easeOutQuart"), (11, [-15, 0, 90]), (15, Z)),
    "left_leg": rot((0, Z), (8, [-35, 0, -5], "easeOutQuart"), (11, [-35, 0, -5]), (15, Z)),
    "right_leg": rot((0, Z), (8, [25, 0, 5], "easeOutQuart"), (11, [25, 0, 5]), (15, Z)),
})
anim("sword_lunge", 12, {    # 踏み込み：前へ低く飛び込む（剣は後ろに引いて構える）
    "body": {"rotation": [(0, Z), (3, [15, 0, 0], "easeOutQuad"), (8, [15, 0, 0]), (12, Z)],
             "position": [(0, Z), (3, [0, -3, -3], "easeOutQuad"), (8, [0, -3, -3]), (12, Z)]},
    "right_arm": rot((0, R0), (3, [-35, 35, 25], "easeOutQuad"), (8, [-35, 35, 25]), (12, R0)),
    "left_arm": rot((0, L0), (3, [-70, -10, -10], "easeOutQuad"), (8, [-70, -10, -10]), (12, L0)),
    "right_item": rot((0, Z), (3, [-35, 0, 0]), (8, [-35, 0, 0]), (12, Z)),
    "left_leg": rot((0, Z), (3, [-45, 0, 0], "easeOutQuad"), (8, [-45, 0, 0]), (12, Z)),
    "right_leg": rot((0, Z), (3, [40, 0, 0], "easeOutQuad"), (8, [40, 0, 0]), (12, Z)),
})
anim("sword_whirl", 22, {    # 旋風斬：腕と刃を横に伸ばして、3回まわる（1回ごとに斬る）
    "body": {"rotation": [(0, Z), (3, [0, 40, 0], "easeOutQuad"), (18, [0, -1040, 0], "linear"), (22, [0, -1080, 0], "easeOutQuad")],
             "position": [(0, Z), (3, [0, -2, 0]), (18, [0, -2, 0]), (22, Z)]},
    "right_arm": rot((0, R0), (3, [-85, 80, 0], "easeOutQuad"), (18, [-90, 75, 0]), (22, R0)),
    "left_arm": rot((0, L0), (3, [-30, 0, -60]), (18, [-30, 0, -60]), (22, L0)),
    "right_item": rot((0, Z), (3, [10, 0, 90]), (18, [0, 0, 90]), (22, Z)),
    "right_leg": rot((0, Z), (3, [-20, 0, 15]), (18, [-20, 0, 15]), (22, Z)),
    "left_leg": rot((0, Z), (3, [15, 0, -15]), (18, [15, 0, -15]), (22, Z)),
})
anim("sword_parry", 22, {    # 受け流し：剣を横にして前にかまえる（1秒）
    "body": {"position": [(0, Z), (3, [0, -2, 1], "easeOutQuad"), (18, [0, -2, 1]), (22, Z)]},
    "right_arm": rot((0, R0), (3, [-100, -40, 0], "easeOutQuad"), (18, [-100, -40, 0]), (22, R0)),
    "left_arm": rot((0, L0), (3, [-85, 30, 0], "easeOutQuad"), (18, [-85, 30, 0]), (22, L0)),
    "right_item": rot((0, Z), (3, [0, 0, -90], "easeOutQuad"), (18, [0, 0, -90]), (22, Z)),
    "right_leg": rot((0, Z), (3, [15, 0, 8]), (18, [15, 0, 8]), (22, Z)),
    "left_leg": rot((0, Z), (3, [-15, 0, -8]), (18, [-15, 0, -8]), (22, Z)),
})
anim("sword_iai_ready", 12, {  # 居合のため：腰を落とし、左の腰の柄に手をかける
    "body": {"rotation": [(0, Z), (5, [10, -25, 0], "easeOutQuad"), (12, [10, -25, 0])],
             "position": [(0, Z), (5, [0, -4, 0], "easeOutQuad"), (12, [0, -4, 0])]},
    "right_arm": rot((0, R0), (5, [-35, -60, -20], "easeOutQuad"), (12, [-35, -60, -20])),
    "left_arm": rot((0, L0), (5, [-15, 25, -10], "easeOutQuad"), (12, [-15, 25, -10])),
    "right_item": rot((0, Z), (5, [-20, 0, -90], "easeOutQuad"), (12, [-20, 0, -90])),
    "left_leg": rot((0, Z), (5, [-30, 0, -10], "easeOutQuad"), (12, [-30, 0, -10])),
    "right_leg": rot((0, Z), (5, [30, 0, 10], "easeOutQuad"), (12, [30, 0, 10])),
})
anim("sword_iai", 14, {      # 居合・一閃：駆け抜けながら、一気に抜いて横に斬る
    "body": {"rotation": [(0, [10, -25, 0]), (3, [5, 45, 0], "easeOutExpo"), (10, [5, 50, 0]), (14, Z)],
             "position": [(0, [0, -4, 0]), (3, [0, -3, -2], "easeOutExpo"), (10, [0, -3, -2]), (14, Z)]},
    "right_arm": rot((0, [-35, -60, -20]), (3, [-90, 95, 20], "easeOutExpo"), (10, [-90, 95, 20]), (14, R0)),
    "left_arm": rot((0, [-15, 25, -10]), (3, [-20, 30, -20], "easeOutExpo"), (10, [-20, 30, -20]), (14, L0)),
    "right_item": rot((0, [-20, 0, -90]), (3, [-10, 0, 90], "easeOutExpo"), (10, [-10, 0, 90]), (14, Z)),
    "left_leg": rot((0, [-30, 0, -10]), (3, [-35, 0, 0], "easeOutExpo"), (10, [-35, 0, 0]), (14, Z)),
    "right_leg": rot((0, [30, 0, 10]), (3, [30, 0, 0], "easeOutExpo"), (10, [30, 0, 0]), (14, Z)),
})
anim("sword_flash_ready", 10, {  # 蒼閃のため：剣を肩の後ろへ大きく振りかぶる
    "body": {"rotation": [(0, Z), (5, [-8, 30, 0], "easeOutQuad"), (10, [-8, 30, 0])]},
    "right_arm": rot((0, R0), (5, [-190, 35, 20], "easeOutQuad"), (10, [-190, 35, 20])),
    "left_arm": rot((0, L0), (5, [-90, 15, -10], "easeOutQuad"), (10, [-90, 15, -10])),
    "right_item": rot((0, Z), (5, [40, 0, 0], "easeOutQuad"), (10, [40, 0, 0])),
    "left_leg": rot((0, Z), (5, [-20, 0, 0]), (10, [-20, 0, 0])),
    "right_leg": rot((0, Z), (5, [15, 0, 0]), (10, [15, 0, 0])),
})
anim("sword_flash", 16, {    # 奥義・蒼閃：渾身の振り下ろしで、三日月の斬撃を飛ばす
    "body": {"rotation": [(0, [-8, 30, 0]), (4, [12, -40, 0], "easeInQuart"), (11, [12, -42, 0]), (16, Z)],
             "position": [(0, Z), (4, [0, -3, -4], "easeInQuart"), (11, [0, -3, -4]), (16, Z)]},
    "right_arm": rot((0, [-190, 35, 20]), (4, [-20, -55, -10], "easeInQuart"), (11, [-18, -58, -12]), (16, R0)),
    "left_arm": rot((0, [-90, 15, -10]), (4, [-10, 10, -50], "easeInQuart"), (11, [-10, 10, -50]), (16, L0)),
    "right_item": rot((0, [40, 0, 0]), (4, [-40, 0, 0], "easeInQuart"), (11, [-40, 0, 0]), (16, Z)),
    "right_leg": rot((0, [15, 0, 0]), (4, [-35, 0, 0], "easeInQuart"), (11, [-35, 0, 0]), (16, Z)),
    "left_leg": rot((0, [-20, 0, 0]), (4, [25, 0, 0], "easeInQuart"), (11, [25, 0, 0]), (16, Z)),
})
anim("sword_leap", 26, {     # 天翔蒼刃：しゃがんで跳び、空中で振りかぶって、着地で叩きつける
    "body": {"rotation": [(0, Z), (3, [15, 0, 0]), (8, [-10, 0, 0], "easeOutQuad"), (15, [-12, 0, 0]), (18, [30, 0, 0], "easeInQuart"), (22, [25, 0, 0]), (26, Z)],
             "position": [(0, Z), (3, [0, -3, 0]), (8, Z, "easeOutQuad"), (18, [0, -4, -2], "easeInQuart"), (22, [0, -4, -2]), (26, Z)]},
    "right_arm": rot((0, R0), (3, [-20, 20, 20]), (8, [-185, 10, 10], "easeOutQuad"), (15, [-190, 10, 10]), (18, [-25, 0, 0], "easeInQuart"), (22, [-25, 0, 0]), (26, R0)),
    "left_arm": rot((0, L0), (3, [-20, 0, -20]), (8, [-40, 0, -60]), (15, [-40, 0, -60]), (18, [-20, 0, -45], "easeInQuart"), (22, [-20, 0, -45]), (26, L0)),
    "right_item": rot((0, Z), (8, [40, 0, 0], "easeOutQuad"), (15, [45, 0, 0]), (18, [-45, 0, 0], "easeInQuart"), (22, [-45, 0, 0]), (26, Z)),
    "right_leg": rot((0, Z), (3, [-30, 0, 0]), (8, [-50, 0, 0]), (15, [-50, 0, 0]), (18, [-40, 0, 0]), (22, [-40, 0, 0]), (26, Z)),
    "left_leg": rot((0, Z), (3, [-30, 0, 0]), (8, [20, 0, 0]), (15, [20, 0, 0]), (18, [10, 0, 0]), (22, [10, 0, 0]), (26, Z)),
})
_fl_arm = [(0, R0)]; _fl_body = [(0, Z)]; _fl_item = [(0, Z)]
for i in range(7):   # 千刃乱舞：3tick ごとに左右へ細かく斬る
    t = 2 + i * 3
    side = 1 if i % 2 == 0 else -1
    _fl_arm.append((t, [-85 + (i % 3) * 8, 65 * side, 25 * side], "easeOutQuart"))
    _fl_body.append((t, [0, 22 * side, 0], "easeOutQuart"))
    _fl_item.append((t, [-15 * side, 0, 90], "easeOutQuart"))
_fl_arm += [(23, [-190, 30, 20], "easeOutQuad"), (26, [-25, -50, -10], "easeInQuart"), (30, [-25, -52, -10]), (34, R0)]
_fl_body += [(23, [-8, 25, 0], "easeOutQuad"), (26, [15, -35, 0], "easeInQuart"), (30, [15, -36, 0]), (34, Z)]
_fl_item += [(23, [45, 0, 0], "easeOutQuad"), (26, [-45, 0, 0], "easeInQuart"), (30, [-45, 0, 0]), (34, Z)]
anim("sword_flurry", 34, {   # 千刃乱舞：左右に7回細かく斬って、最後は大振り
    "body": {"rotation": _fl_body, "position": [(0, Z), (2, [0, -2, -1]), (23, [0, -1, -1]), (26, [0, -3, -4], "easeInQuart"), (30, [0, -3, -4]), (34, Z)]},
    "right_arm": {"rotation": _fl_arm},
    "right_item": {"rotation": _fl_item},
    "left_arm": rot((0, L0), (2, [-40, 0, -30]), (26, [-15, 10, -50]), (30, [-15, 10, -50]), (34, L0)),
    "left_leg": rot((0, Z), (2, [-25, 0, 0]), (26, [25, 0, 0]), (30, [25, 0, 0]), (34, Z)),
    "right_leg": rot((0, Z), (2, [20, 0, 0]), (26, [-35, 0, 0]), (30, [-35, 0, 0]), (34, Z)),
})
anim("sword_moon_ready", 22, {  # 天穹蒼月のため：剣を空へ掲げ、空を見上げる
    "body": {"rotation": [(0, Z), (6, [-8, 0, 0], "easeOutQuad"), (22, [-8, 0, 0])]},
    "head": rot((0, Z), (6, [-30, 0, 0], "easeOutQuad"), (22, [-30, 0, 0])),
    "right_arm": rot((0, R0), (6, [-150, 0, 10], "easeOutQuad"), (22, [-152, 0, 12])),
    "left_arm": rot((0, L0), (6, [-20, 0, -65], "easeOutQuad"), (22, [-22, 0, -68])),
    "right_item": rot((0, Z), (6, [-45, 0, 0], "easeOutQuad"), (22, [-45, 0, 0])),
    "right_leg": rot((0, Z), (6, [10, 0, 8]), (22, [10, 0, 8])),
    "left_leg": rot((0, Z), (6, [-10, 0, -8]), (22, [-10, 0, -8])),
})
anim("sword_moon", 16, {     # 奥義・天穹蒼月：掲げた剣を、月を落とす先へ振り下ろす
    "body": {"rotation": [(0, [-8, 0, 0]), (4, [18, 0, 0], "easeInQuart"), (11, [18, 0, 0]), (16, Z)],
             "position": [(0, Z), (4, [0, -3, -3], "easeInQuart"), (11, [0, -3, -3]), (16, Z)]},
    "head": rot((0, [-30, 0, 0]), (4, [5, 0, 0]), (16, Z)),
    "right_arm": rot((0, [-152, 0, 12]), (4, [-75, 0, 0], "easeInQuart"), (11, [-75, 0, 0]), (16, R0)),
    "left_arm": rot((0, [-22, 0, -68]), (4, [-10, 0, -40], "easeInQuart"), (11, [-10, 0, -40]), (16, L0)),
    "right_item": rot((0, [-45, 0, 0]), (4, [-30, 0, 0], "easeInQuart"), (11, [-30, 0, 0]), (16, Z)),
    "right_leg": rot((0, [10, 0, 8]), (4, [-30, 0, 0], "easeInQuart"), (11, [-30, 0, 0]), (16, Z)),
    "left_leg": rot((0, [-10, 0, -8]), (4, [25, 0, 0], "easeInQuart"), (11, [25, 0, 0]), (16, Z)),
})

# ================= 刀（両手持ち） =================
KR = [-55, -30, 0]   # 刀のかまえ：右手は鍔の下
KL = [-45, 35, 0]    # 左手は柄の下（両手が胸の前で合う）
KI = [-15, 0, 0]
anim("katana_hold", 40, {    # 刀を持っているあいだのかまえ（ずっと）
    "right_arm": rot((0, KR), (20, [-57, -30, 0]), (40, KR)),
    "left_arm": rot((0, KL), (20, [-47, 35, 0]), (40, KL)),
    "right_item": rot((0, KI), (40, KI)),
}, loop=True)
anim("katana_slash_1", 15, {  # 袈裟斬り：右上から左下へ斜めに
    "body": {"rotation": [(0, Z), (4, [-5, 25, 0], "easeOutQuad"), (8, [10, -30, 0], "easeInQuart"), (11, [10, -32, 0]), (15, Z)],
             "position": [(0, Z), (4, [0, 1, 1]), (8, [0, -2, -3], "easeInQuart"), (11, [0, -2, -3]), (15, Z)]},
    "right_arm": rot((0, KR), (4, [-170, 0, 10], "easeOutQuad"), (8, [-35, -50, 0], "easeInQuart"), (11, [-33, -52, 0]), (15, KR)),
    "left_arm": rot((0, KL), (4, [-160, 35, 0], "easeOutQuad"), (8, [-25, 5, 0], "easeInQuart"), (11, [-23, 3, 0]), (15, KL)),
    "right_item": rot((0, KI), (4, [35, 0, 0], "easeOutQuad"), (8, [-35, 0, 0], "easeInQuart"), (11, [-35, 0, 0]), (15, KI)),
    "right_leg": rot((0, Z), (8, [-30, 0, 0], "easeInQuart"), (11, [-30, 0, 0]), (15, Z)),
    "left_leg": rot((0, Z), (8, [20, 0, 0], "easeInQuart"), (11, [20, 0, 0]), (15, Z)),
})
anim("katana_slash_2", 15, {  # 逆袈裟：左下から右上へ斬り上げる
    "body": {"rotation": [(0, Z), (4, [10, -30, 0], "easeOutQuad"), (8, [-6, 35, 0], "easeOutQuart"), (11, [-6, 37, 0]), (15, Z)],
             "position": [(0, Z), (4, [0, -2, 0]), (8, [0, 1, -3], "easeOutQuart"), (11, [0, 1, -3]), (15, Z)]},
    "right_arm": rot((0, KR), (4, [-25, -55, 0], "easeOutQuad"), (8, [-155, 10, 10], "easeOutQuart"), (11, [-158, 10, 10]), (15, KR)),
    "left_arm": rot((0, KL), (4, [-15, 5, 0], "easeOutQuad"), (8, [-145, 45, 0], "easeOutQuart"), (11, [-148, 45, 0]), (15, KL)),
    "right_item": rot((0, KI), (4, [-40, 0, 0], "easeOutQuad"), (8, [25, 0, 0], "easeOutQuart"), (11, [25, 0, 0]), (15, KI)),
    "left_leg": rot((0, Z), (8, [-30, 0, 0], "easeOutQuart"), (11, [-30, 0, 0]), (15, Z)),
    "right_leg": rot((0, Z), (8, [20, 0, 0], "easeOutQuart"), (11, [20, 0, 0]), (15, Z)),
})
anim("katana_slash_3", 18, {  # 唐竹割り：真上から、まっすぐ振り下ろす（しめ）
    "body": {"rotation": [(0, Z), (5, [-10, 0, 0], "easeOutQuad"), (9, [22, 0, 0], "easeInQuart"), (13, [22, 0, 0]), (18, Z)],
             "position": [(0, Z), (5, [0, 1, 1]), (9, [0, -4, -4], "easeInQuart"), (13, [0, -4, -4]), (18, Z)]},
    "right_arm": rot((0, KR), (5, [-185, -15, 0], "easeOutQuad"), (9, [-40, -25, 0], "easeInQuart"), (13, [-40, -25, 0]), (18, KR)),
    "left_arm": rot((0, KL), (5, [-175, 25, 0], "easeOutQuad"), (9, [-30, 35, 0], "easeInQuart"), (13, [-30, 35, 0]), (18, KL)),
    "right_item": rot((0, KI), (5, [40, 0, 0], "easeOutQuad"), (9, [-40, 0, 0], "easeInQuart"), (13, [-40, 0, 0]), (18, KI)),
    "right_leg": rot((0, Z), (9, [-40, 0, 0], "easeInQuart"), (13, [-40, 0, 0]), (18, Z)),
    "left_leg": rot((0, Z), (9, [30, 0, 0], "easeInQuart"), (13, [30, 0, 0]), (18, Z)),
})

def two_handed(src, dst, keep_left=False):
    """剣（片手）の技の動きから、刀（両手）の動きを作る。左手は右手に寄せ、始まりと終わりは刀のかまえ。"""
    import copy
    a = copy.deepcopy(ANIMS[src])
    b = a["bones"]
    ra = b["right_arm"]["rotation"]
    keys = sorted(ra, key=float)
    first, last = keys[0], keys[-1]
    if ra[first]["vector"] == R0: ra[first]["vector"] = KR
    if ra[last]["vector"] == R0: ra[last]["vector"] = KR
    if not keep_left:
        la = {}
        for k in keys:
            v = ra[k]["vector"]
            la[k] = {"vector": KL if v == KR else [v[0] + 10, v[1] + 60, v[2]], "easing": ra[k]["easing"]}
        b["left_arm"] = {"rotation": la}
    else:
        lk = sorted(b["left_arm"]["rotation"], key=float)
        if b["left_arm"]["rotation"][lk[-1]]["vector"] == L0: b["left_arm"]["rotation"][lk[-1]]["vector"] = KL
    if "right_item" in b:
        it = b["right_item"]["rotation"]; ik = sorted(it, key=float)
        for k in (ik[0], ik[-1]):
            if it[k]["vector"] == Z: it[k]["vector"] = KI
    ANIMS[dst] = a

for n in ("sword_sweep", "sword_lunge", "sword_whirl", "sword_parry", "sword_flash", "sword_flash_ready", "sword_flurry", "sword_leap", "sword_moon", "sword_moon_ready"):
    two_handed(n, "katana" + n[5:])
for n in ("sword_iai_ready", "sword_iai"):   # 居合は左手でさやを持つので、左手はそのまま
    two_handed(n, "katana" + n[5:], keep_left=True)   # 居合は左手でさやを持つので、片手のまま

# ================= 共通の道具 =================
def spin(t0, t1, start, turns, ease="linear"):
    """body の y を start から turns 回まわす（右回り＝マイナス）"""
    return [(t0, [0, start, 0]), (t1, [0, start - 360 * turns, 0], ease)]

def with_left(src, dst, hold_r, hold_l, hold_i, dx=10, dy=60, keep_left=False):
    """右手の動きから、両手持ちの動きを作る（左手を右手に寄せる）。始まりと終わりは持っているときのかまえ"""
    import copy
    a = copy.deepcopy(ANIMS[src]); b = a["bones"]
    ra = b["right_arm"]["rotation"]; keys = sorted(ra, key=float)
    for k in (keys[0], keys[-1]):
        if ra[k]["vector"] == R0: ra[k]["vector"] = hold_r
    if not keep_left:
        b["left_arm"] = {"rotation": {k: {"vector": hold_l if ra[k]["vector"] == hold_r else [ra[k]["vector"][0] + dx, ra[k]["vector"][1] + dy, ra[k]["vector"][2]],
                                          "easing": ra[k]["easing"]} for k in keys}}
    it = b.setdefault("right_item", {"rotation": {"0.0": {"vector": Z, "easing": "easeInOutSine"}}})["rotation"]
    ik = sorted(it, key=float)
    for k in (ik[0], ik[-1]):
        if it[k]["vector"] == Z: it[k]["vector"] = hold_i
    ANIMS[dst] = a
    if dst != src: pass

def drop(name):
    ANIMS.pop(name, None)

# ================= 斧（両手で振る大きな武器） =================
AR, AL, AI = [-40, -28, 0], [-30, 32, 0], [-10, 0, 0]   # 大斧のかまえ（両手で低く持つ）
anim("_axe_chop_1", 16, {   # 振りかぶって、真上から叩きつける
    "body": {"rotation": [(0, Z), (5, [-10, 10, 0], "easeOutQuad"), (9, [25, -5, 0], "easeInQuart"), (12, [25, -5, 0]), (16, Z)],
             "position": [(0, Z), (5, [0, 1, 1]), (9, [0, -4, -3], "easeInQuart"), (12, [0, -4, -3]), (16, Z)]},
    "right_arm": rot((0, R0), (5, [-185, 5, 5], "easeOutQuad"), (9, [-35, -20, 0], "easeInQuart"), (12, [-33, -20, 0]), (16, R0)),
    "right_item": rot((0, Z), (5, [40, 0, 0], "easeOutQuad"), (9, [-40, 0, 0], "easeInQuart"), (12, [-40, 0, 0]), (16, Z)),
    "right_leg": rot((0, Z), (9, [-35, 0, 0], "easeInQuart"), (12, [-35, 0, 0]), (16, Z)),
    "left_leg": rot((0, Z), (9, [25, 0, 0], "easeInQuart"), (12, [25, 0, 0]), (16, Z)),
})
anim("_axe_chop_2", 16, {   # 体ごと横になぎ払う
    "body": {"rotation": [(0, Z), (5, [0, 55, 0], "easeOutQuad"), (9, [5, -65, 0], "easeInQuart"), (12, [5, -68, 0]), (16, Z)],
             "position": [(0, Z), (5, [0, -1, 1]), (9, [0, -2, -3], "easeInQuart"), (12, [0, -2, -3]), (16, Z)]},
    "right_arm": rot((0, R0), (5, [-80, 60, 10], "easeOutQuad"), (9, [-80, -60, 0], "easeInQuart"), (12, [-78, -62, 0]), (16, R0)),
    "right_item": rot((0, Z), (5, [20, 0, 90], "easeOutQuad"), (9, [-20, 0, 90], "easeInQuart"), (12, [-20, 0, 90]), (16, Z)),
    "left_leg": rot((0, Z), (9, [-30, 0, 0], "easeInQuart"), (12, [-30, 0, 0]), (16, Z)),
    "right_leg": rot((0, Z), (9, [22, 0, 0], "easeInQuart"), (12, [22, 0, 0]), (16, Z)),
})
anim("_axe_chop_3", 20, {   # しめ：低く引いて、下から上へかち上げる
    "body": {"rotation": [(0, Z), (6, [15, -40, 0], "easeOutQuad"), (11, [-12, 45, 0], "easeOutQuart"), (15, [-12, 47, 0]), (20, Z)],
             "position": [(0, Z), (6, [0, -4, 0], "easeOutQuad"), (11, [0, 3, -3], "easeOutQuart"), (15, [0, 2, -3]), (20, Z)]},
    "right_arm": rot((0, R0), (6, [-10, -50, 0], "easeOutQuad"), (11, [-170, 30, 10], "easeOutQuart"), (15, [-172, 30, 10]), (20, R0)),
    "right_item": rot((0, Z), (6, [-40, 0, 0], "easeOutQuad"), (11, [35, 0, 0], "easeOutQuart"), (15, [35, 0, 0]), (20, Z)),
    "right_leg": rot((0, Z), (6, [-30, 0, 0]), (11, [10, 0, 0], "easeOutQuart"), (15, [10, 0, 0]), (20, Z)),
    "left_leg": rot((0, Z), (6, [-30, 0, 0]), (11, [-25, 0, 0], "easeOutQuart"), (15, [-25, 0, 0]), (20, Z)),
})
anim("_axe_whirl", 26, {    # 回転斬り：斧を横に構えて3回転
    "body": {"rotation": [(0, Z), (3, [0, 40, 0], "easeOutQuad")] + spin(3, 22, 40, 3)[1:] + [(26, [0, -1080, 0], "easeOutQuad")],
             "position": [(0, Z), (3, [0, -2, 0]), (22, [0, -2, 0]), (26, Z)]},
    "right_arm": rot((0, R0), (3, [-85, 70, 0], "easeOutQuad"), (22, [-85, 70, 0]), (26, R0)),
    "right_item": rot((0, Z), (3, [0, 0, 90]), (22, [0, 0, 90]), (26, Z)),
    "right_leg": rot((0, Z), (3, [-20, 0, 15]), (22, [-20, 0, 15]), (26, Z)),
    "left_leg": rot((0, Z), (3, [15, 0, -15]), (22, [15, 0, -15]), (26, Z)),
})
anim("axe_frenzy", 80, {    # 狂乱の大旋風：4秒間まわり続ける（左手は外へ）
    "body": {"rotation": [(0, Z)] + spin(4, 76, 0, 9)[1:] + [(80, [0, -3240, 0], "easeOutQuad")]},
    "right_arm": rot((0, R0), (4, [-85, 70, 0], "easeOutQuad"), (76, [-85, 70, 0]), (80, R0)),
    "left_arm": rot((0, L0), (4, [-40, 0, -70]), (76, [-40, 0, -70]), (80, L0)),
    "right_item": rot((0, Z), (4, [0, 0, 90]), (76, [0, 0, 90]), (80, Z)),
}, loop=False)
anim("axe_throw", 14, {     # 投げ斧：片手で振りかぶって、前へ投げる
    "body": {"rotation": [(0, Z), (5, [-8, 30, 0], "easeOutQuad"), (8, [15, -25, 0], "easeOutExpo"), (11, [15, -25, 0]), (14, Z)]},
    "right_arm": rot((0, R0), (5, [-190, 30, 20], "easeOutQuad"), (8, [-70, -20, 0], "easeOutExpo"), (11, [-70, -20, 0]), (14, R0)),
    "left_arm": rot((0, L0), (5, [-80, 10, -10]), (8, [-20, 0, -40], "easeOutExpo"), (11, [-20, 0, -40]), (14, L0)),
    "right_item": rot((0, Z), (5, [40, 0, 0]), (8, [-30, 0, 0], "easeOutExpo"), (14, Z)),
    "left_leg": rot((0, Z), (5, [-25, 0, 0]), (8, [-30, 0, 0]), (11, [-30, 0, 0]), (14, Z)),
    "right_leg": rot((0, Z), (8, [25, 0, 0]), (11, [25, 0, 0]), (14, Z)),
})
anim("_axe_cry", 24, {      # 雄叫び・激昂：斧を高く掲げて、胸を張って吠える
    "body": {"rotation": [(0, Z), (5, [-12, 0, 0], "easeOutQuad"), (19, [-12, 0, 0]), (24, Z)]},
    "head": rot((0, Z), (5, [-35, 0, 0], "easeOutQuad"), (19, [-35, 0, 0]), (24, Z)),
    "right_arm": rot((0, R0), (5, [-165, 0, 25], "easeOutQuad"), (19, [-168, 0, 25]), (24, R0)),
    "left_arm": rot((0, L0), (5, [-20, 0, -55], "easeOutQuad"), (19, [-22, 0, -58]), (24, L0)),
    "right_item": rot((0, Z), (5, [-30, 0, 0]), (19, [-30, 0, 0]), (24, Z)),
    "right_leg": rot((0, Z), (5, [8, 0, 10]), (19, [8, 0, 10]), (24, Z)),
    "left_leg": rot((0, Z), (5, [-8, 0, -10]), (19, [-8, 0, -10]), (24, Z)),
})
anim("_axe_slam", 18, {     # 大地割り：振りかぶって、地面に深く叩きつける
    "body": {"rotation": [(0, Z), (6, [-12, 0, 0], "easeOutQuad"), (10, [35, 0, 0], "easeInQuart"), (14, [35, 0, 0]), (18, Z)],
             "position": [(0, Z), (6, [0, 1, 1]), (10, [0, -6, -3], "easeInQuart"), (14, [0, -6, -3]), (18, Z)]},
    "right_arm": rot((0, R0), (6, [-190, 0, 5], "easeOutQuad"), (10, [-15, 0, 0], "easeInQuart"), (14, [-15, 0, 0]), (18, R0)),
    "right_item": rot((0, Z), (6, [40, 0, 0]), (10, [-45, 0, 0], "easeInQuart"), (14, [-45, 0, 0]), (18, Z)),
    "right_leg": rot((0, Z), (10, [-40, 0, 0], "easeInQuart"), (14, [-40, 0, 0]), (18, Z)),
    "left_leg": rot((0, Z), (10, [30, 0, 0], "easeInQuart"), (14, [30, 0, 0]), (18, Z)),
})
anim("_axe_sunder_ready", 14, {  # 断鋼撃のため：体を大きくひねって、斧を後ろへ
    "body": {"rotation": [(0, Z), (8, [-10, 45, 0], "easeOutQuad"), (14, [-10, 48, 0])],
             "position": [(0, Z), (8, [0, -2, 2]), (14, [0, -2, 2])]},
    "right_arm": rot((0, R0), (8, [-195, 40, 20], "easeOutQuad"), (14, [-198, 42, 20])),
    "right_item": rot((0, Z), (8, [45, 0, 0]), (14, [45, 0, 0])),
    "left_leg": rot((0, Z), (8, [-25, 0, 0]), (14, [-25, 0, 0])),
    "right_leg": rot((0, Z), (8, [20, 0, 0]), (14, [20, 0, 0])),
})
anim("_axe_sunder", 18, {   # 断鋼撃：渾身の振り下ろし
    "body": {"rotation": [(0, [-10, 48, 0]), (4, [30, -20, 0], "easeInQuart"), (12, [30, -22, 0]), (18, Z)],
             "position": [(0, [0, -2, 2]), (4, [0, -6, -4], "easeInQuart"), (12, [0, -6, -4]), (18, Z)]},
    "right_arm": rot((0, [-198, 42, 20]), (4, [-20, -15, 0], "easeInQuart"), (12, [-20, -15, 0]), (18, R0)),
    "right_item": rot((0, [45, 0, 0]), (4, [-45, 0, 0], "easeInQuart"), (12, [-45, 0, 0]), (18, Z)),
    "right_leg": rot((0, [20, 0, 0]), (4, [-40, 0, 0], "easeInQuart"), (12, [-40, 0, 0]), (18, Z)),
    "left_leg": rot((0, [-25, 0, 0]), (4, [30, 0, 0], "easeInQuart"), (12, [30, 0, 0]), (18, Z)),
})
anim("_axe_charge", 16, {   # 破城槌：斧を低く横に構えて、前のめりで突っ込む
    "body": {"rotation": [(0, Z), (3, [22, -15, 0], "easeOutQuad"), (12, [22, -15, 0]), (16, Z)],
             "position": [(0, Z), (3, [0, -3, -3]), (12, [0, -3, -3]), (16, Z)]},
    "right_arm": rot((0, R0), (3, [-60, -40, 0], "easeOutQuad"), (12, [-60, -40, 0]), (16, R0)),
    "right_item": rot((0, Z), (3, [0, 0, 90]), (12, [0, 0, 90]), (16, Z)),
    "left_leg": rot((0, Z), (3, [-45, 0, 0]), (7, [30, 0, 0]), (12, [-40, 0, 0]), (16, Z)),
    "right_leg": rot((0, Z), (3, [35, 0, 0]), (7, [-40, 0, 0]), (12, [35, 0, 0]), (16, Z)),
})
anim("_axe_sky_ready", 22, {  # 天崩しのため：深くしゃがんで、斧を後ろへ
    "body": {"rotation": [(0, Z), (8, [20, 0, 0], "easeOutQuad"), (22, [20, 0, 0])],
             "position": [(0, Z), (8, [0, -6, 0], "easeOutQuad"), (22, [0, -6, 0])]},
    "right_arm": rot((0, R0), (8, [30, 0, 10], "easeOutQuad"), (22, [32, 0, 10])),
    "right_item": rot((0, Z), (8, [30, 0, 0]), (22, [30, 0, 0])),
    "right_leg": rot((0, Z), (8, [-50, 0, 5]), (22, [-50, 0, 5])),
    "left_leg": rot((0, Z), (8, [-50, 0, -5]), (22, [-50, 0, -5])),
})
anim("_axe_sky", 30, {      # 奥義・天崩し：跳び上がって斧を振りかぶり、落ちながら叩き落とす
    "body": {"rotation": [(0, [20, 0, 0]), (6, [-15, 0, 0], "easeOutQuad"), (16, [-18, 0, 0]), (20, [40, 0, 0], "easeInQuart"), (25, [38, 0, 0]), (30, Z)],
             "position": [(0, [0, -6, 0]), (6, Z, "easeOutQuad"), (20, [0, -6, -3], "easeInQuart"), (25, [0, -6, -3]), (30, Z)]},
    "right_arm": rot((0, [32, 0, 10]), (6, [-190, 0, 10], "easeOutQuad"), (16, [-195, 0, 10]), (20, [-15, 0, 0], "easeInQuart"), (25, [-15, 0, 0]), (30, R0)),
    "right_item": rot((0, [30, 0, 0]), (6, [40, 0, 0]), (16, [45, 0, 0]), (20, [-45, 0, 0], "easeInQuart"), (25, [-45, 0, 0]), (30, Z)),
    "right_leg": rot((0, [-50, 0, 5]), (6, [-45, 0, 0]), (16, [-45, 0, 0]), (20, [-45, 0, 0]), (25, [-45, 0, 0]), (30, Z)),
    "left_leg": rot((0, [-50, 0, -5]), (6, [15, 0, 0]), (16, [15, 0, 0]), (20, [35, 0, 0]), (25, [35, 0, 0]), (30, Z)),
})
anim("_axe_leap", 24, {}) if False else None
for n in ("_axe_chop_1", "_axe_chop_2", "_axe_chop_3", "_axe_whirl", "_axe_cry", "_axe_slam", "_axe_sunder_ready", "_axe_sunder", "_axe_charge", "_axe_sky_ready", "_axe_sky"):
    with_left(n, n[1:], AR, AL, AI)
    drop(n)
anim("axe_hold", 40, {      # 大斧を持っているあいだのかまえ（両手で低く持つ）
    "right_arm": rot((0, AR), (20, [-42, -28, 0]), (40, AR)),
    "left_arm": rot((0, AL), (20, [-32, 32, 0]), (40, AL)),
    "right_item": rot((0, AI), (40, AI)),
}, loop=True)

# ================= 槍（両手でかまえる） =================
SR_, SL_, SI_ = [-30, 10, 10], [-65, 30, 0], [-40, 0, 0]   # 槍のかまえ：右手は腰、左手は前で柄を持つ
def spear_left(src, dst):
    with_left(src, dst, SR_, SL_, SI_, dx=-30, dy=25)
anim("_spear_thrust", 12, {   # 引いて、まっすぐ突く
    "body": {"rotation": [(0, Z), (4, [0, 25, 0], "easeOutQuad"), (6, [8, -15, 0], "easeOutExpo"), (9, [8, -15, 0]), (12, Z)],
             "position": [(0, Z), (4, [0, 0, 2]), (6, [0, -2, -4], "easeOutExpo"), (9, [0, -2, -4]), (12, Z)]},
    "right_arm": rot((0, R0), (4, [-25, 25, 15], "easeOutQuad"), (6, [-88, -5, 0], "easeOutExpo"), (9, [-88, -5, 0]), (12, R0)),
    "right_item": rot((0, Z), (4, [-35, 0, 0]), (6, [-78, 0, 0], "easeOutExpo"), (9, [-78, 0, 0]), (12, Z)),
    "left_leg": rot((0, Z), (6, [-35, 0, 0], "easeOutExpo"), (9, [-35, 0, 0]), (12, Z)),
    "right_leg": rot((0, Z), (6, [25, 0, 0], "easeOutExpo"), (9, [25, 0, 0]), (12, Z)),
})
anim("_spear_sweep", 15, {    # 柄で横になぎ払う
    "body": {"rotation": [(0, Z), (5, [0, 55, 0], "easeOutQuad"), (9, [5, -60, 0], "easeInQuart"), (12, [5, -62, 0]), (15, Z)],
             "position": [(0, Z), (9, [0, -2, -2], "easeInQuart"), (12, [0, -2, -2]), (15, Z)]},
    "right_arm": rot((0, R0), (5, [-70, 65, 15], "easeOutQuad"), (9, [-75, -65, -5], "easeInQuart"), (12, [-75, -67, -5]), (15, R0)),
    "right_item": rot((0, Z), (5, [10, 0, 90]), (9, [-10, 0, 90], "easeInQuart"), (12, [-10, 0, 90]), (15, Z)),
    "left_leg": rot((0, Z), (9, [-30, 0, 0], "easeInQuart"), (12, [-30, 0, 0]), (15, Z)),
    "right_leg": rot((0, Z), (9, [22, 0, 0], "easeInQuart"), (12, [22, 0, 0]), (15, Z)),
})
anim("_spear_thrust_2", 16, {  # しめ：深く踏み込んで、体ごと突き出す
    "body": {"rotation": [(0, Z), (5, [-5, 30, 0], "easeOutQuad"), (8, [15, -20, 0], "easeOutExpo"), (12, [15, -20, 0]), (16, Z)],
             "position": [(0, Z), (5, [0, -1, 3]), (8, [0, -4, -6], "easeOutExpo"), (12, [0, -4, -6]), (16, Z)]},
    "right_arm": rot((0, R0), (5, [-20, 30, 15], "easeOutQuad"), (8, [-92, -8, 0], "easeOutExpo"), (12, [-92, -8, 0]), (16, R0)),
    "right_item": rot((0, Z), (5, [-40, 0, 0]), (8, [-82, 0, 0], "easeOutExpo"), (12, [-82, 0, 0]), (16, Z)),
    "left_leg": rot((0, Z), (8, [-82, 0, 0], "easeOutExpo"), (12, [-82, 0, 0]), (16, Z)),
    "right_leg": rot((0, Z), (8, [35, 0, 0], "easeOutExpo"), (12, [35, 0, 0]), (16, Z)),
})
anim("_spear_lunge", 14, {    # 一閃突き：低く構えて、駆け抜けながら突く
    "body": {"rotation": [(0, Z), (3, [20, 0, 0], "easeOutQuad"), (10, [20, 0, 0]), (14, Z)],
             "position": [(0, Z), (3, [0, -3, -3]), (10, [0, -3, -3]), (14, Z)]},
    "right_arm": rot((0, R0), (3, [-85, -5, 0], "easeOutExpo"), (10, [-85, -5, 0]), (14, R0)),
    "right_item": rot((0, Z), (3, [-75, 0, 0], "easeOutExpo"), (10, [-75, 0, 0]), (14, Z)),
    "left_leg": rot((0, Z), (3, [-45, 0, 0]), (10, [-45, 0, 0]), (14, Z)),
    "right_leg": rot((0, Z), (3, [40, 0, 0]), (10, [40, 0, 0]), (14, Z)),
})
anim("_spear_wide", 22, {     # 薙ぎ払い：槍を大きくまわして、周りをなぎ払う
    "body": {"rotation": [(0, Z), (4, [0, 50, 0], "easeOutQuad"), (14, [0, -310, 0], "easeOutQuart"), (18, [0, -320, 0]), (22, [0, -360, 0])],
             "position": [(0, Z), (4, [0, -2, 0]), (18, [0, -2, 0]), (22, Z)]},
    "right_arm": rot((0, R0), (4, [-80, 70, 10], "easeOutQuad"), (14, [-80, 70, 10]), (18, [-80, 70, 10]), (22, R0)),
    "right_item": rot((0, Z), (4, [0, 0, 90]), (18, [0, 0, 90]), (22, Z)),
    "right_leg": rot((0, Z), (4, [-20, 0, 15]), (18, [-20, 0, 15]), (22, Z)),
    "left_leg": rot((0, Z), (4, [15, 0, -15]), (18, [15, 0, -15]), (22, Z)),
})
anim("spear_throw", 16, {     # 投げ槍：肩の上に構えて、遠くへ投げる（片手）
    "body": {"rotation": [(0, Z), (6, [-8, 35, 0], "easeOutQuad"), (9, [15, -25, 0], "easeOutExpo"), (12, [15, -25, 0]), (16, Z)],
             "position": [(0, Z), (6, [0, 0, 2]), (9, [0, -2, -3], "easeOutExpo"), (12, [0, -2, -3]), (16, Z)]},
    "right_arm": rot((0, R0), (6, [-170, 20, 15], "easeOutQuad"), (9, [-80, -15, 0], "easeOutExpo"), (12, [-80, -15, 0]), (16, R0)),
    "left_arm": rot((0, L0), (6, [-90, 20, -5]), (9, [-20, 0, -40], "easeOutExpo"), (12, [-20, 0, -40]), (16, L0)),
    "right_item": rot((0, Z), (6, [-80, 0, 0]), (9, [-40, 0, 0], "easeOutExpo"), (16, Z)),
    "left_leg": rot((0, Z), (6, [-25, 0, 0]), (9, [-30, 0, 0]), (12, [-30, 0, 0]), (16, Z)),
    "right_leg": rot((0, Z), (9, [25, 0, 0]), (12, [25, 0, 0]), (16, Z)),
})
anim("_spear_leap", 26, {     # 跳び突き：跳び上がり、真下へ突き刺す
    "body": {"rotation": [(0, Z), (3, [15, 0, 0]), (8, [-10, 0, 0], "easeOutQuad"), (15, [-10, 0, 0]), (18, [30, 0, 0], "easeInQuart"), (22, [30, 0, 0]), (26, Z)],
             "position": [(0, Z), (3, [0, -3, 0]), (8, Z), (18, [0, -4, -2], "easeInQuart"), (22, [0, -4, -2]), (26, Z)]},
    "right_arm": rot((0, R0), (3, [-20, 0, 15]), (8, [-170, 0, 10], "easeOutQuad"), (15, [-175, 0, 10]), (18, [-40, 0, 0], "easeInQuart"), (22, [-40, 0, 0]), (26, R0)),
    "right_item": rot((0, Z), (8, [-90, 0, 0]), (15, [-90, 0, 0]), (18, [-80, 0, 0], "easeInQuart"), (22, [-80, 0, 0]), (26, Z)),
    "right_leg": rot((0, Z), (3, [-30, 0, 0]), (8, [-50, 0, 0]), (15, [-50, 0, 0]), (18, [-40, 0, 0]), (22, [-40, 0, 0]), (26, Z)),
    "left_leg": rot((0, Z), (3, [-30, 0, 0]), (8, [20, 0, 0]), (15, [20, 0, 0]), (18, [10, 0, 0]), (22, [10, 0, 0]), (26, Z)),
})
_tr = [(0, R0)]; _tb = [(0, Z)]; _ti = [(0, Z)]
for i, t in enumerate((0, 4, 8)):   # 三段突き：4tickごとに突く（3回目は深く）
    deep = i == 2
    _tr += [(t + 2, [-30, 25, 15], "easeOutQuad"), (t + 4, [-90 - (4 if deep else 0), -5, 0], "easeOutExpo")]
    _tb += [(t + 2, [0, 20, 0], "easeOutQuad"), (t + 4, [10 if deep else 5, -15, 0], "easeOutExpo")]
    _ti += [(t + 2, [-40, 0, 0], "easeOutQuad"), (t + 4, [-80, 0, 0], "easeOutExpo")]
_tr += [(16, [-92, -5, 0]), (20, R0)]; _tb += [(16, [10, -15, 0]), (20, Z)]; _ti += [(16, [-82, 0, 0]), (20, Z)]
anim("_spear_triple", 20, {
    "body": {"rotation": _tb, "position": [(0, Z), (12, [0, -3, -5], "easeOutExpo"), (16, [0, -3, -5]), (20, Z)]},
    "right_arm": {"rotation": _tr},
    "right_item": {"rotation": _ti},
    "left_leg": rot((0, Z), (4, [-25, 0, 0]), (12, [-40, 0, 0]), (16, [-40, 0, 0]), (20, Z)),
    "right_leg": rot((0, Z), (4, [20, 0, 0]), (12, [30, 0, 0]), (16, [30, 0, 0]), (20, Z)),
})
anim("_spear_twirl", 24, {    # 渦潮突き・大渦：槍を頭の上でまわしてから、前へ突き出す
    "body": {"rotation": [(0, Z), (14, [0, 0, 0]), (17, [12, -15, 0], "easeOutExpo"), (20, [12, -15, 0]), (24, Z)],
             "position": [(0, Z), (17, [0, -3, -4], "easeOutExpo"), (20, [0, -3, -4]), (24, Z)]},
    "right_arm": rot((0, R0), (3, [-165, 30, 10]), (6, [-165, -30, -10]), (9, [-165, 30, 10]), (12, [-165, -30, -10]), (14, [-30, 25, 15]),
                     (17, [-90, -5, 0], "easeOutExpo"), (20, [-90, -5, 0]), (24, R0)),
    "right_item": rot((0, Z), (3, [-60, 0, 90]), (6, [-60, 0, -90]), (9, [-60, 0, 90]), (12, [-60, 0, -90]), (14, [-40, 0, 0]),
                      (17, [-80, 0, 0], "easeOutExpo"), (20, [-80, 0, 0]), (24, Z)),
    "left_leg": rot((0, Z), (17, [-40, 0, 0], "easeOutExpo"), (20, [-40, 0, 0]), (24, Z)),
    "right_leg": rot((0, Z), (17, [30, 0, 0], "easeOutExpo"), (20, [30, 0, 0]), (24, Z)),
})
anim("_spear_brace", 100, {   # 槍衾：低く腰を落とし、槍を前に向けて構え続ける（5秒）
    "body": {"rotation": [(0, Z), (5, [12, 0, 0]), (95, [12, 0, 0]), (100, Z)], "position": [(0, Z), (5, [0, -4, 0]), (95, [0, -4, 0]), (100, Z)]},
    "right_arm": rot((0, R0), (5, [-60, 10, 10]), (95, [-60, 10, 10]), (100, R0)),
    "right_item": rot((0, Z), (5, [-60, 0, 0]), (95, [-60, 0, 0]), (100, Z)),
    "left_leg": rot((0, Z), (5, [-40, 0, -10]), (95, [-40, 0, -10]), (100, Z)),
    "right_leg": rot((0, Z), (5, [35, 0, 10]), (95, [35, 0, 10]), (100, Z)),
})
anim("_spear_raise_ready", 30, {  # 千本槍・海神降臨のため：槍を天へ掲げる
    "body": {"rotation": [(0, Z), (6, [-10, 0, 0], "easeOutQuad"), (30, [-10, 0, 0])]},
    "head": rot((0, Z), (6, [-30, 0, 0], "easeOutQuad"), (30, [-30, 0, 0])),
    "right_arm": rot((0, R0), (6, [-170, 0, 10], "easeOutQuad"), (30, [-172, 0, 10])),
    "right_item": rot((0, Z), (6, [-90, 0, 0], "easeOutQuad"), (30, [-90, 0, 0])),
    "right_leg": rot((0, Z), (6, [10, 0, 8]), (30, [10, 0, 8])),
    "left_leg": rot((0, Z), (6, [-10, 0, -8]), (30, [-10, 0, -8])),
})
anim("_spear_raise", 16, {    # 掲げた槍を、前へ振り下ろして指し示す
    "body": {"rotation": [(0, [-10, 0, 0]), (4, [15, 0, 0], "easeInQuart"), (11, [15, 0, 0]), (16, Z)],
             "position": [(0, Z), (4, [0, -3, -3], "easeInQuart"), (11, [0, -3, -3]), (16, Z)]},
    "head": rot((0, [-30, 0, 0]), (4, Z), (16, Z)),
    "right_arm": rot((0, [-172, 0, 10]), (4, [-88, -5, 0], "easeInQuart"), (11, [-88, -5, 0]), (16, R0)),
    "right_item": rot((0, [-90, 0, 0]), (4, [-78, 0, 0], "easeInQuart"), (11, [-78, 0, 0]), (16, Z)),
    "right_leg": rot((0, [10, 0, 8]), (4, [-35, 0, 0], "easeInQuart"), (11, [-35, 0, 0]), (16, Z)),
    "left_leg": rot((0, [-10, 0, -8]), (4, [25, 0, 0], "easeInQuart"), (11, [25, 0, 0]), (16, Z)),
})
for n in ("_spear_thrust", "_spear_sweep", "_spear_thrust_2", "_spear_lunge", "_spear_wide", "_spear_leap", "_spear_triple", "_spear_twirl", "_spear_brace", "_spear_raise_ready", "_spear_raise"):
    spear_left(n, n[1:]); drop(n)
anim("spear_hold", 40, {    # 槍を持っているあいだのかまえ（両手で、穂先を前へ）
    "right_arm": rot((0, SR_), (20, [-32, 10, 10]), (40, SR_)),
    "left_arm": rot((0, SL_), (20, [-67, 30, 0]), (40, SL_)),
    "right_item": rot((0, SI_), (40, SI_)),
}, loop=True)

# ================= 短剣（逆手の二刀流） =================
anim("dagger_right", 9, {     # 右の逆手で、外から内へ斬る
    "body": {"rotation": [(0, Z), (3, [0, 20, 0], "easeOutQuad"), (5, [5, -20, 0], "easeOutExpo"), (7, [5, -20, 0]), (9, Z)],
             "position": [(0, Z), (5, [0, -1, -2], "easeOutExpo"), (7, [0, -1, -2]), (9, Z)]},
    "right_arm": rot((0, R0), (3, [-110, 50, 40], "easeOutQuad"), (5, [-55, -60, -10], "easeOutExpo"), (7, [-55, -60, -10]), (9, R0)),
    "right_item": rot((0, Z), (3, [35, 0, 0]), (5, [-25, 0, 0], "easeOutExpo"), (9, Z)),
    "left_arm": rot((0, L0), (3, [-60, -10, -10]), (7, [-40, 0, -20]), (9, L0)),
})
anim("dagger_left", 9, {      # 左の逆手で、外から内へ斬る
    "body": {"rotation": [(0, Z), (3, [0, -20, 0], "easeOutQuad"), (5, [5, 20, 0], "easeOutExpo"), (7, [5, 20, 0]), (9, Z)],
             "position": [(0, Z), (5, [0, -1, -2], "easeOutExpo"), (7, [0, -1, -2]), (9, Z)]},
    "left_arm": rot((0, L0), (3, [-110, -50, -40], "easeOutQuad"), (5, [-55, 60, 10], "easeOutExpo"), (7, [-55, 60, 10]), (9, L0)),
    "left_item": rot((0, Z), (3, [35, 0, 0]), (5, [-25, 0, 0], "easeOutExpo"), (9, Z)),
    "right_arm": rot((0, R0), (3, [-60, 10, 10]), (7, [-40, 0, 20]), (9, R0)),
})
anim("dagger_twin", 12, {     # 双牙：左右で続けて×の字に斬る
    "body": {"rotation": [(0, Z), (2, [0, 20, 0]), (4, [5, -20, 0], "easeOutExpo"), (6, [0, -20, 0]), (8, [5, 20, 0], "easeOutExpo"), (10, [5, 20, 0]), (12, Z)],
             "position": [(0, Z), (4, [0, -2, -2], "easeOutExpo"), (10, [0, -2, -2]), (12, Z)]},
    "right_arm": rot((0, R0), (2, [-120, 45, 35]), (4, [-50, -55, -10], "easeOutExpo"), (12, R0)),
    "left_arm": rot((0, L0), (2, [-40, 0, -20]), (6, [-120, -45, -35]), (8, [-50, 55, 10], "easeOutExpo"), (12, L0)),
    "right_item": rot((0, Z), (2, [35, 0, 0]), (4, [-25, 0, 0], "easeOutExpo"), (12, Z)),
    "left_item": rot((0, Z), (6, [35, 0, 0]), (8, [-25, 0, 0], "easeOutExpo"), (12, Z)),
})
anim("dagger_cross", 14, {    # ×の字斬り（暗殺など、両手で同時に）
    "body": {"rotation": [(0, Z), (4, [-10, 0, 0], "easeOutQuad"), (7, [20, 0, 0], "easeOutExpo"), (10, [20, 0, 0]), (14, Z)],
             "position": [(0, Z), (4, [0, 1, 1]), (7, [0, -3, -3], "easeOutExpo"), (10, [0, -3, -3]), (14, Z)]},
    "right_arm": rot((0, R0), (4, [-160, -30, -20], "easeOutQuad"), (7, [-40, 50, 35], "easeOutExpo"), (10, [-40, 50, 35]), (14, R0)),
    "left_arm": rot((0, L0), (4, [-160, 30, 20], "easeOutQuad"), (7, [-40, -50, -35], "easeOutExpo"), (10, [-40, -50, -35]), (14, L0)),
    "right_item": rot((0, Z), (4, [30, 0, 0]), (7, [-30, 0, 0], "easeOutExpo"), (14, Z)),
    "left_item": rot((0, Z), (4, [30, 0, 0]), (7, [-30, 0, 0], "easeOutExpo"), (14, Z)),
    "right_leg": rot((0, Z), (7, [-30, 0, 0], "easeOutExpo"), (10, [-30, 0, 0]), (14, Z)),
    "left_leg": rot((0, Z), (7, [20, 0, 0], "easeOutExpo"), (10, [20, 0, 0]), (14, Z)),
})
anim("dagger_crouch", 20, {   # 影渡り・隠密・煙玉・ため：低くしゃがみ、短剣を後ろへ
    "body": {"rotation": [(0, Z), (4, [25, 0, 0], "easeOutQuad"), (16, [25, 0, 0]), (20, Z)],
             "position": [(0, Z), (4, [0, -5, 0], "easeOutQuad"), (16, [0, -5, 0]), (20, Z)]},
    "right_arm": rot((0, R0), (4, [20, 20, 25], "easeOutQuad"), (16, [20, 20, 25]), (20, R0)),
    "left_arm": rot((0, L0), (4, [20, -20, -25], "easeOutQuad"), (16, [20, -20, -25]), (20, L0)),
    "right_leg": rot((0, Z), (4, [-45, 0, 8]), (16, [-45, 0, 8]), (20, Z)),
    "left_leg": rot((0, Z), (4, [-20, 0, -8]), (16, [-20, 0, -8]), (20, Z)),
})
anim("dagger_throw", 12, {    # 投げナイフ：腕を横に振って、扇の形に投げる
    "body": {"rotation": [(0, Z), (4, [0, -35, 0], "easeOutQuad"), (7, [0, 30, 0], "easeOutExpo"), (9, [0, 32, 0]), (12, Z)]},
    "right_arm": rot((0, R0), (4, [-80, -60, -10], "easeOutQuad"), (7, [-90, 75, 20], "easeOutExpo"), (9, [-90, 77, 20]), (12, R0)),
    "right_item": rot((0, Z), (4, [0, 0, 90]), (7, [0, 0, 90], "easeOutExpo"), (12, Z)),
    "left_arm": rot((0, L0), (4, [-40, 0, -30]), (9, [-30, 0, -40]), (12, L0)),
})
anim("dagger_coat", 18, {     # 毒刃：短剣を胸の前で交差させて、刃に毒をぬる
    "right_arm": rot((0, R0), (4, [-85, -35, 0], "easeOutQuad"), (14, [-85, -35, 0]), (18, R0)),
    "left_arm": rot((0, L0), (4, [-85, 35, 0], "easeOutQuad"), (9, [-80, 25, 0]), (14, [-85, 35, 0]), (18, L0)),
    "head": rot((0, Z), (4, [15, 0, 0]), (14, [15, 0, 0]), (18, Z)),
})
_tf_r = [(0, R0)]; _tf_l = [(0, L0)]; _tf_b = [(0, Z)]
for i in range(20):   # 千刃：2tickごとに左右交互に切り刻む（2秒）
    t = 1 + i * 2
    if i % 2 == 0:
        _tf_r.append((t, [-70 - (i % 4) * 5, -45 if i % 4 == 0 else 45, 0], "easeOutExpo")); _tf_b.append((t, [5, -10, 0], "easeOutExpo"))
    else:
        _tf_l.append((t, [-70 - (i % 4) * 5, 45 if i % 4 == 1 else -45, 0], "easeOutExpo")); _tf_b.append((t, [5, 10, 0], "easeOutExpo"))
_tf_r.append((44, R0)); _tf_l.append((44, L0)); _tf_b.append((44, Z))
anim("dagger_flurry", 44, {
    "body": {"rotation": _tf_b, "position": [(0, Z), (2, [0, -2, -2]), (40, [0, -2, -2]), (44, Z)]},
    "right_arm": {"rotation": _tf_r}, "left_arm": {"rotation": _tf_l},
    "left_leg": rot((0, Z), (2, [-25, 0, 0]), (40, [-25, 0, 0]), (44, Z)),
    "right_leg": rot((0, Z), (2, [20, 0, 0]), (40, [20, 0, 0]), (44, Z)),
})
anim("dagger_point", 18, {    # 死の刻印：短剣で敵を指す
    "right_arm": rot((0, R0), (4, [-95, 10, 0], "easeOutExpo"), (14, [-95, 10, 0]), (18, R0)),
    "right_item": rot((0, Z), (4, [-90, 0, 0], "easeOutExpo"), (14, [-90, 0, 0]), (18, Z)),
    "body": {"rotation": [(0, Z), (4, [0, -20, 0]), (14, [0, -20, 0]), (18, Z)]},
})
anim("dagger_spin", 16, {     # 影舞・冥府送り：くるっとまわりながら両手で斬る
    "body": {"rotation": [(0, Z), (2, [0, 30, 0]), (10, [0, -330, 0], "easeOutQuart"), (13, [0, -350, 0]), (16, [0, -360, 0])],
             "position": [(0, Z), (2, [0, -2, 0]), (13, [0, -2, 0]), (16, Z)]},
    "right_arm": rot((0, R0), (2, [-80, 60, 30]), (13, [-80, 60, 30]), (16, R0)),
    "left_arm": rot((0, L0), (2, [-80, -60, -30]), (13, [-80, -60, -30]), (16, L0)),
})

# ================= 拳 =================
anim("fist_right", 8, {       # 右ストレート（体ごと）
    "body": {"rotation": [(0, Z), (2, [0, 15, 0], "easeOutQuad"), (4, [5, -20, 0], "easeOutExpo"), (6, [5, -20, 0]), (8, Z)],
             "position": [(0, Z), (4, [0, -1, -2], "easeOutExpo"), (6, [0, -1, -2]), (8, Z)]},
    "right_arm": rot((0, R0), (2, [-50, 25, 10], "easeOutQuad"), (4, [-95, -15, 0], "easeOutExpo"), (6, [-95, -15, 0]), (8, R0)),
    "left_arm": rot((0, L0), (2, [-70, -20, -5]), (4, [-50, 15, 0]), (8, L0)),
    "left_leg": rot((0, Z), (4, [-20, 0, 0], "easeOutExpo"), (6, [-20, 0, 0]), (8, Z)),
})
anim("fist_left", 8, {        # 左ストレート
    "body": {"rotation": [(0, Z), (2, [0, -15, 0], "easeOutQuad"), (4, [5, 20, 0], "easeOutExpo"), (6, [5, 20, 0]), (8, Z)],
             "position": [(0, Z), (4, [0, -1, -2], "easeOutExpo"), (6, [0, -1, -2]), (8, Z)]},
    "left_arm": rot((0, L0), (2, [-50, -25, -10], "easeOutQuad"), (4, [-95, 15, 0], "easeOutExpo"), (6, [-95, 15, 0]), (8, L0)),
    "right_arm": rot((0, R0), (2, [-70, 20, 5]), (4, [-50, -15, 0]), (8, R0)),
    "right_leg": rot((0, Z), (4, [-20, 0, 0], "easeOutExpo"), (6, [-20, 0, 0]), (8, Z)),
})
def fist_rapid(name, count, step, length):
    r = [(0, R0)]; l = [(0, L0)]; bd = [(0, Z)]
    for i in range(count):
        t = 1 + i * step
        if i % 2 == 0:
            r += [(t, [-95, -10 + (i % 3) * 8, 0], "easeOutExpo"), (t + step, [-55, 20, 5])]; bd.append((t, [5, -12, 0], "easeOutExpo"))
        else:
            l += [(t, [-95, 10 - (i % 3) * 8, 0], "easeOutExpo"), (t + step, [-55, -20, -5])]; bd.append((t, [5, 12, 0], "easeOutExpo"))
    r.append((length, R0)); l.append((length, L0)); bd.append((length, Z))
    anim(name, length, {"body": {"rotation": bd, "position": [(0, Z), (2, [0, -2, -2]), (length - 4, [0, -2, -2]), (length, Z)]},
                        "right_arm": {"rotation": r}, "left_arm": {"rotation": l},
                        "left_leg": rot((0, Z), (2, [-25, 0, 0]), (length - 4, [-25, 0, 0]), (length, Z)),
                        "right_leg": rot((0, Z), (2, [20, 0, 0]), (length - 4, [20, 0, 0]), (length, Z))})
fist_rapid("fist_rush", 8, 3, 30)       # ラッシュ：8回
fist_rapid("fist_hundred", 30, 2, 66)   # 百裂拳：30回（0.1秒ごと）
anim("fist_straight", 14, {   # 正拳突き：腰を落として、重い一撃
    "body": {"rotation": [(0, Z), (5, [5, 30, 0], "easeOutQuad"), (7, [10, -25, 0], "easeOutExpo"), (11, [10, -25, 0]), (14, Z)],
             "position": [(0, Z), (5, [0, -3, 1]), (7, [0, -4, -4], "easeOutExpo"), (11, [0, -4, -4]), (14, Z)]},
    "right_arm": rot((0, R0), (5, [-30, 30, 15], "easeOutQuad"), (7, [-92, -12, 0], "easeOutExpo"), (11, [-92, -12, 0]), (14, R0)),
    "left_arm": rot((0, L0), (5, [-85, -20, -5]), (7, [-30, 20, -10], "easeOutExpo"), (11, [-30, 20, -10]), (14, L0)),
    "left_leg": rot((0, Z), (7, [-40, 0, 0], "easeOutExpo"), (11, [-40, 0, 0]), (14, Z)),
    "right_leg": rot((0, Z), (7, [30, 0, 0], "easeOutExpo"), (11, [30, 0, 0]), (14, Z)),
})
anim("fist_dash", 14, {       # 踏み込み拳：前へ飛び込みながら殴る
    "body": {"rotation": [(0, Z), (3, [25, -15, 0], "easeOutQuad"), (10, [25, -15, 0]), (14, Z)],
             "position": [(0, Z), (3, [0, -3, -4]), (10, [0, -3, -4]), (14, Z)]},
    "right_arm": rot((0, R0), (3, [-100, -10, 0], "easeOutExpo"), (10, [-100, -10, 0]), (14, R0)),
    "left_arm": rot((0, L0), (3, [30, 0, -20]), (10, [30, 0, -20]), (14, L0)),
    "left_leg": rot((0, Z), (3, [-45, 0, 0]), (10, [-45, 0, 0]), (14, Z)),
    "right_leg": rot((0, Z), (3, [40, 0, 0]), (10, [40, 0, 0]), (14, Z)),
})
anim("fist_uppercut", 16, {   # 昇龍拳：しゃがんで、跳び上がりながら突き上げる
    "body": {"rotation": [(0, Z), (4, [20, 15, 0], "easeOutQuad"), (7, [-15, -20, 0], "easeOutExpo"), (12, [-15, -20, 0]), (16, Z)],
             "position": [(0, Z), (4, [0, -4, 0]), (7, [0, 6, -2], "easeOutExpo"), (12, [0, 5, -2]), (16, Z)]},
    "right_arm": rot((0, R0), (4, [-10, 10, 25], "easeOutQuad"), (7, [-178, -10, 0], "easeOutExpo"), (12, [-178, -10, 0]), (16, R0)),
    "left_arm": rot((0, L0), (4, [-70, 20, -10]), (7, [-20, 0, -30], "easeOutExpo"), (12, [-20, 0, -30]), (16, L0)),
    "right_leg": rot((0, Z), (4, [-40, 0, 0]), (7, [10, 0, 0], "easeOutExpo"), (12, [10, 0, 0]), (16, Z)),
    "left_leg": rot((0, Z), (4, [-30, 0, 0]), (7, [-50, 0, 0], "easeOutExpo"), (12, [-50, 0, 0]), (16, Z)),
})
anim("fist_ki", 24, {         # 気合い・気功：腰を落とし、両拳を腰に引いて気をためる
    "body": {"position": [(0, Z), (5, [0, -4, 0], "easeOutQuad"), (19, [0, -4, 0]), (24, Z)]},
    "head": rot((0, Z), (5, [-15, 0, 0]), (19, [-15, 0, 0]), (24, Z)),
    "right_arm": rot((0, R0), (5, [20, 0, 25], "easeOutQuad"), (19, [20, 0, 25]), (24, R0)),
    "left_arm": rot((0, L0), (5, [20, 0, -25], "easeOutQuad"), (19, [20, 0, -25]), (24, L0)),
    "right_leg": rot((0, Z), (5, [-20, 0, 18]), (19, [-20, 0, 18]), (24, Z)),
    "left_leg": rot((0, Z), (5, [-20, 0, -18]), (19, [-20, 0, -18]), (24, Z)),
})
anim("fist_kick", 16, {       # 旋風脚：片足を上げて、くるっと回し蹴り
    "body": {"rotation": [(0, Z), (3, [0, 30, 0]), (11, [0, -330, 0], "easeOutQuart"), (13, [0, -350, 0]), (16, [0, -360, 0])],
             "position": [(0, Z), (3, [0, 2, 0]), (11, [0, 2, 0]), (16, Z)]},
    "right_leg": rot((0, Z), (3, [-30, 0, 70], "easeOutQuad"), (11, [-20, 0, 85]), (13, [-20, 0, 80]), (16, Z)),
    "left_leg": rot((0, Z), (3, [5, 0, -10]), (13, [5, 0, -10]), (16, Z)),
    "right_arm": rot((0, R0), (3, [-40, 0, 60]), (13, [-40, 0, 60]), (16, R0)),
    "left_arm": rot((0, L0), (3, [-40, 0, -60]), (13, [-40, 0, -60]), (16, L0)),
})
anim("fist_double_ready", 12, {  # 双拳打ちのため：両拳を後ろへ引く
    "body": {"rotation": [(0, Z), (6, [10, 0, 0], "easeOutQuad"), (12, [10, 0, 0])], "position": [(0, Z), (6, [0, -3, 1]), (12, [0, -3, 1])]},
    "right_arm": rot((0, R0), (6, [25, 0, 20], "easeOutQuad"), (12, [25, 0, 20])),
    "left_arm": rot((0, L0), (6, [25, 0, -20], "easeOutQuad"), (12, [25, 0, -20])),
    "right_leg": rot((0, Z), (6, [25, 0, 0]), (12, [25, 0, 0])),
    "left_leg": rot((0, Z), (6, [-30, 0, 0]), (12, [-30, 0, 0])),
})
anim("fist_double", 14, {     # 双拳打ち：両拳を同時に打ち込む
    "body": {"rotation": [(0, [10, 0, 0]), (3, [12, 0, 0], "easeOutExpo"), (10, [12, 0, 0]), (14, Z)],
             "position": [(0, [0, -3, 1]), (3, [0, -3, -4], "easeOutExpo"), (10, [0, -3, -4]), (14, Z)]},
    "right_arm": rot((0, [25, 0, 20]), (3, [-92, -12, 0], "easeOutExpo"), (10, [-92, -12, 0]), (14, R0)),
    "left_arm": rot((0, [25, 0, -20]), (3, [-92, 12, 0], "easeOutExpo"), (10, [-92, 12, 0]), (14, L0)),
    "right_leg": rot((0, [25, 0, 0]), (3, [30, 0, 0]), (10, [30, 0, 0]), (14, Z)),
    "left_leg": rot((0, [-30, 0, 0]), (3, [-40, 0, 0]), (10, [-40, 0, 0]), (14, Z)),
})
anim("fist_stomp", 18, {      # 震脚：片ひざを高く上げて、地面を踏み鳴らす
    "body": {"rotation": [(0, Z), (6, [-5, 0, 0], "easeOutQuad"), (9, [15, 0, 0], "easeInQuart"), (14, [15, 0, 0]), (18, Z)],
             "position": [(0, Z), (6, [0, 1, 0]), (9, [0, -4, 0], "easeInQuart"), (14, [0, -4, 0]), (18, Z)]},
    "right_leg": rot((0, Z), (6, [-95, 0, 5], "easeOutQuad"), (9, [-20, 0, 12], "easeInQuart"), (14, [-20, 0, 12]), (18, Z)),
    "left_leg": rot((0, Z), (9, [-15, 0, -12], "easeInQuart"), (14, [-15, 0, -12]), (18, Z)),
    "right_arm": rot((0, R0), (6, [-60, 0, 40]), (9, [10, 0, 35], "easeInQuart"), (14, [10, 0, 35]), (18, R0)),
    "left_arm": rot((0, L0), (6, [-60, 0, -40]), (9, [10, 0, -35], "easeInQuart"), (14, [10, 0, -35]), (18, L0)),
})
anim("fist_overlord_ready", 22, {  # 覇王拳のため：深く腰を落とし、右の拳を大きく引く
    "body": {"rotation": [(0, Z), (8, [10, 40, 0], "easeOutQuad"), (22, [10, 42, 0])], "position": [(0, Z), (8, [0, -5, 1]), (22, [0, -5, 1])]},
    "right_arm": rot((0, R0), (8, [30, 20, 25], "easeOutQuad"), (22, [32, 20, 25])),
    "left_arm": rot((0, L0), (8, [-90, -10, -5], "easeOutQuad"), (22, [-90, -10, -5])),
    "right_leg": rot((0, Z), (8, [30, 0, 10]), (22, [30, 0, 10])),
    "left_leg": rot((0, Z), (8, [-40, 0, -10]), (22, [-40, 0, -10])),
})
anim("fist_overlord", 20, {   # 奥義・覇王拳：体ごと、渾身の拳を打ち出す
    "body": {"rotation": [(0, [10, 42, 0]), (3, [15, -35, 0], "easeOutExpo"), (14, [15, -35, 0]), (20, Z)],
             "position": [(0, [0, -5, 1]), (3, [0, -5, -6], "easeOutExpo"), (14, [0, -5, -6]), (20, Z)]},
    "right_arm": rot((0, [32, 20, 25]), (3, [-92, -15, 0], "easeOutExpo"), (14, [-92, -15, 0]), (20, R0)),
    "left_arm": rot((0, [-90, -10, -5]), (3, [20, 20, -20], "easeOutExpo"), (14, [20, 20, -20]), (20, L0)),
    "right_leg": rot((0, [30, 0, 10]), (3, [35, 0, 0]), (14, [35, 0, 0]), (20, Z)),
    "left_leg": rot((0, [-40, 0, -10]), (3, [-50, 0, 0]), (14, [-50, 0, 0]), (20, Z)),
})

# ================= 鞭 =================
anim("whip_crack", 12, {      # 振りかぶって、前へ打ちつける（伸びる鞭は別に描く）
    "body": {"rotation": [(0, Z), (4, [-8, 20, 0], "easeOutQuad"), (6, [10, -15, 0], "easeOutExpo"), (9, [10, -15, 0]), (12, Z)],
             "position": [(0, Z), (6, [0, -1, -2], "easeOutExpo"), (9, [0, -1, -2]), (12, Z)]},
    "right_arm": rot((0, R0), (4, [-185, 25, 15], "easeOutQuad"), (6, [-75, -10, 0], "easeOutExpo"), (9, [-75, -10, 0]), (12, R0)),
    "right_item": rot((0, Z), (4, [40, 0, 0], "easeOutQuad"), (6, [-30, 0, 0], "easeOutExpo"), (12, Z)),
    "left_leg": rot((0, Z), (6, [-25, 0, 0], "easeOutExpo"), (9, [-25, 0, 0]), (12, Z)),
})
anim("whip_side", 12, {       # 横から打つ
    "body": {"rotation": [(0, Z), (4, [0, 40, 0], "easeOutQuad"), (6, [5, -30, 0], "easeOutExpo"), (9, [5, -30, 0]), (12, Z)]},
    "right_arm": rot((0, R0), (4, [-80, 75, 30], "easeOutQuad"), (6, [-85, -30, 0], "easeOutExpo"), (9, [-85, -30, 0]), (12, R0)),
    "right_item": rot((0, Z), (4, [20, 0, 90]), (6, [-20, 0, 90], "easeOutExpo"), (12, Z)),
    "left_leg": rot((0, Z), (6, [-20, 0, 0], "easeOutExpo"), (9, [-20, 0, 0]), (12, Z)),
})
anim("whip_lash", 16, {       # 鞭打ち（技）：大きく振りかぶって、遠くまで打つ
    "body": {"rotation": [(0, Z), (5, [-12, 25, 0], "easeOutQuad"), (8, [15, -20, 0], "easeOutExpo"), (12, [15, -20, 0]), (16, Z)],
             "position": [(0, Z), (5, [0, 1, 2]), (8, [0, -2, -4], "easeOutExpo"), (12, [0, -2, -4]), (16, Z)]},
    "right_arm": rot((0, R0), (5, [-200, 25, 15], "easeOutQuad"), (8, [-80, -10, 0], "easeOutExpo"), (12, [-80, -10, 0]), (16, R0)),
    "right_item": rot((0, Z), (5, [45, 0, 0], "easeOutQuad"), (8, [-35, 0, 0], "easeOutExpo"), (16, Z)),
    "left_arm": rot((0, L0), (5, [-60, -10, -20]), (8, [-15, 0, -40]), (12, [-15, 0, -40]), (16, L0)),
    "left_leg": rot((0, Z), (8, [-35, 0, 0], "easeOutExpo"), (12, [-35, 0, 0]), (16, Z)),
    "right_leg": rot((0, Z), (8, [25, 0, 0], "easeOutExpo"), (12, [25, 0, 0]), (16, Z)),
})
anim("whip_call", 20, {       # 相棒を呼ぶ：鞭を高く掲げて、頭の上でくるりと回す
    "right_arm": rot((0, R0), (4, [-170, 25, 10]), (7, [-170, -25, -5]), (10, [-170, 25, 10]), (13, [-170, -25, -5]), (16, [-165, 0, 5]), (20, R0)),
    "left_arm": rot((0, L0), (4, [-20, 0, -40]), (16, [-20, 0, -40]), (20, L0)),
    "head": rot((0, Z), (4, [-20, 0, 0]), (16, [-20, 0, 0]), (20, Z)),
})
anim("whip_point", 16, {      # 突撃：鞭で敵を指して「行けっ！」
    "body": {"rotation": [(0, Z), (4, [-5, 20, 0], "easeOutQuad"), (6, [12, -10, 0], "easeOutExpo"), (12, [12, -10, 0]), (16, Z)],
             "position": [(0, Z), (6, [0, -2, -3], "easeOutExpo"), (12, [0, -2, -3]), (16, Z)]},
    "right_arm": rot((0, R0), (4, [-170, 20, 10], "easeOutQuad"), (6, [-95, 0, 0], "easeOutExpo"), (12, [-95, 0, 0]), (16, R0)),
    "right_item": rot((0, Z), (4, [40, 0, 0]), (6, [-40, 0, 0], "easeOutExpo"), (16, Z)),
    "left_leg": rot((0, Z), (6, [-35, 0, 0], "easeOutExpo"), (12, [-35, 0, 0]), (16, Z)),
    "right_leg": rot((0, Z), (6, [25, 0, 0], "easeOutExpo"), (12, [25, 0, 0]), (16, Z)),
})
anim("whip_roar", 26, {       # 咆哮・野生解放・百獣の号令：両腕を広げて、天へ吠える
    "body": {"rotation": [(0, Z), (6, [-15, 0, 0], "easeOutQuad"), (20, [-15, 0, 0]), (26, Z)],
             "position": [(0, Z), (3, [0, -3, 0]), (6, [0, 1, 0], "easeOutQuad"), (20, [0, 1, 0]), (26, Z)]},
    "head": rot((0, Z), (6, [-40, 0, 0], "easeOutQuad"), (20, [-40, 0, 0]), (26, Z)),
    "right_arm": rot((0, R0), (3, [10, 0, 20]), (6, [-130, 0, 45], "easeOutQuad"), (20, [-132, 0, 48]), (26, R0)),
    "left_arm": rot((0, L0), (3, [10, 0, -20]), (6, [-130, 0, -45], "easeOutQuad"), (20, [-132, 0, -48]), (26, L0)),
    "right_leg": rot((0, Z), (6, [5, 0, 12]), (20, [5, 0, 12]), (26, Z)),
    "left_leg": rot((0, Z), (6, [-5, 0, -12]), (20, [-5, 0, -12]), (26, Z)),
})

# ================= 弓の技（右手で弓を前へ、左手で弦を引く） =================
# バニラの弓を引く腕：右 x -90 y -6、左 x -90 y +29。pitch で上へ向ける（x をさらにマイナス）
def bow_arms(pitch=0, pull=0.0, yaw=0):
    """pull 0 = 弦に手をかけただけ、1 = いっぱいに引いた。yaw は体のひねり（そのぶん腕を戻して、狙いはまっすぐ前のまま）"""
    return [-90 - pitch, -6 - yaw, 0], [-90 - pitch, 29 + 26 * pull - yaw, -8 * pull]

def bow_shots(times, pitch=0, end=None, draw=3, step_back=False):
    """times の tick に矢を放つ：放つ draw tick 前から引いて、放つと左手が後ろへはじける"""
    r, l, bd = [(0, R0)], [(0, L0)], [(0, Z)]
    for t in times:
        t0 = max(1, t - draw)
        ra, la = bow_arms(pitch, 0.1, 10); r.append((t0, ra, "easeOutQuad")); l.append((t0, la, "easeOutQuad")); bd.append((t0, [0, 10, 0]))
        ra, la = bow_arms(pitch, 1.0, 18); r.append((t, ra)); l.append((t, la)); bd.append((t, [0, 18, 0]))
        l.append((t + 1, [-80 - pitch, 75 - 14, -25], "easeOutExpo")); r.append((t + 1, [-92 - pitch, -8 - 14, 0], "easeOutExpo")); bd.append((t + 1, [-3, 14, 0], "easeOutExpo"))
    end = end or times[-1] + 8
    r.append((end, R0)); l.append((end, L0)); bd.append((end, Z))
    # 同じ tick が重なったら後のほうを使う
    def uniq(fr):
        d = {}
        for f in fr: d[f[0]] = f
        return [d[k] for k in sorted(d)]
    return {"right_arm": {"rotation": uniq(r)}, "left_arm": {"rotation": uniq(l)}, "body": {"rotation": uniq(bd)}}

def bow_anim(name, length, times, pitch=0, extra=None, draw=3):
    b = bow_shots(times, pitch, length, draw)
    for k, v in (extra or {}).items():
        if k == "body" and "body" in b:
            b["body"].update(v)
        else:
            b[k] = v
    anim(name, length, b)

bow_anim("bow_double", 14, [3, 7])                         # ダブルショット：すばやく2本
bow_anim("bow_shot", 12, [4], extra={                      # 爆裂矢など：しっかり引いて1本
    "left_leg": rot((0, Z), (4, [-15, 0, 0]), (8, [-15, 0, 0]), (12, Z))})
anim("bow_power_ready", 14, {                              # パワーショットのため：腰を落として、いっぱいに引きしぼる
    "body": {"rotation": [(0, Z), (8, [-5, 25, 0], "easeOutQuad"), (14, [-5, 26, 0])], "position": [(0, Z), (8, [0, -3, 1]), (14, [0, -3, 1])]},
    "right_arm": rot((0, R0), (8, bow_arms(0, 0, 26)[0], "easeOutQuad"), (14, bow_arms(0, 0, 26)[0])),
    "left_arm": rot((0, L0), (8, bow_arms(0, 1.3, 26)[1], "easeOutQuad"), (14, bow_arms(0, 1.35, 26)[1])),
    "left_leg": rot((0, Z), (8, [-25, 0, -8]), (14, [-25, 0, -8])),
    "right_leg": rot((0, Z), (8, [20, 0, 8]), (14, [20, 0, 8])),
})
anim("bow_release", 14, {                                  # 渾身の一射：はなすと、反動で少しのけぞる
    "body": {"rotation": [(0, [-5, 26, 0]), (1, [-12, 20, 0], "easeOutExpo"), (8, [-8, 18, 0]), (14, Z)],
             "position": [(0, [0, -3, 1]), (1, [0, -3, 3], "easeOutExpo"), (8, [0, -3, 2]), (14, Z)]},
    "right_arm": rot((0, bow_arms(0, 0, 26)[0]), (1, [-98, -28, 0], "easeOutExpo"), (8, [-95, -26, 0]), (14, R0)),
    "left_arm": rot((0, bow_arms(0, 1.35, 26)[1]), (1, [-75, 65, -30], "easeOutExpo"), (8, [-70, 65, -30]), (14, L0)),
    "left_leg": rot((0, [-25, 0, -8]), (8, [-25, 0, -8]), (14, Z)),
    "right_leg": rot((0, [20, 0, 8]), (8, [20, 0, 8]), (14, Z)),
})
bow_anim("bow_back_step", 14, [3], extra={                 # バックステップ：後ろへ跳びながら撃つ
    "body": {"position": [(0, Z), (3, [0, 3, 4], "easeOutQuad"), (8, [0, 0, 3]), (14, Z)]},
    "left_leg": rot((0, Z), (3, [-35, 0, 0]), (8, [-20, 0, 0]), (14, Z)),
    "right_leg": rot((0, Z), (3, [-10, 0, 0]), (8, [10, 0, 0]), (14, Z))}, draw=2)
bow_anim("bow_rain", 16, [5], pitch=50, extra={            # アローレイン：空へ向けて放つ
    "body": {"position": [(0, Z), (5, [0, -1, 0]), (16, Z)]},
    "head": rot((0, Z), (5, [-35, 0, 0]), (10, [-35, 0, 0]), (16, Z))}, draw=4)
anim("bow_snipe_ready", 26, {                              # スナイプのため：片ひざをついて、じっと狙う
    "body": {"rotation": [(0, Z), (8, [0, 22, 0], "easeOutQuad"), (26, [0, 22, 0])], "position": [(0, Z), (8, [0, -6, 0], "easeOutQuad"), (26, [0, -6, 0])]},
    "right_arm": rot((0, R0), (8, bow_arms(0, 0, 22)[0], "easeOutQuad"), (26, bow_arms(0, 0, 22)[0])),
    "left_arm": rot((0, L0), (8, bow_arms(0, 1.2, 22)[1], "easeOutQuad"), (26, bow_arms(0, 1.25, 22)[1])),
    "left_leg": rot((0, Z), (8, [-70, 0, -5], "easeOutQuad"), (26, [-70, 0, -5])),
    "right_leg": rot((0, Z), (8, [30, 0, 5], "easeOutQuad"), (26, [30, 0, 5])),
})
anim("bow_snipe", 16, {                                    # スナイプ：ひざをついたまま、鋭く放つ
    "body": {"rotation": [(0, [0, 22, 0]), (1, [-5, 18, 0], "easeOutExpo"), (10, [-3, 20, 0]), (16, Z)],
             "position": [(0, [0, -6, 0]), (10, [0, -6, 0]), (16, Z)]},
    "right_arm": rot((0, bow_arms(0, 0, 22)[0]), (1, [-95, -26, 0], "easeOutExpo"), (10, [-93, -26, 0]), (16, R0)),
    "left_arm": rot((0, bow_arms(0, 1.25, 22)[1]), (1, [-75, 60, -25], "easeOutExpo"), (10, [-75, 60, -25]), (16, L0)),
    "left_leg": rot((0, [-70, 0, -5]), (10, [-70, 0, -5]), (16, Z)),
    "right_leg": rot((0, [30, 0, 5]), (10, [30, 0, 5]), (16, Z)),
})
anim("bow_wind", 24, {                                     # 風読み：左手を空へかざして、風を感じる
    "left_arm": rot((0, L0), (6, [-150, 0, -30], "easeOutQuad"), (18, [-152, 0, -32]), (24, L0)),
    "right_arm": rot((0, R0), (6, [-20, 0, 15]), (18, [-20, 0, 15]), (24, R0)),
    "head": rot((0, Z), (6, [-25, 15, 0]), (18, [-25, -15, 0]), (24, Z)),
})
bow_anim("bow_storm", 104, list(range(4, 100, 5)), pitch=50, extra={  # アローストーム：空へ向けて、休まず撃ち続ける（5秒）
    "head": rot((0, Z), (4, [-35, 0, 0]), (100, [-35, 0, 0]), (104, Z)),
    "left_leg": rot((0, Z), (4, [-20, 0, -8]), (100, [-20, 0, -8]), (104, Z)),
    "right_leg": rot((0, Z), (4, [15, 0, 8]), (100, [15, 0, 8]), (104, Z))}, draw=2)
anim("bow_meteor_ready", 22, {                             # 流星の矢のため：大きく引きしぼって、光をためる
    "body": {"rotation": [(0, Z), (8, [-8, 28, 0], "easeOutQuad"), (22, [-8, 30, 0])], "position": [(0, Z), (8, [0, -3, 1]), (22, [0, -3, 1])]},
    "right_arm": rot((0, R0), (8, bow_arms(0, 0, 30)[0], "easeOutQuad"), (22, bow_arms(0, 0, 30)[0])),
    "left_arm": rot((0, L0), (8, bow_arms(0, 1.35, 30)[1], "easeOutQuad"), (22, bow_arms(0, 1.4, 30)[1])),
    "left_leg": rot((0, Z), (8, [-25, 0, -8]), (22, [-25, 0, -8])),
    "right_leg": rot((0, Z), (8, [20, 0, 8]), (22, [20, 0, 8])),
})
anim("bow_sky_ready", 32, {                                # 天穿のため：真上の空へ弓を向けて、引きしぼる
    "body": {"rotation": [(0, Z), (8, [-15, 0, 0], "easeOutQuad"), (32, [-15, 0, 0])], "position": [(0, Z), (8, [0, -2, 0]), (32, [0, -2, 0])]},
    "head": rot((0, Z), (8, [-55, 0, 0], "easeOutQuad"), (32, [-55, 0, 0])),
    "right_arm": rot((0, R0), (8, bow_arms(80, 0)[0], "easeOutQuad"), (32, bow_arms(80, 0)[0])),
    "left_arm": rot((0, L0), (8, bow_arms(70, 1.2)[1], "easeOutQuad"), (32, bow_arms(70, 1.25)[1])),
    "left_leg": rot((0, Z), (8, [-15, 0, -10]), (32, [-15, 0, -10])),
    "right_leg": rot((0, Z), (8, [15, 0, 10]), (32, [15, 0, 10])),
})
anim("bow_sky", 20, {                                      # 奥義・天穿：天へ放つ
    "body": {"rotation": [(0, [-15, 0, 0]), (1, [-20, 0, 0], "easeOutExpo"), (12, [-18, 0, 0]), (20, Z)], "position": [(0, [0, -2, 0]), (12, [0, -2, 0]), (20, Z)]},
    "head": rot((0, [-55, 0, 0]), (12, [-55, 0, 0]), (20, Z)),
    "right_arm": rot((0, bow_arms(80, 0)[0]), (1, [-175, -8, 0], "easeOutExpo"), (12, [-172, -8, 0]), (20, R0)),
    "left_arm": rot((0, bow_arms(70, 1.25)[1]), (1, [-120, 80, -30], "easeOutExpo"), (12, [-120, 80, -30]), (20, L0)),
    "left_leg": rot((0, [-15, 0, -10]), (12, [-15, 0, -10]), (20, Z)),
    "right_leg": rot((0, [15, 0, 10]), (12, [15, 0, 10]), (20, Z)),
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

# ================= 盾騎士（左手に盾、右手に剣） =================
# 盾を前に向けるときは、バニラでガードするときの腕（x -54、y +30）にする。腕を前に伸ばすと盾が横を向いてしまう
anim("shield_bash", 12, {     # シールドバッシュ：盾を前へ叩きつける
    "body": {"rotation": [(0, Z), (4, [-5, -25, 0], "easeOutQuad"), (6, [12, 20, 0], "easeOutExpo"), (9, [12, 20, 0]), (12, Z)],
             "position": [(0, Z), (4, [0, 0, 2]), (6, [0, -2, -4], "easeOutExpo"), (9, [0, -2, -4]), (12, Z)]},
    "left_arm": rot((0, L0), (4, [-25, 45, -5], "easeOutQuad"), (6, [-60, 30, 0], "easeOutExpo"), (9, [-60, 30, 0]), (12, L0)),
    "right_arm": rot((0, R0), (4, [-30, 20, 15]), (9, [-30, 20, 15]), (12, R0)),
    "left_leg": rot((0, Z), (6, [-35, 0, 0], "easeOutExpo"), (9, [-35, 0, 0]), (12, Z)),
    "right_leg": rot((0, Z), (6, [25, 0, 0], "easeOutExpo"), (9, [25, 0, 0]), (12, Z)),
})
anim("shield_taunt", 24, {    # 挑発：剣で盾をたたいて、敵を呼ぶ
    "left_arm": rot((0, L0), (4, [-54, 30, 0], "easeOutQuad"), (20, [-54, 30, 0]), (24, L0)),
    "right_arm": rot((0, R0), (4, [-120, -20, 0]), (6, [-75, -35, 0], "easeOutExpo"), (9, [-120, -20, 0]), (11, [-75, -35, 0], "easeOutExpo"),
                     (14, [-120, -20, 0]), (16, [-75, -35, 0], "easeOutExpo"), (20, [-70, -30, 0]), (24, R0)),
    "head": rot((0, Z), (4, [-10, 0, 0]), (20, [-10, 0, 0]), (24, Z)),
})
anim("shield_charge", 16, {   # シールドチャージ：盾を前に構えて突進
    "body": {"rotation": [(0, Z), (3, [20, 15, 0], "easeOutQuad"), (12, [20, 15, 0]), (16, Z)],
             "position": [(0, Z), (3, [0, -3, -3]), (12, [0, -3, -3]), (16, Z)]},
    "left_arm": rot((0, L0), (3, [-54, 30, 0], "easeOutQuad"), (12, [-54, 30, 0]), (16, L0)),
    "right_arm": rot((0, R0), (3, [-20, 20, 20]), (12, [-20, 20, 20]), (16, R0)),
    "left_leg": rot((0, Z), (3, [-45, 0, 0]), (7, [30, 0, 0]), (12, [-40, 0, 0]), (16, Z)),
    "right_leg": rot((0, Z), (3, [35, 0, 0]), (7, [-40, 0, 0]), (12, [35, 0, 0]), (16, Z)),
})
anim("shield_wall", 80, {     # 鉄壁の構え・聖盾の裁き：腰を落として、盾の後ろに身を隠す
    "body": {"rotation": [(0, Z), (4, [15, 0, 0]), (76, [15, 0, 0]), (80, Z)], "position": [(0, Z), (4, [0, -5, 0]), (76, [0, -5, 0]), (80, Z)]},
    "left_arm": rot((0, L0), (4, [-54, 30, 0], "easeOutQuad"), (76, [-54, 30, 0]), (80, L0)),
    "right_arm": rot((0, R0), (4, [-40, -20, 0]), (76, [-40, -20, 0]), (80, R0)),
    "left_leg": rot((0, Z), (4, [-40, 0, -10]), (76, [-40, 0, -10]), (80, Z)),
    "right_leg": rot((0, Z), (4, [30, 0, 10]), (76, [30, 0, 10]), (80, Z)),
})
anim("shield_raise", 24, {    # かばう・守護の陣・金剛不壊：剣と盾を高く掲げる
    "body": {"rotation": [(0, Z), (5, [-8, 0, 0], "easeOutQuad"), (19, [-8, 0, 0]), (24, Z)]},
    "head": rot((0, Z), (5, [-20, 0, 0]), (19, [-20, 0, 0]), (24, Z)),
    "right_arm": rot((0, R0), (5, [-165, 0, 15], "easeOutQuad"), (19, [-167, 0, 15]), (24, R0)),
    "right_item": rot((0, Z), (5, [-40, 0, 0]), (19, [-40, 0, 0]), (24, Z)),
    "left_arm": rot((0, L0), (5, [-120, 10, -25], "easeOutQuad"), (19, [-120, 10, -25]), (24, L0)),
})
anim("shield_plant", 20, {    # 城壁：盾を地面に突き立てる
    "body": {"rotation": [(0, Z), (5, [-5, 0, 0]), (8, [20, 0, 0], "easeInQuart"), (16, [20, 0, 0]), (20, Z)],
             "position": [(0, Z), (5, [0, 1, 0]), (8, [0, -4, -2], "easeInQuart"), (16, [0, -4, -2]), (20, Z)]},
    "left_arm": rot((0, L0), (5, [-100, 30, 0], "easeOutQuad"), (8, [-45, 30, 0], "easeInQuart"), (16, [-45, 30, 0]), (20, L0)),
    "right_arm": rot((0, R0), (5, [-30, 20, 20]), (16, [-30, 20, 20]), (20, R0)),
    "left_leg": rot((0, Z), (8, [-35, 0, 0], "easeInQuart"), (16, [-35, 0, 0]), (20, Z)),
    "right_leg": rot((0, Z), (8, [25, 0, 0], "easeInQuart"), (16, [25, 0, 0]), (20, Z)),
})

# 盾を体の正面へ寄せる：ガードの腕（y +30）のときは、盾を体の中心へ 5 ピクセルずらす
SHIELD_CENTER = 0
for _n in ("shield_bash", "shield_taunt", "shield_charge", "shield_wall", "shield_plant"):
    _la = ANIMS[_n]["bones"]["left_arm"]["rotation"]
    ANIMS[_n]["bones"]["left_item"] = {"position": {k: {"vector": [SHIELD_CENTER, 0, 0] if v["vector"][1] == 30 else [0, 0, 0], "easing": v["easing"]}
                                                    for k, v in _la.items()}}

# ================= ドッジロール（腰のあたりを中心に、体ごと1回転） =================
# body の回転は腰（高さ0.7）が中心。体を丸めて（腕・脚をたたむ）、しゃがみながら回る。
def roll(name, axis, sign):
    L = 12
    def rv(deg):
        v = [0, 0, 0]; v[axis] = sign * deg; return v
    tuck_arm = [-70, 0, 0]
    anim(name, L, {
        "body": {"rotation": [(0, Z), (2, rv(40), "linear"), (5, rv(160), "linear"), (8, rv(290), "linear"), (11, rv(360), "easeOutQuad"), (12, rv(360))],
                 "position": [(0, Z), (3, [0, -6, 0], "easeOutQuad"), (8, [0, -6, 0]), (12, Z, "easeOutQuad")]},
        "right_arm": rot((0, R0), (3, tuck_arm, "easeOutQuad"), (9, tuck_arm), (12, R0)),
        "left_arm": rot((0, L0), (3, tuck_arm, "easeOutQuad"), (9, tuck_arm), (12, L0)),
        "right_leg": rot((0, Z), (3, [-80, 0, 0], "easeOutQuad"), (9, [-80, 0, 0]), (12, Z)),
        "left_leg": rot((0, Z), (3, [-80, 0, 0], "easeOutQuad"), (9, [-80, 0, 0]), (12, Z)),
        "torso": rot((0, Z), (3, [35, 0, 0], "easeOutQuad"), (9, [35, 0, 0]), (12, Z)),
    })

roll("dodge_forward", 0, 1)
roll("dodge_back", 0, -1)
roll("dodge_left", 2, -1)
roll("dodge_right", 2, 1)

if __name__ == "__main__":
    out = os.path.join(os.path.dirname(__file__), "..", "src/main/resources/assets/jsn_craft/player_animations/weapons.json")
    with open(out, "w") as f:
        json.dump({"format_version": "1.8.0", "animations": ANIMS}, f, indent=1)
    print(len(ANIMS), "animations:", ", ".join(ANIMS))


# ================= 振る音のタイミング（anim/AnimSounds.java を作る） =================
STRIKE = {"easeOutQuart", "easeInQuart", "easeOutExpo"}
NO_SOUND = ("_ready", "_hold", "cast", "_parry", "_lunge", "dagger_skill", "bow_")

def sound_kind(n):
    if any(k in n for k in ("_flash", "_leap", "_moon")) or n.startswith(("axe_", "shield_")): return "heavy"
    if n.startswith("katana_"): return "katana"
    if n.startswith(("sword_", "staff_")): return "sword"
    if n.startswith(("dagger_", "fist_")): return "light"
    if n.startswith("spear_"): return "thrust"
    if n.startswith("whip_"): return "whip"
    return None

def strike_ticks(n, a):
    if n.endswith("_whirl"): return [3, 8, 13] if n.startswith(("sword", "katana")) else [3, 9, 15]
    if n == "axe_frenzy": return list(range(4, 76, 8))
    bone = "left_arm" if n.endswith("_left") else "right_arm"
    rot = a["bones"].get(bone, {}).get("rotation")
    if not rot: return []
    keys = sorted(rot, key=float)
    out = []
    for i in range(1, len(keys)):
        if rot[keys[i]]["easing"] in STRIKE:
            out.append(max(0, int(round(float(keys[i - 1]) * 20))))
    return out

def write_sounds_java():
    rows = []
    for n, a in ANIMS.items():
        if any(x in n for x in NO_SOUND): continue
        k = sound_kind(n)
        t = strike_ticks(n, a)
        if k and t: rows.append((n, k, t))
    body = ",\n".join(f'            Map.entry("{n}", new Entry("{k}", new int[]{{{", ".join(map(str, t))}}}))' for n, k, t in rows)
    java = f'''package com.chigusa.jsncraft.anim;

import java.util.Map;

/**
 * 体の動きごとの「振る音」：何tick目に、どの音を鳴らすか。
 * tools/gen_player_anims.py が作るファイル（手で直さない）。
 */
public final class AnimSounds {{

    public record Entry(String kind, int[] ticks) {{}}

    public static final Map<String, Entry> SOUNDS = Map.ofEntries(
{body});

    private AnimSounds() {{}}
}}
'''
    out = os.path.join(os.path.dirname(__file__), "..", "src/main/java/com/chigusa/jsncraft/anim/AnimSounds.java")
    open(out, "w").write(java)
    return len(rows)

if __name__ == "__main__":
    print("sounds:", write_sounds_java())
