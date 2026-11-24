package com.chigusa.jsncraft.smithing;

import com.chigusa.jsncraft.JsnCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 耐久の仕組み：
 * ・武器・防具・道具は壊れても消えない（エリトラのように「壊れた」状態で残り、使えなくなる）
 * ・耐久が減ると切れ味（攻撃力・防御力）が落ちる
 * ・砥石・修理キットで直せるのは 80% まで。壊れた物と、80% より上は鍛冶師が鍛冶台で直す
 * ・修繕のエンチャントは無し（持ち物から消える）、作業台・金床・砥石での修理も無し
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class Durability {

    /** 砥石・修理キットで直せる上限 */
    public static final double KIT_CAP = 0.8;
    private static final EquipmentSlot[] SLOTS = EquipmentSlot.values();

    // ================= 状態 =================

    /** 壊れても消えない装備（強化できる武器・防具・道具） */
    public static boolean isGear(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxDamage() > 0 && !SmithData.kinds(stack).isEmpty();
    }

    public static boolean broken(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean("jsn_broken");
    }

    /** 残りの耐久の割合（0〜1、壊れていれば 0） */
    public static double ratio(ItemStack stack) {
        if (stack.getMaxDamage() <= 0) return 1;
        if (broken(stack)) return 0;
        return 1.0 - (double) stack.getDamageValue() / stack.getMaxDamage();
    }

    /** 切れ味の段階：3 青 / 2 緑 / 1 黄 / 0 赤 / -1 壊れた */
    public static int band(ItemStack stack) {
        if (broken(stack)) return -1;
        double r = ratio(stack);
        return r >= 0.7 ? 3 : r >= 0.4 ? 2 : r >= 0.2 ? 1 : 0;
    }

    /** 武器の攻撃力の倍率（青の上に、研ぎ澄ましの白・紫がある） */
    public static double weaponFactor(ItemStack stack) {
        return switch (band(stack)) {
            case 3 -> hone(stack) > 0 ? 1.0 + 0.1 * honeLevel(stack) : 1.0;
            case 2 -> 0.9;
            case 1 -> 0.75;
            case 0 -> 0.5;
            default -> 0.1;
        };
    }

    /** 防具の防御力の倍率 */
    public static double armorFactor(ItemStack stack) {
        if (broken(stack)) return 0;
        double r = ratio(stack);
        return r >= 0.4 ? 1.0 : r >= 0.2 ? 0.8 : 0.6;
    }

    /** 壊れた状態にする（耐久は残り 1、減らなくなる） */
    public static void breakGear(ItemStack stack) {
        stack.setDamageValue(stack.getMaxDamage() - 1);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putBoolean("jsn_broken", true));
        stack.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
    }

    /** 壊れた状態を直す（耐久は残り 1 から） */
    public static void unbreak(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.remove("jsn_broken"));
        stack.remove(DataComponents.UNBREAKABLE);
        stack.setDamageValue(stack.getMaxDamage() - 1);
    }

    /** 割合で直す（cap まで）。直った量（割合）を返す */
    public static double repair(ItemStack stack, double amount, double cap) {
        if (broken(stack)) unbreak(stack);
        double before = ratio(stack);
        double after = Math.min(cap, before + amount);
        if (after <= before) return 0;
        stack.setDamageValue(Math.max(0, (int) Math.floor(stack.getMaxDamage() * (1 - after))));
        return ratio(stack) - before;
    }

    private static void announceBroken(Player player, ItemStack stack) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.9F, 0.8F);
        player.displayClientMessage(Component.literal("✖ ").append(stack.getHoverName().copy())
                .append(" が壊れた！ 鍛冶師に直してもらおう").withStyle(ChatFormatting.RED), true);
    }

    /** 残りがわずかなら壊れた状態にする */
    private static void guard(Player player, ItemStack stack, int margin) {
        if (!isGear(stack) || broken(stack)) return;
        if (stack.getMaxDamage() - stack.getDamageValue() <= margin) {
            breakGear(stack);
            announceBroken(player, stack);
        }
    }

    // ================= 壊れないようにする =================

    private static final Map<UUID, List<ItemStack>> PENDING = new HashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        for (EquipmentSlot s : SLOTS) guard(p, p.getItemBySlot(s), 2);
        List<ItemStack> back = PENDING.remove(p.getUUID());
        if (back != null) for (ItemStack st : back) if (!p.getInventory().add(st)) p.drop(st, false);
        if (p.tickCount % 40 == 0) stripMending(p);
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        int margin = (int) Math.max(1, event.getAmount() / 4) + 2;
        for (EquipmentSlot s : SLOTS) if (s.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) guard(p, p.getItemBySlot(s), margin);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) guard(p, p.getMainHandItem(), 3);
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer p) guard(p, p.getMainHandItem(), 3);
    }

    /** それでも壊れてしまったら、壊れた状態で手元に戻す */
    @SubscribeEvent
    public static void onDestroyed(PlayerDestroyItemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        ItemStack orig = event.getOriginal();
        if (orig.getMaxDamage() <= 0 || SmithData.kinds(orig).isEmpty()) return;
        ItemStack copy = orig.copyWithCount(1);
        breakGear(copy);
        PENDING.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(copy);
        announceBroken(p, copy);
    }

    /** 壊れた弓・クロスボウ・三叉槍などは使えない */
    @SubscribeEvent
    public static void onUse(LivingEntityUseItemEvent.Start event) {
        if (broken(event.getItem())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player p && !p.level().isClientSide)
                p.displayClientMessage(Component.literal("壊れていて使えない…鍛冶師に直してもらおう").withStyle(ChatFormatting.RED), true);
        }
    }

    /** 壊れた盾では防げない */
    @SubscribeEvent
    public static void onShield(net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent event) {
        if (event.getBlocked() && broken(event.getEntity().getUseItem())) event.setBlocked(false);
    }

    // ================= 研ぎ澄まし（切れ味の上限：白・紫） =================

    public static int hone(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("jsn_hone");
    }

    /** 1 白（攻撃 110%）/ 2 紫（攻撃 120%） */
    public static int honeLevel(ItemStack stack) {
        return Math.max(1, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("jsn_hone_lv"));
    }

    public static void setHone(ItemStack stack, int hits, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
            if (hits > 0) {
                t.putInt("jsn_hone", hits);
                t.putInt("jsn_hone_lv", level);
            } else {
                t.remove("jsn_hone");
                t.remove("jsn_hone_lv");
            }
        });
    }

    /** 研ぎ澄ました武器で殴ると、回数が減る */
    @SubscribeEvent
    public static void onHoneHit(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof ServerPlayer p)) return;
        ItemStack w = p.getMainHandItem();
        int h = hone(w);
        if (h <= 0) return;
        setHone(w, h - 1, honeLevel(w));
        if (h - 1 <= 0) p.displayClientMessage(Component.literal("研ぎ澄ましが切れた").withStyle(ChatFormatting.GRAY), true);
    }

    // ================= 当て板 =================

    public static int plate(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("jsn_plate");
    }

    /** 1 当て板 / 2 鉄の当て板 */
    public static int plateLevel(ItemStack stack) {
        return Math.max(1, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("jsn_plate_lv"));
    }

    public static void setPlate(ItemStack stack, int hits) {
        setPlate(stack, hits, plateLevel(stack));
    }

    public static void setPlate(ItemStack stack, int hits, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> {
            if (hits > 0) {
                t.putInt("jsn_plate", hits);
                t.putInt("jsn_plate_lv", level);
            } else {
                t.remove("jsn_plate");
                t.remove("jsn_plate_lv");
            }
        });
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getNewDamage() <= 0) return;
        for (EquipmentSlot s : SLOTS) {
            if (s.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack st = p.getItemBySlot(s);
            int hits = plate(st);
            if (hits <= 0) continue;
            setPlate(st, hits - 1);
            if (hits - 1 <= 0) {
                p.level().playSound(null, p.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.7F, 1.2F);
                p.displayClientMessage(Component.literal("当て板が外れた：").append(st.getHoverName().copy()).withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    // ================= 修繕なし・ほかの修理なし =================

    private static boolean hasMending(ItemEnchantments e) {
        for (var h : e.keySet()) if (h.is(Enchantments.MENDING)) return true;
        return false;
    }

    public static void stripMending(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            boolean book = st.has(DataComponents.STORED_ENCHANTMENTS);
            ItemEnchantments e = st.getOrDefault(book ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!hasMending(e)) continue;
            EnchantmentHelper.updateEnchantments(st, m -> m.removeIf(h -> h.is(Enchantments.MENDING)));
            if (book && st.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty() && st.is(Items.ENCHANTED_BOOK)) {
                inv.setItem(i, new ItemStack(Items.BOOK, st.getCount()));
            }
            p.displayClientMessage(Component.literal("修繕のエンチャントは消えた（修理は鍛冶師に）").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** 金床での修理（素材・同じ物の合成）は無し */
    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft(), right = event.getRight();
        if (!isGear(left) || right.isEmpty()) return;
        if (!(left.isDamaged() || broken(left))) return;
        if (left.getItem().isValidRepairItem(left, right) || right.is(left.getItem())) event.setCanceled(true);
    }

    /** 砥石（グラインドストーン）で2つを合わせる修理も無し */
    @SubscribeEvent
    public static void onGrindstone(GrindstoneEvent.OnPlaceItem event) {
        ItemStack a = event.getTopItem(), b = event.getBottomItem();
        if (!a.isEmpty() && !b.isEmpty() && (isGear(a) || isGear(b))) event.setCanceled(true);
    }

    static InteractionHand otherHand(InteractionHand h) {
        return h == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private Durability() {}
}
