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

    /** スキルで与えるダメージ +○%（0.05 = 5%） */
    public static final DeferredHolder<Attribute, Attribute> SKILL_DAMAGE = register("skill_damage", 0, 0, 10);
    /** スキルの待ち時間 -○%（最大50%） */
    public static final DeferredHolder<Attribute, Attribute> COOLDOWN_REDUCTION = register("cooldown_reduction", 0, 0, 0.5);
    /** スキルの消費ST -○%（最大50%） */
    public static final DeferredHolder<Attribute, Attribute> STAMINA_COST_REDUCTION = register("stamina_cost_reduction", 0, 0, 0.5);
    /** 受けるダメージ -○%（最大60%） */
    public static final DeferredHolder<Attribute, Attribute> DAMAGE_REDUCTION = register("damage_reduction", 0, 0, 0.6);
    /** 吸血：与えたダメージの○%だけHPが回復 */
    public static final DeferredHolder<Attribute, Attribute> LIFESTEAL = register("lifesteal", 0, 0, 1);
    /** 職業経験値 +○% */
    public static final DeferredHolder<Attribute, Attribute> JOB_XP_BONUS = register("job_xp_bonus", 0, 0, 10);

    /** ボス（最大HPが100以上の相手）へのダメージ +○% */
    public static final DeferredHolder<Attribute, Attribute> BOSS_DAMAGE = register("boss_damage", 0, 0, 10);
    /** 1秒あたりのHP自然回復 */
    public static final DeferredHolder<Attribute, Attribute> HP_REGEN = register("hp_regen", 0, 0, 100);

    /** 道具の長持ち：ブロックを壊したとき、○%の確率で道具の耐久が減らない */
    public static final DeferredHolder<Attribute, Attribute> TOOL_DURABILITY = register("tool_durability", 0, 0, 1);

    /** 釣り：キャッチ枠の大きさ +○% */
    public static final DeferredHolder<Attribute, Attribute> FISHING_ZONE = register("fishing_zone", 0, 0, 1);
    /** 釣り：魚の動き -○%（最大60%） */
    public static final DeferredHolder<Attribute, Attribute> FISHING_CALM = register("fishing_calm", 0, 0, 0.6);
    /** 釣り：釣りゲージの溜まる速さ +○% */
    public static final DeferredHolder<Attribute, Attribute> FISHING_REEL = register("fishing_reel", 0, 0, 3);
    /** 釣り：釣った物がお宝に化ける確率 */
    public static final DeferredHolder<Attribute, Attribute> FISHING_TREASURE = register("fishing_treasure", 0, 0, 0.5);
    /** 釣り：魚がかかるまでの速さ +○%（最大80%） */
    public static final DeferredHolder<Attribute, Attribute> FISHING_BITE = register("fishing_bite", 0, 0, 0.8);

    /** 収穫の幸運：実った作物のドロップが増える確率。0.25 なら 25% で2倍 */
    public static final DeferredHolder<Attribute, Attribute> HARVEST_FORTUNE = register("harvest_fortune", 0, 0, 10);

    /** 料理の腕前：料理の品質が上がりやすくなる */
    public static final DeferredHolder<Attribute, Attribute> COOKING_MASTERY = register("cooking_mastery", 0, 0, 1);

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
        event.add(EntityType.PLAYER, SKILL_DAMAGE);
        event.add(EntityType.PLAYER, COOLDOWN_REDUCTION);
        event.add(EntityType.PLAYER, STAMINA_COST_REDUCTION);
        event.add(EntityType.PLAYER, DAMAGE_REDUCTION);
        event.add(EntityType.PLAYER, LIFESTEAL);
        event.add(EntityType.PLAYER, JOB_XP_BONUS);
        event.add(EntityType.PLAYER, BOSS_DAMAGE);
        event.add(EntityType.PLAYER, HP_REGEN);
        event.add(EntityType.PLAYER, TOOL_DURABILITY);
        event.add(EntityType.PLAYER, FISHING_ZONE);
        event.add(EntityType.PLAYER, FISHING_CALM);
        event.add(EntityType.PLAYER, FISHING_REEL);
        event.add(EntityType.PLAYER, FISHING_TREASURE);
        event.add(EntityType.PLAYER, FISHING_BITE);
        event.add(EntityType.PLAYER, HARVEST_FORTUNE);
        event.add(EntityType.PLAYER, COOKING_MASTERY);
    }

    private ModAttributes() {}
}
