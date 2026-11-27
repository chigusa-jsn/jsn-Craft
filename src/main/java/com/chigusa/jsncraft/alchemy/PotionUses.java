package com.chigusa.jsncraft.alchemy;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.smithing.SmithData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ふつうのポーションの使い道。
 * ・16個まで重ねられる
 * ・調合台の追加素材に入れると、その効果が薬に付く（時間 ×1.5）
 * ・持って近接武器の上で右クリックすると、悪い効果を武器に塗れる（当てるたびに相手にかかる）
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class PotionUses {
    private static final String COAT = "jsn_coat";
    public static final int CHARGES = 10, CHARGES_ALCHEMIST = 20;

    // ================= 調合の追加素材 =================

    /** 追加効果の文字列：p/効果のID/強さ/時間(tick) */
    public static List<String> extrasOf(ItemStack potion) {
        List<String> out = new ArrayList<>();
        PotionContents pc = potion.get(DataComponents.POTION_CONTENTS);
        if (pc == null) return out;
        for (MobEffectInstance e : pc.getAllEffects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value());
            if (id == null) continue;
            out.add("p/" + id + "/" + e.getAmplifier() + "/" + e.getDuration());
        }
        return out;
    }

    public static boolean isExtra(String ex) {
        return ex.startsWith("p/");
    }

    private record Eff(Holder<MobEffect> effect, int amp, int ticks) {}

    private static Eff parse(String ex) {
        String[] a = ex.split("/");
        if (a.length != 4) return null;
        try {
            Optional<Holder.Reference<MobEffect>> h = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(a[1]));
            return h.map(r -> new Eff(r, Integer.parseInt(a[2]), Integer.parseInt(a[3]))).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /** 薬を飲んだときの追加効果（ポーション由来なら true） */
    public static boolean applyExtra(ServerPlayer p, String ex) {
        if (!isExtra(ex)) return false;
        Eff e = parse(ex);
        if (e != null) p.addEffect(new MobEffectInstance(e.effect(), Math.max(1, (int) Math.round(e.ticks() * 1.5)), e.amp()));
        return true;
    }

    private static MutableComponent effectName(Holder<MobEffect> h, int amp) {
        MutableComponent c = h.value().getDisplayName().copy();
        if (amp > 0) c.append(" " + (amp + 1 <= 5 ? new String[]{"I", "II", "III", "IV", "V"}[amp] : String.valueOf(amp + 1)));
        return c;
    }

    public static Component describeExtra(String ex) {
        Eff e = parse(ex);
        if (e == null) return Component.literal("？");
        MutableComponent c = effectName(e.effect(), e.amp());
        if (!e.effect().value().isInstantenous()) c.append("（" + Elixirs.time(e.ticks() * 1.5 / 20.0) + "）");
        return c;
    }

    private static Optional<Holder.Reference<MobEffect>> effectHolder(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? Optional.empty() : BuiltInRegistries.MOB_EFFECT.getHolder(rl);
    }

    // ================= 武器に塗る =================

    private static List<MobEffectInstance> harmful(ItemStack potion) {
        List<MobEffectInstance> out = new ArrayList<>();
        PotionContents pc = potion.get(DataComponents.POTION_CONTENTS);
        if (pc == null) return out;
        for (MobEffectInstance e : pc.getAllEffects())
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) out.add(e);
        return out;
    }

    private static boolean coatable(ItemStack weapon) {
        String k = SmithData.kinds(weapon);
        return k.contains("W") && !SmithData.ranged(weapon);
    }

    @SubscribeEvent
    public static void onStacked(ItemStackedOnOtherEvent event) {
        if (event.getClickAction() != ClickAction.SECONDARY) return;
        ItemStack potion = event.getCarriedItem(), weapon = event.getStackedOnItem();
        if (!(potion.getItem() instanceof PotionItem) || !coatable(weapon)) return;
        event.setCanceled(true);
        if (event.getPlayer() instanceof ServerPlayer sp && applyCoat(sp, potion, weapon)) event.getSlot().setChanged();
    }

    /**
     * オフハンドにポーション、メインハンドに近接武器を持って右クリックでも塗れる。
     * （右クリックで自分の動きがある武器＝槍などは、しゃがみながら右クリック）
     */
    @SubscribeEvent
    public static void onRightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack weapon = player.getMainHandItem(), potion = player.getOffhandItem();
        if (!(potion.getItem() instanceof PotionItem) || !coatable(weapon)) return;
        boolean ownUse = weapon.getUseAnimation() != net.minecraft.world.item.UseAnim.NONE;
        if (ownUse && !player.isShiftKeyDown()) return;
        // メインの右クリックで塗る。オフハンドのポーションを飲むのは止める
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (player instanceof ServerPlayer sp) applyCoat(sp, potion, weapon);
        else player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    /** ポーションを1本使って武器に塗る。塗れたら true */
    public static boolean applyCoat(ServerPlayer sp, ItemStack potion, ItemStack weapon) {
        List<MobEffectInstance> list = harmful(potion);
        if (list.isEmpty()) {
            sp.displayClientMessage(Component.literal("武器に塗れるのは、毒や弱化など悪い効果のポーションだけ").withStyle(ChatFormatting.RED), true);
            return false;
        }
        int charges = AlchemyService.alchemyLevel(sp) >= 0 ? CHARGES_ALCHEMIST : CHARGES;
        CompoundTag c = new CompoundTag();
        ListTag effs = new ListTag();
        for (MobEffectInstance e : list) {
            CompoundTag t = new CompoundTag();
            t.putString("id", String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(e.getEffect().value())));
            t.putInt("amp", e.getAmplifier());
            t.putInt("ticks", e.getDuration());
            effs.add(t);
        }
        c.put("effects", effs);
        c.putInt("charges", charges);
        CustomData.update(DataComponents.CUSTOM_DATA, weapon, root -> root.put(COAT, c));
        potion.shrink(1);
        ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
        if (!sp.getInventory().add(bottle)) sp.drop(bottle, false);
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.7F, 1.4F);
        sp.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME, sp.getX(), sp.getY() + 1.1, sp.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        sp.displayClientMessage(Component.literal("武器にポーションを塗った！（" + charges + " 回）").withStyle(ChatFormatting.DARK_GREEN), true);
        return true;
    }

    private static CompoundTag coat(ItemStack weapon) {
        CustomData d = weapon.get(DataComponents.CUSTOM_DATA);
        if (d == null) return null;
        CompoundTag root = d.getUnsafe();
        return root.contains(COAT) ? root.getCompound(COAT) : null;
    }

    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p) return;
        LivingEntity target = event.getEntity();
        if (target == p || !target.isAlive()) return;
        ItemStack weapon = p.getMainHandItem();
        CompoundTag c = coat(weapon);
        if (c == null) return;
        for (Tag tg : c.getList("effects", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) tg;
            Optional<Holder.Reference<MobEffect>> h = effectHolder(t.getString("id"));
            if (h.isEmpty()) continue;
            int ticks = h.get().value().isInstantenous() ? 1 : Math.max(40, Math.min(300, t.getInt("ticks") / 4));
            target.addEffect(new MobEffectInstance(h.get(), ticks, t.getInt("amp")), p);
        }
        int left = c.getInt("charges") - 1;
        CustomData.update(DataComponents.CUSTOM_DATA, weapon, root -> {
            if (left <= 0) root.remove(COAT);
            else root.getCompound(COAT).putInt("charges", left);
        });
        if (left <= 0) p.displayClientMessage(Component.literal("武器に塗ったポーションが切れた").withStyle(ChatFormatting.GRAY), true);
    }

    // ================= 重ねる =================

    @EventBusSubscriber(modid = JsnCraft.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Stacking {
        @SubscribeEvent
        public static void onComponents(ModifyDefaultComponentsEvent event) {
            event.modify(Items.POTION, b -> b.set(DataComponents.MAX_STACK_SIZE, 16));
            event.modify(Items.SPLASH_POTION, b -> b.set(DataComponents.MAX_STACK_SIZE, 16));
            event.modify(Items.LINGERING_POTION, b -> b.set(DataComponents.MAX_STACK_SIZE, 16));
        }
    }

    // ================= 説明 =================

    @EventBusSubscriber(modid = JsnCraft.MODID, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onTooltip(ItemTooltipEvent event) {
            ItemStack st = event.getItemStack();
            List<Component> tip = event.getToolTip();
            if (st.getItem() instanceof PotionItem && !extrasOf(st).isEmpty()) {
                tip.add(Component.literal("調合台の追加素材に入れると、薬にこの効果が付く（時間 ×1.5）").withStyle(ChatFormatting.DARK_AQUA));
                if (!harmful(st).isEmpty()) tip.add(Component.literal("持って近接武器の上で右クリック、またはオフハンドに持って武器で右クリック：武器に塗る").withStyle(ChatFormatting.DARK_GREEN));
                return;
            }
            CompoundTag c = coat(st);
            if (c == null) return;
            MutableComponent line = Component.literal("☠ 塗り：");
            boolean first = true;
            for (Tag tg : c.getList("effects", Tag.TAG_COMPOUND)) {
                CompoundTag t = (CompoundTag) tg;
                var h = effectHolder(t.getString("id"));
                if (h.isEmpty()) continue;
                if (!first) line.append("・");
                line.append(effectName(h.get(), t.getInt("amp")));
                first = false;
            }
            line.append("（あと " + c.getInt("charges") + " 回）");
            tip.add(Math.min(1, tip.size()), line.withStyle(ChatFormatting.DARK_GREEN));
        }
    }

    private PotionUses() {}
}
