package com.chigusa.jsncraft.weapon;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.smithing.SmithData;
import com.chigusa.jsncraft.stat.ModAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.Map;

/** 職業武器：杖の魔法の能力値、月銀・星銀の鍛冶型の入手（ボスと宝箱）。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class WeaponEvents {

    // ================= 杖の能力値 =================

    @SubscribeEvent
    public static void onModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() instanceof JobAxeItem axe) {
            event.addModifier(ModAttributes.SKILL_DAMAGE, new AttributeModifier(JsnCraft.id("job_axe/skill"), axe.spec().skill(),
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            return;
        }
        if (stack.getItem() instanceof JobSwordItem sword) {
            event.addModifier(ModAttributes.SKILL_DAMAGE, new AttributeModifier(JsnCraft.id("job_sword/skill"), sword.spec().skill(),
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            return;
        }
        if (!(stack.getItem() instanceof StaffItem staff)) return;
        StaffItem.Tier t = staff.tier();
        int plus = SmithData.plus(stack);
        event.addModifier(ModAttributes.MAGIC_POWER, new AttributeModifier(JsnCraft.id("staff/magic"), t.magic() + 0.02 * plus,
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        event.addModifier(ModAttributes.MAX_MANA, new AttributeModifier(JsnCraft.id("staff/mana"), t.mana(),
                AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
    }

    // ================= 宝箱 =================

    /** 宝箱 → (月銀の鍛冶型の確率, 星銀の鍛冶型の確率) */
    private static final Map<ResourceKey<LootTable>, float[]> CHESTS = Map.of(
            BuiltInLootTables.ANCIENT_CITY, new float[]{0.08F, 0.015F},
            BuiltInLootTables.END_CITY_TREASURE, new float[]{0.06F, 0.03F},
            BuiltInLootTables.BASTION_TREASURE, new float[]{0.10F, 0.0F},
            BuiltInLootTables.STRONGHOLD_LIBRARY, new float[]{0.05F, 0.0F},
            BuiltInLootTables.WOODLAND_MANSION, new float[]{0.05F, 0.01F});

    @SubscribeEvent
    public static void onLootLoad(LootTableLoadEvent event) {
        for (var e : CHESTS.entrySet()) {
            if (!e.getKey().location().equals(event.getName())) continue;
            float[] ch = e.getValue();
            add(event.getTable(), "moon_template", ModWeapons.TEMPLATE_MOON.get(), ch[0]);
            add(event.getTable(), "star_template", ModWeapons.TEMPLATE_STAR.get(), ch[1]);
        }
    }

    private static void add(LootTable table, String name, Item item, float chance) {
        if (chance <= 0) return;
        table.addPool(LootPool.lootPool().name(JsnCraft.MODID + ":" + name).setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(item)).when(LootItemRandomChanceCondition.randomChance(chance)).build());
    }

    // ================= ボス =================

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        var e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) return;
        var r = e.getRandom();
        Item drop = null;
        if (e instanceof Warden || e instanceof WitherBoss) drop = ModWeapons.TEMPLATE_MOON.get();
        else if (e instanceof ElderGuardian && r.nextFloat() < 0.5F) drop = ModWeapons.TEMPLATE_MOON.get();
        if (e instanceof WitherBoss && r.nextFloat() < 0.25F) {
            event.getDrops().add(new ItemEntity(level, e.getX(), e.getY(), e.getZ(), new ItemStack(ModWeapons.TEMPLATE_STAR.get())));
        }
        if (drop != null) event.getDrops().add(new ItemEntity(level, e.getX(), e.getY(), e.getZ(), new ItemStack(drop)));
    }

    /** エンダードラゴンはドロップがないので、近くで戦った人みんなに星銀の鍛冶型 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !(dragon.level() instanceof ServerLevel level)) return;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(dragon) > 200 * 200) continue;
            ItemStack stack = new ItemStack(ModWeapons.TEMPLATE_STAR.get());
            if (!p.getInventory().add(stack)) p.drop(stack, false);
            p.displayClientMessage(Component.literal("星銀の鍛冶型を手に入れた！").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        }
    }

    private WeaponEvents() {}
}
