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


# ================= 振る音のタイミング（anim/AnimSounds.java を作る） =================
STRIKE = {"easeOutQuart", "easeInQuart", "easeOutExpo"}
NO_SOUND = ("_ready", "_hold", "cast", "_parry", "_lunge", "dagger_skill")

def sound_kind(n):
    if any(k in n for k in ("_flash", "_leap", "_moon")) or n.startswith(("axe_", "shield_")): return "heavy"
    if n.startswith("katana_"): return "katana"
    if n.startswith(("sword_", "staff_")): return "sword"
    if n.startswith(("dagger_", "fist_")): return "light"
    if n.startswith("spear_"): return "thrust"
    if n.startswith("whip_"): return "whip"
    return None

def strike_ticks(n, a):
    if n.endswith("_whirl"): return [3, 8, 13]
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
