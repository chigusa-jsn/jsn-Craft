package com.chigusa.jsncraft.anim;

import com.chigusa.jsncraft.network.AnimPayloads;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.weapon.DaggerItem;
import com.chigusa.jsncraft.weapon.JobSpearItem;
import com.chigusa.jsncraft.weapon.KatanaItem;
import com.chigusa.jsncraft.weapon.StaffItem;
import com.chigusa.jsncraft.weapon.WhipItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/**
 * 武器の振り・技のときの体の動き（サーバー側）。動きそのものは assets/jsn_craft/player_animations/weapons.json。
 * 見せるのはクライアント（client/PlayerAnimClient）。
 */
public final class PlayerAnims {

    /** 剣の技（効果の名前 → 動きの名前。刀は katana_、剣は sword_ をつける） */
    static final Map<String, String> SWORD_SKILLS = Map.of("sweep", "sweep", "dash", "lunge", "whirlwind", "whirl", "parry", "parry",
            "blink_slash", "iai", "slash_line", "flash", "leap_slam", "leap", "flurry", "flurry", "moon_fall", "moon");
    /** ため（詠唱）のある剣の技の、ため中の動き */
    static final Map<String, String> SWORD_READY = Map.of("blink_slash", "iai_ready", "slash_line", "flash_ready", "moon_fall", "moon_ready");

    /** 動きの名前として受け付けるか（まわりへ送る前に確かめる） */
    static boolean validName(String n) {
        return n != null && n.length() <= 32 && n.matches("[a-z0-9_]+");
    }

    /** 本人とまわりの人に、この動きを見せる */
    public static void play(ServerPlayer p, String anim) {
        if (anim == null) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new AnimPayloads.Play(p.getId(), anim));
    }

    /** 本人が武器を振った → まわりの人にも見せる（本人はもう動いている） */
    public static void onSwing(ServerPlayer p, String anim) {
        if (!validName(anim)) return;
        PacketDistributor.sendToPlayersTrackingEntity(p, new AnimPayloads.Play(p.getId(), anim));
    }

    /** 技を出したときの動き（持っている武器で決まる） */
    public static void skill(ServerPlayer p, SkillDefinition s) {
        String e = s.effect();
        if (e.startsWith("bow_")) return; // 弓はバニラの引く動きのまま
        if (e.startsWith("guard_")) {
            play(p, "shield_bash");
            return;
        }
        Item item = p.getMainHandItem().getItem();
        if (item instanceof SwordItem && SWORD_SKILLS.containsKey(e)) {
            play(p, (item instanceof KatanaItem ? "katana_" : "sword_") + SWORD_SKILLS.get(e));
            return;
        }
        play(p, skillAnim(item, p.getMainHandItem().isEmpty()));
    }

    /** ため（詠唱）が始まったときの動き */
    public static void charge(ServerPlayer p, SkillDefinition s) {
        Item item = p.getMainHandItem().getItem();
        if (item instanceof StaffItem) play(p, "cast_charge");
        else if (item instanceof SwordItem && SWORD_READY.containsKey(s.effect()))
            play(p, (item instanceof KatanaItem ? "katana_" : "sword_") + SWORD_READY.get(s.effect()));
    }

    static String skillAnim(Item i, boolean empty) {
        if (i instanceof StaffItem) return "cast";
        if (i instanceof DaggerItem) return "dagger_skill";
        if (i instanceof WhipItem) return "whip_skill";
        if (i instanceof SwordItem) return i instanceof KatanaItem ? "katana_whirl" : "sword_whirl";
        if (i instanceof AxeItem) return "axe_skill";
        if (i instanceof JobSpearItem || i instanceof TridentItem) return "spear_skill";
        if (empty) return "fist_skill";
        return null;
    }

    private PlayerAnims() {}
}
