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
            Map.entry("axe_chop_1", new Entry("heavy", new int[]{4})),
            Map.entry("axe_chop_2", new Entry("heavy", new int[]{4})),
            Map.entry("axe_skill", new Entry("heavy", new int[]{6})),
            Map.entry("spear_thrust", new Entry("thrust", new int[]{3})),
            Map.entry("spear_sweep", new Entry("thrust", new int[]{4})),
            Map.entry("spear_skill", new Entry("thrust", new int[]{11})),
            Map.entry("dagger_right", new Entry("light", new int[]{2})),
            Map.entry("dagger_left", new Entry("light", new int[]{2})),
            Map.entry("fist_right", new Entry("light", new int[]{1})),
            Map.entry("fist_left", new Entry("light", new int[]{1})),
            Map.entry("fist_skill", new Entry("light", new int[]{3})),
            Map.entry("whip_crack", new Entry("whip", new int[]{3})),
            Map.entry("whip_skill", new Entry("whip", new int[]{8})),
            Map.entry("shield_bash", new Entry("heavy", new int[]{3})));

    private AnimSounds() {}
}
