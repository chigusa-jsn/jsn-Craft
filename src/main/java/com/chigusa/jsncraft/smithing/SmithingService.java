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

    /** +n にするときの成功率（%） */
    private static final int[] RATE = {100, 95, 90, 80, 70, 60, 50, 40, 32, 25, 20, 15, 12, 9, 7};
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

    /** 強化できる上限：鍛ち手は Lv4 ごとに +1（最大 +10）、名工は +10〜+15 */
    public static int maxPlus(ServerPlayer player) {
        int lv = smithLevel(player);
        if (lv < 0) return 0;
        if (lv <= 50) return Math.min(10, 1 + lv / 4);
        return Math.min(15, 10 + (lv - 50) / 20);
    }

    public static int traitSlots(ServerPlayer player) {
        return SkillService.passive(player, "star_forge") != null ? 2 : 1;
    }

    /** +n に必要な強化石の段階（0 鉄 … 4 星鉄） */
    public static int tierFor(int target) {
        return Math.min(4, (target - 1) / 3);
    }

    public static double successRate(ServerPlayer player, int target) {
        if (TimedFlags.get(player, "divine_forge") > 0) return 1.0;
        double r = RATE[Math.max(0, Math.min(14, target - 1))] / 100.0;
        r += Math.max(0, smithLevel(player)) * 0.001;
        SkillDefinition focus = SkillService.passive(player, "smith_success");
        if (focus != null) r += focus.power();
        r += TimedFlags.get(player, "quench");
        return Math.min(1.0, r);
    }

    // ================= 強化 =================

    public static boolean enhance(ServerPlayer player, ForgeBlockEntity be) {
        ItemStack item = be.item(), mat = be.material();
        if (smithLevel(player) < 0 || SmithData.kinds(item).isEmpty()) return false;
        int plus = SmithData.plus(item);
        int max = maxPlus(player);
        if (plus >= max) return false;
        int tier = ModSmithing.stoneTier(mat);
        if (tier < tierFor(plus + 1)) return false;
        var r = player.getRandom();
        boolean success = r.nextDouble() < successRate(player, plus + 1);
        SkillDefinition save = SkillService.passive(player, "smith_save");
        if (save == null || r.nextDouble() >= save.chance()) mat.shrink(1);
        be.setChanged();
        ServerLevel level = player.serverLevel();
        BlockPos pos = be.getBlockPos();
        Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
        level.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 6, 0.2, 0.1, 0.2, 0);
        Vfx.sparks(level, c, 16);
        if (success) {
            double great = 0.03;
            SkillDefinition g1 = SkillService.passive(player, "smith_great");
            SkillDefinition g2 = SkillService.passive(player, "smith_great2");
            if (g1 != null) great += g1.chance();
            if (g2 != null) great += g2.chance();
            int up = (r.nextDouble() < great && plus + 2 <= max) ? 2 : 1;
            SmithData.setPlus(item, plus + up);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.2F);
            level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, up == 2 ? 1.8F : 1.4F);
            Vfx.ring(level, Vec3.atBottomCenterOf(pos).add(0, 1.05, 0), up == 2 ? 2.2F : 1.4F, 14, up == 2 ? Vfx.GOLD : Vfx.WHITE);
            var msg = Component.literal(up == 2 ? "大成功！ " : "強化成功！ ").withStyle(up == 2 ? ChatFormatting.GOLD : ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .append(item.getHoverName().copy());
            player.displayClientMessage(msg, true);
            if (plus + up >= 10) {
                level.getServer().getPlayerList().broadcastSystemMessage(Component.literal("⚒ " + player.getGameProfile().getName() + " が ")
                        .withStyle(ChatFormatting.GOLD).append(item.getHoverName().copy()).append(Component.literal(" を鍛え上げた！").withStyle(ChatFormatting.GOLD)), false);
            }
            JobService.onAction(player, rule -> rule.action().equals("smith"), 5 + (plus + up) * 3);
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

    public static boolean repair(ServerPlayer player, ForgeBlockEntity be) {
        ItemStack item = be.item(), mat = be.material();
        if (smithLevel(player) < 0 || !item.isDamaged() || mat.isEmpty() || !item.getItem().isValidRepairItem(item, mat)) return false;
        double amount = 0.5;
        SkillDefinition skill = SkillService.passive(player, "smith_repair");
        if (skill != null) amount += skill.power();
        item.setDamageValue(Math.max(0, item.getDamageValue() - (int) Math.ceil(item.getMaxDamage() * amount)));
        mat.shrink(1);
        be.setChanged();
        player.serverLevel().playSound(null, be.getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        Vfx.sparks(player.serverLevel(), Vec3.atCenterOf(be.getBlockPos()).add(0, 0.6, 0), 8);
        JobService.onAction(player, rule -> rule.action().equals("smith"), 3);
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
        if (plus <= 0 && traits.isEmpty()) return;
        EquipmentSlotGroup group = stack.getItem() instanceof ArmorItem a ? EquipmentSlotGroup.bySlot(a.getEquipmentSlot()) : EquipmentSlotGroup.MAINHAND;
        if (plus > 0) {
            if (kinds.contains("W") && !SmithData.ranged(stack)) {
                event.addModifier(Attributes.ATTACK_DAMAGE, new AttributeModifier(JsnCraft.id("smith/plus_attack"), 0.6 * plus,
                        AttributeModifier.Operation.ADD_VALUE), group);
            }
            if (kinds.contains("A")) {
                event.addModifier(Attributes.ARMOR, new AttributeModifier(JsnCraft.id("smith/plus_armor_" + group.getSerializedName()), 0.25 * plus,
                        AttributeModifier.Operation.ADD_VALUE), group);
                event.addModifier(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(JsnCraft.id("smith/plus_tough_" + group.getSerializedName()), 0.1 * plus,
                        AttributeModifier.Operation.ADD_VALUE), group);
            }
            if (kinds.contains("T")) {
                // 掘る速さ：効率強化と同じ「採掘効率」（その道具で掘れるブロックだけ）＋全体の倍率
                event.addModifier(Attributes.MINING_EFFICIENCY, new AttributeModifier(JsnCraft.id("smith/plus_efficiency"), 1.5 * plus,
                        AttributeModifier.Operation.ADD_VALUE), group);
                event.addModifier(Attributes.BLOCK_BREAK_SPEED, new AttributeModifier(JsnCraft.id("smith/plus_mining"), 0.06 * plus,
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
    }

    /** 強化した弓・クロスボウの矢は、ダメージが上がる */
    @SubscribeEvent
    public static void onArrow(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof AbstractArrow arrow) || event.getLevel().isClientSide()) return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;
        int plus = SmithData.plus(weapon);
        if (plus > 0) arrow.setBaseDamage(arrow.getBaseDamage() + 0.3 * plus);
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
        Vfx.magicCircle(level, player.position(), (float) s.radius() * 0.5F, 30, Vfx.GOLD);
        Vfx.sparks(level, player.position().add(0, 1, 0), 30);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    /** 神匠の一撃：しばらくの間、強化が必ず成功する */
    public static boolean divineForge(ServerPlayer player, com.chigusa.jsncraft.skill.SkillDefinition s) {
        TimedFlags.set(player, "divine_forge", 1, s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), 3.0F, 40, Vfx.GOLD);
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
