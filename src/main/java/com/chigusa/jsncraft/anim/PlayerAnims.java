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

    /** 剣以外の技（効果の名前 → 動きの名前） */
    static final Map<String, String> SKILL_ANIMS = Map.ofEntries(
            // 斧
            Map.entry("axe_cleave", "axe_chop_1"), Map.entry("axe_whirl", "axe_whirl"), Map.entry("axe_throw", "axe_throw"),
            Map.entry("axe_war_cry", "axe_cry"), Map.entry("axe_rage", "axe_cry"), Map.entry("axe_fissure", "axe_slam"),
            Map.entry("axe_sunder", "axe_sunder"), Map.entry("axe_siege", "axe_charge"), Map.entry("axe_frenzy", "axe_frenzy"),
            Map.entry("axe_sky_breaker", "axe_sky"),
            // 槍
            Map.entry("spear_thrust", "spear_lunge"), Map.entry("spear_sweep", "spear_wide"), Map.entry("spear_throw", "spear_throw"),
            Map.entry("spear_leap", "spear_leap"), Map.entry("spear_triple", "spear_triple"), Map.entry("spear_whirlpool", "spear_twirl"),
            Map.entry("spear_wall", "spear_brace"), Map.entry("spear_thousand", "spear_raise"), Map.entry("spear_maelstrom", "spear_twirl"),
            Map.entry("spear_poseidon", "spear_raise"),
            // 短剣
            Map.entry("dagger_twin", "dagger_twin"), Map.entry("dagger_shadow_step", "dagger_crouch"), Map.entry("dagger_poison", "dagger_coat"),
            Map.entry("dagger_smoke", "dagger_crouch"), Map.entry("dagger_knives", "dagger_throw"), Map.entry("dagger_stealth", "dagger_crouch"),
            Map.entry("dagger_assassinate", "dagger_cross"), Map.entry("dagger_dance", "dagger_spin"), Map.entry("dagger_thousand", "dagger_flurry"),
            Map.entry("dagger_death_mark", "dagger_point"), Map.entry("dagger_netherworld", "dagger_spin"),
            // 拳
            Map.entry("fist_rush", "fist_rush"), Map.entry("fist_straight", "fist_straight"), Map.entry("fist_dash", "fist_dash"),
            Map.entry("fist_uppercut", "fist_uppercut"), Map.entry("fist_ki_gather", "fist_ki"), Map.entry("fist_whirl_kick", "fist_kick"),
            Map.entry("fist_double", "fist_double"), Map.entry("fist_qigong", "fist_ki"), Map.entry("fist_hundred", "fist_hundred"),
            Map.entry("fist_stomp", "fist_stomp"), Map.entry("fist_overlord", "fist_overlord"),
            // 鞭（獣使い）
            // 勝負札（ギャンブラー）
            Map.entry("gamble_card_throw", "dagger_throw"), Map.entry("gamble_coin_toss", "dagger_point"), Map.entry("gamble_dice_shot", "dagger_throw"),
            Map.entry("gamble_chip_guard", "dagger_coat"), Map.entry("gamble_roulette", "dagger_spin"), Map.entry("gamble_double_up", "dagger_point"),
            Map.entry("gamble_poker_hand", "dagger_cross"), Map.entry("gamble_all_in", "dagger_crouch"), Map.entry("gamble_slot_burst", "dagger_twin"),
            Map.entry("gamble_jackpot_rain", "dagger_point"), Map.entry("gamble_fate_dice", "dagger_throw"), Map.entry("gamble_house_rules", "dagger_coat"),
            Map.entry("gamble_royal_flush", "dagger_flurry"), Map.entry("gamble_seven_heaven", "dagger_spin"),
            Map.entry("beast_lash", "whip_lash"), Map.entry("beast_charge", "whip_point"), Map.entry("beast_rally", "whip_call"),
            Map.entry("beast_roar", "whip_roar"), Map.entry("beast_frenzy", "whip_roar"), Map.entry("beast_king_command", "whip_roar"),
            // 弓
            Map.entry("bow_double_shot", "bow_double"), Map.entry("bow_power_shot", "bow_release"), Map.entry("bow_back_step", "bow_back_step"),
            Map.entry("bow_arrow_rain", "bow_rain"), Map.entry("bow_blast_arrow", "bow_shot"), Map.entry("bow_snipe", "bow_snipe"),
            Map.entry("bow_wind_read", "bow_wind"), Map.entry("bow_arrow_storm", "bow_storm"), Map.entry("bow_meteor_arrow", "bow_release"),
            Map.entry("bow_sky_pierce", "bow_sky"),
            // 盾騎士
            Map.entry("guard_bash", "shield_bash"), Map.entry("guard_taunt", "shield_taunt"), Map.entry("guard_charge", "shield_charge"),
            Map.entry("guard_iron_wall", "shield_wall"), Map.entry("guard_cover", "shield_raise"), Map.entry("guard_formation", "shield_raise"),
            Map.entry("guard_rampart", "shield_plant"), Map.entry("guard_judgment", "shield_wall"), Map.entry("guard_adamant", "shield_raise"));
    /** ため（詠唱）のある技の、ため中の動き */
    static final Map<String, String> READY_ANIMS = Map.ofEntries(Map.entry("bow_power_shot", "bow_power_ready"), Map.entry("bow_snipe", "bow_snipe_ready"),
            Map.entry("bow_meteor_arrow", "bow_meteor_ready"), Map.entry("bow_sky_pierce", "bow_sky_ready"),
            Map.entry("axe_sunder", "axe_sunder_ready"), Map.entry("axe_sky_breaker", "axe_sky_ready"), Map.entry("spear_poseidon", "spear_raise_ready"), Map.entry("dagger_assassinate", "dagger_crouch"), Map.entry("dagger_netherworld", "dagger_crouch"), Map.entry("fist_double", "fist_double_ready"), Map.entry("fist_overlord", "fist_overlord_ready"), Map.entry("guard_adamant", "shield_raise"));

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

    /** 技を出したときの動き（技ごとに決まっている。決まっていなければ持っている武器で） */
    public static void skill(ServerPlayer p, SkillDefinition s) {
        String e = s.effect();
        Item item = p.getMainHandItem().getItem();
        if (item instanceof SwordItem && SWORD_SKILLS.containsKey(e)) {
            play(p, (item instanceof KatanaItem ? "katana_" : "sword_") + SWORD_SKILLS.get(e));
            return;
        }
        if (SKILL_ANIMS.containsKey(e)) {
            play(p, SKILL_ANIMS.get(e));
            return;
        }
        if (e.startsWith("beast_call")) {
            play(p, "whip_call");
            return;
        }
        play(p, skillAnim(item, p.getMainHandItem().isEmpty()));
    }

    /** ため（詠唱）が始まったときの動き */
    public static void charge(ServerPlayer p, SkillDefinition s) {
        Item item = p.getMainHandItem().getItem();
        if (item instanceof StaffItem) play(p, "cast_charge");
        else if (READY_ANIMS.containsKey(s.effect())) play(p, READY_ANIMS.get(s.effect()));
        else if (item instanceof SwordItem && SWORD_READY.containsKey(s.effect()))
            play(p, (item instanceof KatanaItem ? "katana_" : "sword_") + SWORD_READY.get(s.effect()));
    }

    static String skillAnim(Item i, boolean empty) {
        if (i instanceof StaffItem) return "cast";
        if (i instanceof DaggerItem) return "dagger_cross";
        if (i instanceof WhipItem) return "whip_lash";
        if (i instanceof com.chigusa.jsncraft.weapon.CardDeckItem) return "dagger_throw";
        if (i instanceof SwordItem) return i instanceof KatanaItem ? "katana_whirl" : "sword_whirl";
        if (i instanceof AxeItem) return "axe_chop_1";
        if (i instanceof JobSpearItem || i instanceof TridentItem) return "spear_lunge";
        if (empty) return "fist_straight";
        return null;
    }

    private PlayerAnims() {}
}
