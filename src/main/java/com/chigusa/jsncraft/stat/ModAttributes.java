package com.chigusa.jsncraft.stat;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * jsn-Craft が追加する能力値。スキルボードでは "jsn_craft:max_mana" のように書く。
 */
@EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, JsnCraft.MODID);

    /** 最大マナ */
    public static final DeferredHolder<Attribute, Attribute> MAX_MANA = register("max_mana", 20, 0, 100000);
    /** 1秒あたりのマナ回復量 */
    public static final DeferredHolder<Attribute, Attribute> MANA_REGEN = register("mana_regen", 1, 0, 10000);
    /** 最大スタミナ */
    public static final DeferredHolder<Attribute, Attribute> MAX_STAMINA = register("max_stamina", 100, 0, 100000);
    /** 1秒あたりのスタミナ回復量 */
    public static final DeferredHolder<Attribute, Attribute> STAMINA_REGEN = register("stamina_regen", 1, 0, 10000);
    /** 採掘の幸運：鉱石のドロップが増える確率。0.25 なら 25% で2倍、1.5 なら必ず2倍＋50%で3倍 */
    public static final DeferredHolder<Attribute, Attribute> MINING_FORTUNE = register("mining_fortune", 0, 0, 10);
    /** 会心率：近接攻撃が会心になる確率（0.1 = 10%） */
    public static final DeferredHolder<Attribute, Attribute> CRIT_CHANCE = register("crit_chance", 0, 0, 1);
    /** 会心ダメージ：会心のときに増えるダメージの割合（0.5 = +50%） */
    public static final DeferredHolder<Attribute, Attribute> CRIT_DAMAGE = register("crit_damage", 0.5, 0, 100);

    private static DeferredHolder<Attribute, Attribute> register(String name, double def, double min, double max) {
        return ATTRIBUTES.register(name, () ->
                new RangedAttribute("attribute.name." + JsnCraft.MODID + "." + name, def, min, max).setSyncable(true));
    }

    @SubscribeEvent
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, MAX_MANA);
        event.add(EntityType.PLAYER, MANA_REGEN);
        event.add(EntityType.PLAYER, MAX_STAMINA);
        event.add(EntityType.PLAYER, STAMINA_REGEN);
        event.add(EntityType.PLAYER, MINING_FORTUNE);
        event.add(EntityType.PLAYER, CRIT_CHANCE);
        event.add(EntityType.PLAYER, CRIT_DAMAGE);
    }

    private ModAttributes() {}
}
