package com.chigusa.jsncraft.mining;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.event.GameEvents;
import com.chigusa.jsncraft.network.OreRevealPayload;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.stat.ResourceService;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 岩窟の掘り師・深淵の鉱脈王のスキル。 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class MiningService {

    private static TagKey<Block> ore(String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/" + path));
    }

    /** 鉱石の種類ごとの色（鉱石感知） */
    private static final List<Object[]> ORE_COLORS = List.of(
            new Object[]{ore("diamond"), 0x40F0FF},
            new Object[]{ore("emerald"), 0x30FF60},
            new Object[]{ore("netherite_scrap"), 0xB0603A},
            new Object[]{ore("gold"), 0xFFD030},
            new Object[]{ore("iron"), 0xE0B090},
            new Object[]{ore("copper"), 0xFF8040},
            new Object[]{ore("redstone"), 0xFF2020},
            new Object[]{ore("lapis"), 0x3050FF},
            new Object[]{ore("quartz"), 0xFFFFFF},
            new Object[]{ore("coal"), 0x505050});

    private static int oreColor(BlockState st) {
        for (Object[] o : ORE_COLORS) {
            @SuppressWarnings("unchecked") TagKey<Block> tag = (TagKey<Block>) o[0];
            if (st.is(tag)) return (int) o[1];
        }
        return 0xC080FF; // 他MODの鉱石など
    }

    static boolean isStone(BlockState st) {
        return st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(BlockTags.BASE_STONE_NETHER) || st.is(Tags.Blocks.COBBLESTONES);
    }

    // ================= 1ブロックを道具で壊す（まとめ掘り用） =================

    /** 道具の残り耐久がこれ以下なら止める（大事な道具を壊さないように） */
    private static boolean toolAlmostBroken(ItemStack tool, int willUse) {
        return tool.isDamageableItem() && tool.getMaxDamage() - tool.getDamageValue() <= willUse + 1;
    }

    /** 手に持った道具で壊したことにする（幸運・シルクタッチ・採掘の幸運が効く）。経験値と依頼も数える */
    public static boolean breakBlock(ServerPlayer player, BlockPos pos, boolean silk) {
        ServerLevel level = player.serverLevel();
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || st.getDestroySpeed(level, pos) < 0 || !player.mayInteract(level, pos)) return false;
        BlockEntity be = level.getBlockEntity(pos);
        ItemStack tool = player.getMainHandItem();
        boolean drops = !player.isCreative() && player.hasCorrectToolForDrops(st);
        level.destroyBlock(pos, false, player);
        if (drops) {
            Block.dropResources(st, level, pos, be, player, tool);
            if (st.is(Tags.Blocks.ORES) && !silk) ExperienceOrb.award(level, Vec3.atCenterOf(pos), 1 + level.random.nextInt(3));
        }
        if (!player.isCreative()) GameEvents.awardBreak(player, st, silk);
        return true;
    }

    // ================= しゃがみ掘り：鉱脈掘り ／ 広域掘り ／ 豊かな鉱脈 =================

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockState state = event.getState();
        BlockPos origin = event.getPos();
        ItemStack tool = player.getMainHandItem();
        boolean pick = tool.is(ItemTags.PICKAXES);

        if (state.is(Tags.Blocks.ORES)) {
            // 豊かな鉱脈：まれに周りの石が同じ鉱石に変わる
            SkillDefinition rich = SkillService.passive(player, "rich_vein");
            if (rich != null && player.getRandom().nextDouble() < rich.chance()) {
                int max = rich.amplifier() > 0 ? rich.amplifier() : 4;
                level.getServer().execute(() -> richVein(player, level, origin, state, max));
            }
            // 鉱脈掘り（しゃがみ）
            SkillDefinition vein = player.isShiftKeyDown() && pick ? SkillService.passive(player, "vein_miner") : null;
            if (vein != null) {
                int limit = vein.amplifier() > 0 ? vein.amplifier() : 16;
                List<BlockPos> found = findVein(level, origin, state.getBlock(), limit);
                boolean silk = GameEvents.hasSilkTouch(player);
                int mined = 0;
                for (BlockPos p : found) {
                    if (toolAlmostBroken(tool, mined + 1)) break;
                    if (!ResourceService.tryUseStamina(player, vein.stamina())) {
                        player.displayClientMessage(Component.literal("スタミナ切れ！"), true);
                        break;
                    }
                    if (breakBlock(player, p, silk)) mined++;
                }
                if (mined > 0) {
                    tool.hurtAndBreak(mined, player, EquipmentSlot.MAINHAND);
                    level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
                    player.displayClientMessage(Component.literal("鉱脈掘り：" + (mined + 1) + "個").withStyle(ChatFormatting.GOLD), true);
                }
                return; // 鉱脈掘りをしたら広域掘りはしない
            }
        }

        // 広域掘り（スキル使用中、ツルハシで掘れるブロック）
        int wide = (int) Math.round(TimedFlags.get(player, "wide_dig"));
        if (wide > 0 && pick && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            Direction face = facing(player, origin);
            float hardness = state.getDestroySpeed(level, origin);
            boolean silk = GameEvents.hasSilkTouch(player);
            int mined = 0;
            for (int a = -wide; a <= wide; a++) for (int b = -wide; b <= wide; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos p = switch (face.getAxis()) {
                    case X -> origin.offset(0, a, b);
                    case Y -> origin.offset(a, 0, b);
                    case Z -> origin.offset(a, b, 0);
                };
                BlockState n = level.getBlockState(p);
                if (!n.is(BlockTags.MINEABLE_WITH_PICKAXE) || level.getBlockEntity(p) != null) continue;
                float h = n.getDestroySpeed(level, p);
                if (h < 0 || h > hardness + 3) continue; // 黒曜石などは巻き込まない
                if (toolAlmostBroken(tool, mined + 1)) break;
                if (breakBlock(player, p, silk)) mined++;
            }
            if (mined > 0) tool.hurtAndBreak(Math.max(1, mined / 2), player, EquipmentSlot.MAINHAND);
        }
    }

    /** 掘っている面（プレイヤーから見た向き） */
    private static Direction facing(ServerPlayer player, BlockPos origin) {
        HitResult hr = player.pick(player.blockInteractionRange(), 0, false);
        if (hr instanceof BlockHitResult bhr && bhr.getBlockPos().equals(origin)) return bhr.getDirection();
        float pitch = player.getXRot();
        if (pitch > 50) return Direction.UP;
        if (pitch < -50) return Direction.DOWN;
        return player.getDirection();
    }

    /** つながった同じ鉱石（斜めも含む） */
    private static List<BlockPos> findVein(ServerLevel level, BlockPos origin, Block block, int limit) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && out.size() < limit) {
            BlockPos p = queue.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = p.offset(dx, dy, dz);
                if (seen.add(n) && level.getBlockState(n).is(block)) {
                    out.add(n);
                    queue.add(n);
                    if (out.size() >= limit) break;
                }
            }
        }
        return out;
    }

    private static void richVein(ServerPlayer player, ServerLevel level, BlockPos origin, BlockState ore, int max) {
        int changed = 0;
        List<BlockPos> around = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-2, -2, -2), origin.offset(2, 2, 2))) around.add(p.immutable());
        java.util.Collections.shuffle(around, new java.util.Random(level.random.nextLong()));
        for (BlockPos p : around) {
            if (changed >= max) break;
            BlockState st = level.getBlockState(p);
            if (!st.is(BlockTags.BASE_STONE_OVERWORLD) && !st.is(BlockTags.BASE_STONE_NETHER)) continue;
            level.setBlockAndUpdate(p, ore);
            level.sendParticles(ParticleTypes.WAX_ON, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.1);
            changed++;
        }
        if (changed == 0) return;
        Vfx.ring(level, Vec3.atCenterOf(origin), 2.5F, 16, Vfx.GOLD);
        level.playSound(null, origin, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.8F);
        player.displayClientMessage(Component.literal("豊かな鉱脈！ 周りの石が鉱石に変わった").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
    }

    // ================= 掘る速さ（石工の腕・深層の加護） =================

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        float mult = 1;
        if (isStone(event.getState())) {
            SkillDefinition mason = SkillService.passive(player, "stone_mastery");
            if (mason != null) mult += (float) mason.power();
        }
        if (player.getY() < 0) {
            SkillDefinition deep = SkillService.passive(player, "deep_blessing");
            if (deep != null) mult += (float) deep.power();
        }
        if (mult != 1) event.setNewSpeed(event.getNewSpeed() * mult);
    }

    // ================= ドロップ（採掘の幸運・宝石の目） =================

    private static final Item[] GEMS = {Items.COAL, Items.COAL, Items.COAL, Items.COAL, Items.RAW_COPPER, Items.RAW_COPPER,
            Items.RAW_COPPER, Items.RAW_IRON, Items.RAW_IRON, Items.RAW_GOLD, Items.LAPIS_LAZULI, Items.REDSTONE, Items.EMERALD, Items.DIAMOND};

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        BlockState st = event.getState();
        BlockPos pos = event.getPos();
        if (st.is(Tags.Blocks.ORES)) {
            SkillDefinition luck = SkillService.passive(player, "ore_fortune");
            if (luck != null && player.getRandom().nextDouble() < luck.chance()) {
                for (ItemEntity drop : event.getDrops()) {
                    ItemStack stack = drop.getItem();
                    if (stack.is(st.getBlock().asItem())) continue; // シルクタッチの鉱石ブロックは増やさない
                    stack.setCount(Math.min(stack.getMaxStackSize(), stack.getCount() * 2));
                    drop.setItem(stack);
                }
            }
        } else if (st.is(BlockTags.BASE_STONE_OVERWORLD) || st.is(BlockTags.BASE_STONE_NETHER)) {
            SkillDefinition eye = SkillService.passive(player, "gem_eye");
            if (eye != null && player.getRandom().nextDouble() < eye.chance()) {
                ItemStack gem = new ItemStack(GEMS[player.getRandom().nextInt(GEMS.length)]);
                event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, gem));
                event.getLevel().sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    // ================= 守り（溶岩の守り・岩の肌） =================

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        float mult = 1;
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            SkillDefinition lava = SkillService.passive(player, "lava_ward");
            if (lava != null) mult *= (float) (1 - Math.min(0.8, lava.power()));
        }
        if (player.getY() < 50 && !player.level().canSeeSky(player.blockPosition())) {
            SkillDefinition skin = SkillService.passive(player, "stone_skin");
            if (skin != null) mult *= (float) (1 - Math.min(0.5, skin.power()));
        }
        if (mult < 1) event.setAmount(event.getAmount() * mult);
    }

    // ================= アクティブスキル =================

    /** 鉱石感知・大地の鼓動：周りの鉱石を壁越しに光らせる（自分にだけ見える） */
    public static boolean revealOres(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = Math.min(32, (int) Math.round(s.radius()));
        int ry = Math.min(24, r);
        BlockPos c = player.blockPosition();
        List<Long> positions = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -ry, -r), c.offset(r, ry, r))) {
            BlockState st = level.getBlockState(p);
            if (!st.is(Tags.Blocks.ORES)) continue;
            positions.add(p.asLong());
            colors.add(oreColor(st));
            if (positions.size() >= 600) break;
        }
        int ticks = (int) Math.round(Math.max(2, s.duration()) * 20);
        PacketDistributor.sendToPlayer(player, new OreRevealPayload(positions, colors, ticks));
        // 足元から波が広がる
        Vec3 o = player.position().add(0, 0.05, 0);
        for (int k = 0; k < 3; k++) {
            int d = k * 5;
            float size = Math.min(16, r) * (0.4F + k * 0.3F);
            SkillEffects.later(player, d, () -> Vfx.ring(level, o, size, 16, Vfx.GOLD));
        }
        level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 40, 0.8, 0.8, 0.8, 0.6);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(s.name().copy().append("：" + positions.size() + "個の鉱石を見つけた").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** 剛掘：掘る速さアップ（power 倍）＋採掘の幸運（bonus） */
    public static boolean powerDig(ServerPlayer player, SkillDefinition s) {
        SkillEffects.attributeBuff(player, Attributes.BLOCK_BREAK_SPEED, "power_dig_speed", s.power(),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, s.duration());
        if (s.bonus() > 0) SkillEffects.attributeBuff(player, ModAttributes.MINING_FORTUNE, "power_dig_fortune", s.bonus(),
                AttributeModifier.Operation.ADD_VALUE, s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), 2.0F, 24, Vfx.GOLD);
        Vfx.ring(level, player.position().add(0, 0.05, 0), 2.6F, 14, Vfx.RED);
        level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1, player.getZ(), 12, 0.5, 0.5, 0.5, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.8F, 0.6F);
        player.displayClientMessage(s.name().copy().append(" " + Math.round(s.duration()) + "秒").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** 広域掘り：しばらくの間、まわりもまとめて掘れる（amplifier 1 = 3×3、2 = 5×5） */
    public static boolean wideDig(ServerPlayer player, SkillDefinition s) {
        TimedFlags.set(player, "wide_dig", Math.max(1, s.amplifier()), s.duration());
        ServerLevel level = player.serverLevel();
        Vfx.magicCircle(level, player.position(), 2.2F, 24, Vfx.AZURE);
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 1, player.getZ(), 30, 0.6, 0.6, 0.6, 0.3);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.2F);
        int size = Math.max(1, s.amplifier()) * 2 + 1;
        player.displayClientMessage(s.name().copy().append(" " + size + "×" + size + "・" + Math.round(s.duration()) + "秒")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** 爆破採掘：前に向かって爆発し、石と鉱石だけをまとめて砕く */
    public static boolean blastMine(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        double r = Math.max(1.5, s.radius());
        Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(r + 1));
        BlockPos c = BlockPos.containing(center);
        List<BlockPos> targets = new ArrayList<>();
        int ri = (int) Math.ceil(r);
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-ri, -ri, -ri), c.offset(ri, ri, ri))) {
            if (Vec3.atCenterOf(p).distanceToSqr(center) > r * r) continue;
            BlockState st = level.getBlockState(p);
            if (!st.is(BlockTags.MINEABLE_WITH_PICKAXE) || level.getBlockEntity(p) != null) continue;
            float h = st.getDestroySpeed(level, p);
            if (h < 0 || h > 6) continue;
            targets.add(p.immutable());
        }
        if (targets.isEmpty()) {
            player.displayClientMessage(Component.literal("前に砕ける岩がありません"), true);
            return false;
        }
        // ため：赤い魔法陣が光ってから爆発
        Vfx.magicCircle(level, center, (float) r, 14, Vfx.RED);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 20, r * 0.3, r * 0.3, r * 0.3, 0.02);
        level.playSound(null, c, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 1.0F, 1.4F);
        SkillEffects.later(player, 12, () -> {
            if (!player.isAlive()) return;
            boolean silk = GameEvents.hasSilkTouch(player);
            ItemStack tool = player.getMainHandItem();
            int broken = 0;
            for (BlockPos p : targets) {
                if (broken >= 300) break;
                if (breakBlock(player, p, silk)) broken++;
            }
            if (broken > 0 && !player.isCreative()) tool.hurtAndBreak(Math.max(1, broken / 6), player, EquipmentSlot.MAINHAND);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 12, r * 0.5, r * 0.5, r * 0.5, 0);
            level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 20, r * 0.4, r * 0.4, r * 0.4, 0);
            Vfx.ring(level, center, (float) r * 1.6F, 12, Vfx.GOLD);
            Vfx.sparks(level, center, 30);
            level.playSound(null, c, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.9F);
            level.playSound(null, c, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
            player.displayClientMessage(Component.literal("爆破採掘：" + broken + "ブロック").withStyle(ChatFormatting.GOLD), true);
        });
        return true;
    }

    private MiningService() {}
}
