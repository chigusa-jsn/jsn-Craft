package com.chigusa.jsncraft.anim;

import com.chigusa.jsncraft.network.AnimPayloads;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.weapon.DaggerItem;
import com.chigusa.jsncraft.weapon.JobSpearItem;
import com.chigusa.jsncraft.weapon.StaffItem;
import com.chigusa.jsncraft.weapon.WhipItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

/**
 * 武器の振り・技のときの体の動き（サーバー側）。動きそのものは assets/jsn_craft/player_animations/weapons.json。
 * 見せるのはクライアント（client/PlayerAnimClient）。
 */
public final class PlayerAnims {

    public static final Set<String> NAMES = Set.of(
            "sword_slash_1", "sword_slash_2", "sword_slash_3", "sword_skill",
            "axe_chop_1", "axe_chop_2", "axe_skill",
            "spear_thrust", "spear_sweep", "spear_skill",
            "dagger_right", "dagger_left", "dagger_skill",
            "fist_right", "fist_left", "fist_skill",
            "whip_crack", "whip_skill",
            "staff_strike", "cast", "cast_charge", "shield_bash");

    /** 本人とまわりの人に、この動きを見せる */
    public static void play(ServerPlayer p, String anim) {
        if (anim == null) return;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new AnimPayloads.Play(p.getId(), anim));
    }

    /** 本人が武器を振った → まわりの人にも見せる（本人はもう動いている） */
    public static void onSwing(ServerPlayer p, String anim) {
        if (!NAMES.contains(anim)) return;
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
        play(p, skillAnim(p.getMainHandItem().getItem(), p.getMainHandItem().isEmpty()));
    }

    static String skillAnim(Item i, boolean empty) {
        if (i instanceof StaffItem) return "cast";
        if (i instanceof DaggerItem) return "dagger_skill";
        if (i instanceof WhipItem) return "whip_skill";
        if (i instanceof SwordItem) return "sword_skill";
        if (i instanceof AxeItem) return "axe_skill";
        if (i instanceof JobSpearItem || i instanceof TridentItem) return "spear_skill";
        if (empty) return "fist_skill";
        return null;
    }

    private PlayerAnims() {}
}
