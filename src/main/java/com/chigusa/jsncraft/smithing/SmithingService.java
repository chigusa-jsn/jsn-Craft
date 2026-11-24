package com.chigusa.jsncraft.smithing;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.job.JobType;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.List;

/** 鍛冶：強化（+1〜+15）・鍛え直し・修理。強化と特性の効果は、アイテムを使うときに能力値として足される。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class SmithingService {

    public static final ResourceLocation SMITH = JsnCraft.id("smith");
    public static final ResourceLocation MASTER = JsnCraft.id("master_smith");

    public static final int MAX_PLUS = 30;
    /** +n にするときの成功率（%）＝ミニゲームの当たり幅の広さ */
    private static final int[] RATE = {100, 97, 94, 90, 86, 82, 78, 74, 70, 66, 62, 58, 54, 50, 46,
            42, 38, 34, 30, 27, 24, 21, 18, 16, 14, 12, 10, 8, 6, 5};
    public static final String[] TIER_NAMES = {"鉄", "金", "ダイヤ", "ネザライト", "星鉄"};

    // ================= 職業 =================

    /** 鍛冶の腕のレベル（鍛ち手ならそのレベル、名工なら 50 + レベル、それ以外は -1） */
    public static int smithLevel(ServerPlayer player) {
        var data = JobService.data(player);
        ResourceLocation life = data.getJob(JobType.LIFE);
        if (SMITH.equals(life)) return data.level(life);
        if (MASTER.equals(life)) return 50 + data.level(life);
        return -1;
    }

    /** 強化できる上限：鍛ち手は Lv5 ごとに +2（最大 +20）、名工は +20〜+30（Lv10 ごとに +1） */
    public static int maxPlus(ServerPlayer player) {
        int lv = smithLevel(player);
        if (lv < 0) return 0;
        if (lv <= 50) return Math.min(20, 1 + lv * 2 / 5);
        return Math.min(MAX_PLUS, 20 + (lv - 50) / 10);
    }

    /** +の値による伸び：+15 までは a ずつ、そこから先は b ずつ */
    public static double grow(int plus, double a, double b) {
        return a * Math.min(plus, 15) + b * Math.max(0, plus - 15);
    }

    public static int traitSlots(ServerPlayer player) {
        return SkillService.passive(player, "star_forge") != null ? 2 : 1;
    }

    /** +n に必要な強化石の段階（0 鉄 +1〜6 / 1 金 +7〜12 / 2 ダイヤ +13〜18 / 3 ネザライト +19〜24 / 4 星鉄 +25〜30） */
    public static int tierFor(int target) {
        return Math.max(0, Math.min(4, (target - 1) / 6));
    }

    public static double successRate(ServerPlayer player, int target) {
        if (TimedFlags.get(player, "divine_forge") > 0) return 1.0;
        double r = RATE[Math.max(0, Math.min(MAX_PLUS - 1, target - 1))] / 100.0;
        r += Math.max(0, smithLevel(player)) * 0.001;
        SkillDefinition focus = SkillService.passive(player, "smith_success");
        if (focus != null) r += focus.power();
        r += TimedFlags.get(player, "quench");
        return Math.min(1.0, r);
    }

    // ================= 強化 =================

    /** 大成功（会心）の幅の足し（ミニゲームの会心の幅が広がる） */
    public static double greatBonus(ServerPlayer player) {
        double great = 0.03;
        SkillDefinition g1 = SkillService.passive(player, "smith_great");
        SkillDefinition g2 = SkillService.passive(player, "smith_great2");
        if (g1 != null) great += g1.chance();
        if (g2 != null) great += g2.chance();
        return great;
    }

    public static boolean canEnhance(ServerPlayer player, ForgeBlockEntity be) {
        ItemStack item = be.item(), mat = be.material();
        if (smithLevel(player) < 0 || SmithData.kinds(item).isEmpty()) return false;
        int plus = SmithData.plus(item);
        return plus < maxPlus(player) && ModSmithing.stoneTier(mat) >= tierFor(plus + 1);
    }

    /** ミニゲームを始めた時刻（ずるの防止） */
    private static final java.util.Map<java.util.UUID, Long> GAME_START = new java.util.HashMap<>();

    public static boolean startEnhance(ServerPlayer player, ForgeBlockEntity be) {
        if (!canEnhance(player, be)) return false;
        GAME_START.put(player.getUUID(), player.serverLevel().getGameTime());
        player.serverLevel().playSound(null, be.getBlockPos(), SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 0.4F, 1.4F);
        return true;
    }

    /** ミニゲームの結果で強化する：0 失敗 / 1 成功 / 2 大成功 */
    public static boolean enhance(ServerPlayer player, ForgeBlockEntity be, int result) {
        Long started = GAME_START.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        if (started == null || level.getGameTime() - started < 15 || !canEnhance(player, be)) return false;
        ItemStack item = be.item(), mat = be.material();
        int plus = SmithData.plus(item);
        int max = maxPlus(player);
        if (TimedFlags.get(player, "divine_forge") > 0 && result == 0) result = 1;   // 神匠の一撃：失敗しない
        var r = player.getRandom();
        SkillDefinition save = SkillService.passive(player, "smith_save");
        if (save == null || r.nextDouble() >= save.chance()) mat.shrink(1);
        be.setChanged();
        BlockPos pos = be.getBlockPos();
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        level.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 6, 0.2, 0.1, 0.2, 0);
        Vfx.sparks(level, c, 16);
        if (result > 0) {
            int up = (result == 2 && plus + 2 <= max) ? 2 : 1;
            SmithData.setPlus(item, plus + up);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, result == 2 ? 1.8F : 1.4F);
            Vfx.ring(level, Vec3.atBottomCenterOf(pos).add(0, 1.05, 0), result == 2 ? 2.2F : 1.4F, 14, result == 2 ? Vfx.GOLD : Vfx.WHITE);
            var msg = Component.literal(result == 2 ? "大成功！ " : "強化成功！ ").withStyle(result == 2 ? ChatFormatting.GOLD : ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .append(item.getHoverName().copy());
            player.displayClientMessage(msg, true);
            int now = plus + up;
            if (now >= 15) {
                String verb = now >= MAX_PLUS ? " を極限まで鍛え上げた！！" : now >= 25 ? " を伝説の域まで鍛え上げた！" : " を鍛え上げた！";
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("⚒ " + player.getGameProfile().getName() + " が ")
                        .withStyle(ChatFormatting.GOLD).append(item.getHoverName().copy()).append(Component.literal(verb).withStyle(ChatFormatting.GOLD)), false);
            }
            if (now >= 20) {
                level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.4, c.z, 30, 0.3, 0.6, 0.3, 0.08);
                level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.8F, 1.2F);
            }
            JobService.onAction(player, rule -> rule.action().equals("smith"), 5 + now * 2 + (result == 2 ? 5 : 0));
        } else {
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.7F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 10, 0.2, 0.2, 0.2, 0.02);
            player.displayClientMessage(Component.literal("失敗…（強化石だけなくなった）").withStyle(ChatFormatting.GRAY), true);
            JobService.onAction(player, rule -> rule.action().equals("smith"), 2 + plus);
        }
        return true;
    }

    // ================= 鍛え直し =================

    public static boolean reforge(ServerPlayer player, ForgeBlockEntity be, int slot) {
        ItemStack item = be.item(), mat = be.material();
        String kinds = SmithData.kinds(item);
        if (smithLevel(player) < 0 || kinds.isEmpty() || slot >= traitSlots(player)) return false;
        List<SmithTraits.Trait> pool = SmithTraits.pool(mat.getItem(), kinds);
        if (pool.isEmpty()) return false;
        var r = player.getRandom();
        boolean echo = mat.is(net.minecraft.world.item.Items.ECHO_SHARD);
        SmithTraits.Trait t = pool.get(r.nextInt(pool.size()));
        double ratio = r.nextDouble();
        if (echo) ratio = 0.5 + ratio * 0.5;                        // 残響の欠片は強めに出る
        SkillDefinition eye = SkillService.passive(player, "reforge_luck");
        if (eye != null) ratio = Math.max(ratio, Math.min(1, r.nextDouble() + eye.power()));
        if (SkillService.passive(player, "reforge_twice") != null) ratio = Math.max(ratio, r.nextDouble());
        double value = t.min() + (t.max() - t.min()) * ratio;
        if (echo && r.nextDouble() < 0.1) value *= 1.2;              // まれに上限を超える
        SmithData.setTrait(item, slot, new SmithData.TraitRoll(t.id(), value, ratio));
        mat.shrink(1);
        be.setChanged();
        ServerLevel level = player.serverLevel();
        BlockPos pos = be.getBlockPos();
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        level.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 0.4, c.z, 30, 0.3, 0.3, 0.3, 0.6);
        Vfx.sparks(level, c, 10);
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        String grade = SmithTraits.grade(ratio);
        if (grade.equals("S")) level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.6F);
        player.displayClientMessage(Component.literal("鍛え直し：").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("◆" + t.name() + " " + grade).withStyle(SmithTraits.gradeColor(grade), ChatFormatting.BOLD)), true);
        JobService.onAction(player, rule -> rule.action().equals("smith"), 6);
        return true;
    }

    // ================= 修理 =================

    /** 修理：鍛冶師なら素材なしでも直せる（素材を入れるともっと直る）。壊れた物も直せて、100% まで戻せる */
    public static boolean repair(ServerPlayer player, ForgeBlockEntity be) {
        ItemStack item = be.item(), mat = be.material();
        if (smithLevel(player) < 0 || !canRepair(item)) return false;
        boolean useMat = !mat.isEmpty() && item.getItem().isValidRepairItem(item, mat);
        boolean wasBroken = Durability.broken(item);
        double amount = useMat ? 0.5 : 0.35;
        SkillDefinition skill = SkillService.passive(player, "smith_repair");
        if (skill != null) amount += skill.power();
        amount += Math.max(0, smithLevel(player)) * 0.002;
        Durability.repair(item, amount, 1.0);
        if (useMat) mat.shrink(1);
        be.setChanged();
        ServerLevel level = player.serverLevel();
        level.playSound(null, be.getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        Vfx.sparks(level, Vec3.atCenterOf(be.getBlockPos()).add(0, 0.6, 0), 8);
        player.displayClientMessage(Component.literal((wasBroken ? "壊れた物を直した！ " : "修理した！ ") + "耐久 "
                + Math.round(Durability.ratio(item) * 100) + "%").withStyle(ChatFormatting.GREEN), true);
        JobService.onAction(player, rule -> rule.action().equals("smith"), wasBroken ? 6 : 3);
        return true;
    }

    public static boolean canRepair(ItemStack item) {
        return !item.isEmpty() && (item.isDamaged() || Durability.broken(item));
    }

    // ================= 製作（鍛冶師だけが作れる物） =================

    /** 鍛冶台での製作：base（鍛える物の枠、無しなら null）＋ input（素材の枠）→ output */
    public record Craft(net.minecraft.world.item.Item base, int baseCount, net.minecraft.world.item.Item input, int count,
                        java.util.function.Supplier<? extends net.minecraft.world.item.Item> output, int outCount, int minLevel, String name, boolean quality) {}

    private static Craft tool(net.minecraft.world.item.Item in, int n, java.util.function.Supplier<? extends net.minecraft.world.item.Item> out, int lv, String name) {
        return new Craft(null, 0, in, n, out, 1, lv, name, false);
    }

    private static Craft arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind k, net.minecraft.world.item.Item in, int n, int arrows, int lv) {
        return new Craft(net.minecraft.world.item.Items.ARROW, arrows, in, n, com.chigusa.jsncraft.smithing.arrow.ModArrows.ARROWS.get(k), arrows, lv, k.name, true);
    }

    public static final List<Craft> CRAFTS;

    static {
        java.util.List<Craft> l = new java.util.ArrayList<>();
        // 修理の道具
        l.add(tool(net.minecraft.world.item.Items.FLINT, 3, ModSmithing.WHETSTONE, 0, "砥石"));
        l.add(tool(net.minecraft.world.item.Items.IRON_INGOT, 2, ModSmithing.REPAIR_KIT, 0, "修理キット"));
        l.add(tool(net.minecraft.world.item.Items.COPPER_INGOT, 4, ModSmithing.ARMOR_PLATE, 5, "当て板"));
        l.add(tool(net.minecraft.world.item.Items.AMETHYST_SHARD, 3, ModSmithing.WHETSTONE_ADV, 15, "上級砥石"));
        l.add(tool(net.minecraft.world.item.Items.DIAMOND, 1, ModSmithing.REPAIR_KIT_ADV, 15, "上級修理キット"));
        // 矢（鍛える物の枠に矢、素材の枠に材料）
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.STEEL, net.minecraft.world.item.Items.IRON_INGOT, 1, 8, 0));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.POISON, net.minecraft.world.item.Items.SPIDER_EYE, 1, 8, 0));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.LIGHT, net.minecraft.world.item.Items.GLOWSTONE_DUST, 2, 8, 0));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.ROPE, net.minecraft.world.item.Items.STRING, 4, 4, 0));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.FIRE, net.minecraft.world.item.Items.BLAZE_POWDER, 1, 8, 5));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.ICE, net.minecraft.world.item.Items.PACKED_ICE, 1, 8, 5));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.WEAKNESS, net.minecraft.world.item.Items.FERMENTED_SPIDER_EYE, 1, 8, 5));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.PIERCE, net.minecraft.world.item.Items.QUARTZ, 2, 8, 10));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.BLAST, net.minecraft.world.item.Items.GUNPOWDER, 2, 8, 10));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.BOLT, net.minecraft.world.item.Items.CHAIN, 1, 4, 10));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.THUNDER, net.minecraft.world.item.Items.COPPER_INGOT, 2, 8, 15));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.RETRIEVE, net.minecraft.world.item.Items.ENDER_PEARL, 1, 8, 20));
        l.add(arrow(com.chigusa.jsncraft.smithing.arrow.ArrowKind.HOMING, net.minecraft.world.item.Items.ENDER_EYE, 1, 8, 30));
        // 投げ物
        l.add(new Craft(null, 0, net.minecraft.world.item.Items.IRON_NUGGET, 6, com.chigusa.jsncraft.smithing.arrow.ModArrows.THROWING_KNIFE, 4, 5, "投げナイフ", true));
        l.add(new Craft(net.minecraft.world.item.Items.IRON_NUGGET, 4, net.minecraft.world.item.Items.FLINT, 1, com.chigusa.jsncraft.smithing.arrow.ModArrows.SHURIKEN, 8, 10, "手裏剣", true));
        // 三叉槍の穂先
        var T = com.chigusa.jsncraft.smithing.arrow.ModArrows.TIPS;
        l.add(new Craft(net.minecraft.world.item.Items.PRISMARINE_SHARD, 4, net.minecraft.world.item.Items.MAGMA_CREAM, 2, T.get(0), 1, 20, "炎の穂先", false));
        l.add(new Craft(net.minecraft.world.item.Items.PRISMARINE_SHARD, 4, net.minecraft.world.item.Items.BLUE_ICE, 1, T.get(1), 1, 20, "氷の穂先", false));
        l.add(new Craft(net.minecraft.world.item.Items.PRISMARINE_SHARD, 4, net.minecraft.world.item.Items.LIGHTNING_ROD, 2, T.get(2), 1, 25, "雷の穂先", false));
        CRAFTS = List.copyOf(l);
    }

    /** 2つの枠の物で作れる物（無ければ -1） */
    public static int craftIndex(ItemStack item, ItemStack mat) {
        if (mat.isEmpty()) return -1;
        for (int i = 0; i < CRAFTS.size(); i++) {
            Craft c = CRAFTS.get(i);
            if (!mat.is(c.input())) continue;
            if (c.base() == null ? item.isEmpty() : item.is(c.base())) return i;
        }
        return -1;
    }

    public static boolean canCraft(int lv, ItemStack item, ItemStack mat) {
        int idx = craftIndex(item, mat);
        if (lv < 0 || idx < 0) return false;
        Craft c = CRAFTS.get(idx);
        return lv >= c.minLevel() && mat.getCount() >= c.count() && (c.base() == null || item.getCount() >= c.baseCount());
    }

    /** レシピを選んだとき：枠の中身を持ち物に戻して、持ち物から材料を入れる */
    public static boolean fillCraft(ServerPlayer player, ForgeBlockEntity be, int idx) {
        if (idx < 0 || idx >= CRAFTS.size()) return false;
        Craft c = CRAFTS.get(idx);
        var inv = player.getInventory();
        for (int s = 0; s < 2; s++) {
            ItemStack st = be.items().getItem(s);
            if (st.isEmpty()) continue;
            be.items().setItem(s, ItemStack.EMPTY);
            if (!inv.add(st)) player.drop(st, false);
        }
        if (c.base() != null) be.items().setItem(0, take(inv, c.base()));
        be.items().setItem(1, take(inv, c.input()));
        be.setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }

    /** 持ち物から、その物（印の無いふつうの物）を1スタック分集める */
    private static ItemStack take(net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.item.Item item) {
        ItemStack acc = ItemStack.EMPTY;
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack st = inv.items.get(i);
            if (!st.is(item) || st.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) continue;
            if (acc.isEmpty()) {
                acc = st.split(Math.min(st.getCount(), st.getMaxStackSize()));
            } else {
                if (!ItemStack.isSameItemSameComponents(acc, st)) continue;
                int m = Math.min(st.getCount(), acc.getMaxStackSize() - acc.getCount());
                if (m <= 0) break;
                st.shrink(m);
                acc.grow(m);
            }
        }
        return acc;
    }

    public static boolean craft(ServerPlayer player, ForgeBlockEntity be) {
        ItemStack item = be.item(), mat = be.material();
        int lv = smithLevel(player);
        if (!canCraft(lv, item, mat)) return false;
        Craft c = CRAFTS.get(craftIndex(item, mat));
        mat.shrink(c.count());
        if (c.base() != null) item.shrink(c.baseCount());
        ItemStack out = new ItemStack(c.output().get(), c.outCount());
        int q = 1;
        if (c.quality()) {
            q = com.chigusa.jsncraft.smithing.arrow.Quality.roll(lv, player.getRandom());
            com.chigusa.jsncraft.smithing.arrow.Quality.set(out, q);
        }
        be.setChanged();
        if (!player.getInventory().add(out)) player.drop(out, false);
        ServerLevel level = player.serverLevel();
        level.playSound(null, be.getBlockPos(), SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.1F);
        if (q == 3) level.playSound(null, be.getBlockPos(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5F, 1.8F);
        Vfx.sparks(level, Vec3.atCenterOf(be.getBlockPos()).add(0, 0.6, 0), 10);
        player.displayClientMessage(Component.literal("製作：" + c.name() + " ×" + c.outCount() + (c.quality() ? "  " + "★".repeat(q) : ""))
                .withStyle(q == 3 ? ChatFormatting.GOLD : ChatFormatting.GREEN), true);
        JobService.onAction(player, rule -> rule.action().equals("smith"), 2 + c.minLevel() / 5);
        return true;
    }

    // ================= 効果（能力値として足す） =================

    @SubscribeEvent
    public static void onModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        String kinds = SmithData.kinds(stack);
        if (kinds.isEmpty()) return;
        int plus = SmithData.plus(stack);
        var traits = SmithData.traits(stack);
        EquipmentSlotGroup group = groupOf(stack);
        if (Durability.plate(stack) > 0 && kinds.contains("A")) {
            event.addModifier(Attributes.ARMOR, new AttributeModifier(JsnCraft.id("smith/plate_armor_" + group.getSerializedName()), 1.5,
                    AttributeModifier.Operation.ADD_VALUE), group);
            event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(JsnCraft.id("smith/plate_tough_" + group.getSerializedName()), 1.0,
                    AttributeModifier.Operation.ADD_VALUE), group);
        }
        if (plus > 0) {
            if (kinds.contains("W") && !SmithData.ranged(stack)) {
                event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(JsnCraft.id("smith/plus_attack"), grow(plus, 0.6, 0.4),
                        AttributeModifier.Operation.ADD_VALUE), group);
            }
            if (kinds.contains("A")) {
                event.addModifier(Attributes.ARMOR, new AttributeModifier(JsnCraft.id("smith/plus_armor_" + group.getSerializedName()), grow(plus, 0.25, 0.15),
                        AttributeModifier.Operation.ADD_VALUE), group);
                event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(JsnCraft.id("smith/plus_tough_" + group.getSerializedName()), grow(plus, 0.1, 0.06),
                        AttributeModifier.Operation.ADD_VALUE), group);
            }
            String g = group.getSerializedName();
            if (kinds.contains("S")) {   // 盾：防御とノックバック耐性
                event.addModifier(Attributes.ARMOR, new AttributeModifier(JsnCraft.id("smith/plus_shield_armor"), grow(plus, 0.15, 0.1),
                        AttributeModifier.Operation.ADD_VALUE), group);
                event.addModifier(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(JsnCraft.id("smith/plus_shield_kb"), grow(plus, 0.01, 0.01),
                        AttributeModifier.Operation.ADD_VALUE), group);
            }
            if (kinds.contains("E")) {   // エリトラ：防御とタフネス
                event.addModifier(Attributes.ARMOR, new AttributeModifier(JsnCraft.id("smith/plus_elytra_armor_" + g), grow(plus, 0.2, 0.12),
                        AttributeModifier.Operation.ADD_VALUE), group);
                event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(JsnCraft.id("smith/plus_elytra_tough_" + g), grow(plus, 0.08, 0.05),
                        AttributeModifier.Operation.ADD_VALUE), group);
            }
            if (kinds.contains("F")) {   // 釣り竿：幸運（釣れる物が良くなる）
                event.addModifier(Attributes.LUCK, new AttributeModifier(JsnCraft.id("smith/plus_rod_luck"), grow(plus, 0.1, 0.08),
                        AttributeModifier.Operation.ADD_VALUE), group);
            }
            if (kinds.contains("T")) {
                // 掘る速さ：効率強化と同じ「採掘効率」（その道具で掘れるブロックだけ）＋全体の倍率
                event.addModifier(Attributes.MINING_EFFICIENCY, new AttributeModifier(JsnCraft.id("smith/plus_efficiency"), grow(plus, 1.5, 1.0),
                        AttributeModifier.Operation.ADD_VALUE), group);
                event.addModifier(Attributes.BLOCK_BREAK_SPEED, new AttributeModifier(JsnCraft.id("smith/plus_mining"), grow(plus, 0.06, 0.04),
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE), group);
            }
        }
        for (int i = 0; i < traits.size(); i++) {
            SmithData.TraitRoll roll = traits.get(i);
            SmithTraits.Trait t = SmithTraits.TRAITS.get(roll.id());
            if (t == null) continue;
            int idx = i;
            t.holder().ifPresent(h -> event.addModifier(h, new AttributeModifier(
                    JsnCraft.id("smith/trait" + idx + "_" + group.getSerializedName()), roll.value(), t.op()), group));
        }
        sharpness(event, stack, kinds, group);
    }

    public static EquipmentSlotGroup groupOf(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof ArmorItem a) return EquipmentSlotGroup.bySlot(a.getEquipmentSlot());
        if (item instanceof net.minecraft.world.item.ElytraItem) return EquipmentSlotGroup.CHEST;
        if (item instanceof net.minecraft.world.item.ShieldItem || item instanceof net.minecraft.world.item.FishingRodItem) return EquipmentSlotGroup.HAND;
        return EquipmentSlotGroup.MAINHAND;
    }

    /** 切れ味：耐久が減ると攻撃力・防御力が下がる（壊れるとほぼ使えない） */
    private static void sharpness(ItemAttributeModifierEvent event, ItemStack stack, String kinds, EquipmentSlotGroup group) {
        if (stack.getMaxDamage() <= 0) return;
        if (kinds.contains("W") && !SmithData.ranged(stack)) {
            double f = Durability.weaponFactor(stack);
            if (f < 1) {
                double base = sum(event, Attributes.ATTACK_DAMAGE, group);
                if (base > 0) event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(JsnCraft.id("smith/sharpness"),
                        -base * (1 - f), AttributeModifier.Operation.ADD_VALUE), group);
            }
        }
        if (kinds.contains("A")) {
            double f = Durability.armorFactor(stack);
            if (f < 1) {
                String g = group.getSerializedName();
                double armor = sum(event, Attributes.ARMOR, group), tough = sum(event, Attributes.ARMOR_TOUGHNESS, group);
                if (armor > 0) event.addModifier(Attributes.ARMOR, new AttributeModifier(JsnCraft.id("smith/wear_armor_" + g),
                        -armor * (1 - f), AttributeModifier.Operation.ADD_VALUE), group);
                if (tough > 0) event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(JsnCraft.id("smith/wear_tough_" + g),
                        -tough * (1 - f), AttributeModifier.Operation.ADD_VALUE), group);
            }
        }
        if (Durability.broken(stack) && kinds.contains("T")) {
            event.addModifier(Attributes.BLOCK_BREAK_SPEED, new AttributeModifier(JsnCraft.id("smith/broken_mining"), -0.9,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), group);
        }
    }

    private static double sum(ItemAttributeModifierEvent event, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, EquipmentSlotGroup group) {
        double v = 0;
        for (var e : event.getModifiers()) {
            if (e.attribute().equals(attr) && e.slot() == group && e.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) v += e.modifier().amount();
        }
        return v;
    }

    /** 強化した弓・クロスボウの矢は、ダメージが上がる */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow) || event.getLevel().isClientSide()) return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;
        int plus = SmithData.plus(weapon);
        if (plus > 0) arrow.setBaseDamage(arrow.getBaseDamage() + grow(plus, 0.3, 0.2));
        if (weapon.getMaxDamage() > 0) arrow.setBaseDamage(arrow.getBaseDamage() * Durability.weaponFactor(weapon));
    }

    // ================= スキル =================

    /** 焼き入れ：しばらくの間、強化の成功率アップ */
    public static boolean quench(ServerPlayer player, com.chigusa.jsncraft.skill.SkillDefinition s) {
        TimedFlags.set(player, "quench", s.power(), s.duration());
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.2, player.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.3, 0.4, 0.1);
        Vfx.ring(level, player.position().add(0, 0.05, 0), 1.8F, 14, Vfx.AZURE);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.literal("焼き入れ：成功率 +" + Math.round(s.power() * 100) + "%（" + Math.round(s.duration()) + "秒）")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** 火床の加護：周りの仲間の防御力アップ */
    public static boolean forgeBlessing(ServerPlayer player, com.chigusa.jsncraft.skill.SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(s.radius()))) {
            com.chigusa.jsncraft.skill.SkillEffects.attributeBuff(p, Attributes.ARMOR, "forge_armor", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            com.chigusa.jsncraft.skill.SkillEffects.attributeBuff(p, Attributes.ARMOR_TOUGHNESS, "forge_tough", s.power(), AttributeModifier.Operation.ADD_VALUE, s.duration());
            level.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        Vfx.magicCircle(level, player.position(), (float) s.radius() * 0.5F, 30, com.chigusa.jsncraft.vfx.Circle.FIRE);
        Vfx.sparks(level, player.position().add(0, 1, 0), 30);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 神匠の一撃：しばらくの間、強化が必ず成功する */
    public static boolean divineForge(ServerPlayer player, com.chigusa.jsncraft.skill.SkillDefinition s) {
        TimedFlags.set(player, "divine_forge", 1, s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), 3.0F, 40, com.chigusa.jsncraft.vfx.Circle.FIRE);
        Vfx.ring(level, player.position().add(0, 0.05, 0), 4.0F, 24, Vfx.WHITE);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 40, 0.6, 1.0, 0.6, 0.08);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 1.6F);
        player.displayClientMessage(Component.literal("神匠の一撃：" + Math.round(s.duration()) + "秒間、強化が必ず成功する！")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private SmithingService() {}
}
