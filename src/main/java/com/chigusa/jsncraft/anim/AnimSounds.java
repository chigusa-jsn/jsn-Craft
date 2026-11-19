package com.chigusa.jsncraft.anim;

import java.util.Map;

/**
 * 体の動きごとの「振る音」：何tick目に、どの音を鳴らすか。
 * tools/gen_player_anims.py が作るファイル（手で直さない）。
 */
public final class AnimSounds {

    public record Entry(String kind, int[] ticks) {}

    public static final Map<String, Entry> SOUNDS = Map.ofEntries(
            Map.entry("sword_slash_1", new Entry("sword", new int[]{4})),
            Map.entry("sword_slash_2", new Entry("sword", new int[]{4})),
            Map.entry("sword_slash_3", new Entry("sword", new int[]{5})),
            Map.entry("sword_sweep", new Entry("sword", new int[]{4})),
            Map.entry("sword_whirl", new Entry("sword", new int[]{3, 8, 13})),
            Map.entry("sword_iai", new Entry("sword", new int[]{0})),
            Map.entry("sword_flash", new Entry("heavy", new int[]{0})),
            Map.entry("sword_leap", new Entry("heavy", new int[]{15})),
            Map.entry("sword_flurry", new Entry("sword", new int[]{0, 2, 5, 8, 11, 14, 17, 23})),
            Map.entry("sword_moon", new Entry("heavy", new int[]{0})),
            Map.entry("katana_slash_1", new Entry("katana", new int[]{4})),
            Map.entry("katana_slash_2", new Entry("katana", new int[]{4})),
            Map.entry("katana_slash_3", new Entry("katana", new int[]{5})),
            Map.entry("katana_sweep", new Entry("katana", new int[]{4})),
            Map.entry("katana_whirl", new Entry("katana", new int[]{3, 8, 13})),
            Map.entry("katana_flash", new Entry("heavy", new int[]{0})),
            Map.entry("katana_flurry", new Entry("katana", new int[]{0, 2, 5, 8, 11, 14, 17, 23})),
            Map.entry("katana_leap", new Entry("heavy", new int[]{15})),
            Map.entry("katana_moon", new Entry("heavy", new int[]{0})),
            Map.entry("katana_iai", new Entry("katana", new int[]{0})),
            Map.entry("axe_frenzy", new Entry("heavy", new int[]{4, 12, 20, 28, 36, 44, 52, 60, 68})),
            Map.entry("axe_throw", new Entry("heavy", new int[]{5})),
            Map.entry("axe_chop_1", new Entry("heavy", new int[]{5})),
            Map.entry("axe_chop_2", new Entry("heavy", new int[]{5})),
            Map.entry("axe_chop_3", new Entry("heavy", new int[]{6})),
            Map.entry("axe_whirl", new Entry("heavy", new int[]{3, 9, 15})),
            Map.entry("axe_slam", new Entry("heavy", new int[]{6})),
            Map.entry("axe_sunder", new Entry("heavy", new int[]{0})),
            Map.entry("axe_sky", new Entry("heavy", new int[]{16})),
            Map.entry("spear_throw", new Entry("thrust", new int[]{6})),
            Map.entry("spear_thrust", new Entry("thrust", new int[]{4})),
            Map.entry("spear_sweep", new Entry("thrust", new int[]{5})),
            Map.entry("spear_thrust_2", new Entry("thrust", new int[]{5})),
            Map.entry("spear_leap", new Entry("heavy", new int[]{15})),
            Map.entry("spear_triple", new Entry("thrust", new int[]{2, 6, 10})),
            Map.entry("spear_twirl", new Entry("thrust", new int[]{14})),
            Map.entry("spear_raise", new Entry("thrust", new int[]{0})),
            Map.entry("dagger_right", new Entry("light", new int[]{3})),
            Map.entry("dagger_left", new Entry("light", new int[]{3})),
            Map.entry("dagger_twin", new Entry("light", new int[]{2})),
            Map.entry("dagger_cross", new Entry("light", new int[]{4})),
            Map.entry("dagger_throw", new Entry("light", new int[]{4})),
            Map.entry("dagger_flurry", new Entry("light", new int[]{0, 1, 5, 9, 13, 17, 21, 25, 29, 33})),
            Map.entry("dagger_point", new Entry("light", new int[]{0})),
            Map.entry("fist_right", new Entry("light", new int[]{2})),
            Map.entry("fist_left", new Entry("light", new int[]{2})),
            Map.entry("fist_rush", new Entry("light", new int[]{0, 4, 10, 16})),
            Map.entry("fist_hundred", new Entry("light", new int[]{0, 3, 7, 11, 15, 19, 23, 27, 31, 35, 39, 43, 47, 51, 55})),
            Map.entry("fist_straight", new Entry("light", new int[]{5})),
            Map.entry("fist_dash", new Entry("light", new int[]{0})),
            Map.entry("fist_uppercut", new Entry("light", new int[]{4})),
            Map.entry("fist_double", new Entry("light", new int[]{0})),
            Map.entry("fist_stomp", new Entry("light", new int[]{6})),
            Map.entry("fist_overlord", new Entry("light", new int[]{0})),
            Map.entry("whip_crack", new Entry("whip", new int[]{4})),
            Map.entry("whip_side", new Entry("whip", new int[]{4})),
            Map.entry("whip_lash", new Entry("whip", new int[]{5})),
            Map.entry("whip_point", new Entry("whip", new int[]{4})),
            Map.entry("shield_taunt", new Entry("heavy", new int[]{4, 9, 14})));

    private AnimSounds() {}
}
