package com.chigusa.jsncraft.sound;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

/** jsn-Craft の音。振る音は tools/gen_swing_sounds.py で作ったもの。 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, JsnCraft.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> SWING_LIGHT = swing("light");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWING_SWORD = swing("sword");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWING_KATANA = swing("katana");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWING_HEAVY = swing("heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWING_THRUST = swing("thrust");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWING_WHIP = swing("whip");

    /** 種類の名前 → 音 */
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> SWING = Map.of("light", SWING_LIGHT, "sword", SWING_SWORD,
            "katana", SWING_KATANA, "heavy", SWING_HEAVY, "thrust", SWING_THRUST, "whip", SWING_WHIP);

    private static DeferredHolder<SoundEvent, SoundEvent> swing(String kind) {
        return SOUNDS.register("swing." + kind, () -> SoundEvent.createVariableRangeEvent(JsnCraft.id("swing." + kind)));
    }

    private ModSounds() {}
}
