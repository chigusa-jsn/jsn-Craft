package com.chigusa.jsncraft.farming;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.job.JobService;
import com.chigusa.jsncraft.quest.QuestService;
import com.chigusa.jsncraft.skill.SkillDefinition;
import com.chigusa.jsncraft.skill.SkillEffects;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.skill.TimedFlags;
import com.chigusa.jsncraft.stat.ModAttributes;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 大地の耕し手・豊穣の守り手のスキル。
 * 職業経験値は「実った作物を収穫したとき」だけ（action: "harvest"）。自動農場ではたまらない。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class FarmingService {

    // ================= 作物の判定 =================

    /** 実っている（収穫してよい）作物か。スイカとカボチャは、茎につながっているときだけ */
    public static boolean isMature(Level level, BlockPos pos, BlockState st) {
        Block b = st.getBlock();
        if (b instanceof CropBlock c) return c.isMaxAge(st);
        if (b instanceof NetherWartBlock) return st.getValue(NetherWartBlock.AGE) >= 3;
        if (b instanceof CocoaBlock) return st.getValue(CocoaBlock.AGE) >= 2;
        if (st.is(Blocks.MELON) || st.is(Blocks.PUMPKIN)) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState n = level.getBlockState(pos.relative(d));
                if (n.getBlock() instanceof AttachedStemBlock && n.getValue(AttachedStemBlock.FACING) == d.getOpposite()) return true;
            }
        }
        return false;
    }

    /** 育つ途中の作物か（成長を早めてよいもの） */
    static boolean isGrowing(BlockState st) {
        Block b = st.getBlock();
        return b instanceof CropBlock || b instanceof StemBlock || b instanceof NetherWartBlock
                || b instanceof CocoaBlock || b instanceof SweetBerryBushBlock;
    }

    /** 植え直したときの状態（年齢0）。植え直せない物は null */
    private static BlockState replanted(BlockState st) {
        Block b = st.getBlock();
        if (b instanceof JsnCropBlock j && j.perennial()) return j.getStateForAge(JsnCropBlock.REGROW_AGE); // 果樹は実だけ取る
        if (b instanceof CropBlock c) return c.getStateForAge(0);
        if (b instanceof NetherWartBlock) return st.setValue(NetherWartBlock.AGE, 0);
        if (b instanceof CocoaBlock) return st.setValue(CocoaBlock.AGE, 0);
        return null;
    }

    /** 作物を1段階（steps）育てる。育ったら true */
    private static boolean grow(ServerLevel level, BlockPos pos, BlockState st, int steps) {
        Block b = st.getBlock();
        if (b instanceof CropBlock c) {
            if (c.isMaxAge(st)) return false;
            level.setBlock(pos, c.getStateForAge(Math.min(c.getMaxAge(), c.getAge(st) + steps)), 2);
            return true;
        }
        if (b instanceof NetherWartBlock) {
            int age = st.getValue(NetherWartBlock.AGE);
            if (age >= 3) return false;
            level.setBlock(pos, st.setValue(NetherWartBlock.AGE, Math.min(3, age + steps)), 2);
            return true;
        }
        if (isGrowing(st)) {
            boolean any = false;
            for (int i = 0; i < steps; i++) any |= BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos);
            return any;
        }
        return false;
    }

    /** 一気に実らせる */
    private static boolean ripen(ServerLevel level, BlockPos pos, BlockState st) {
        Block b = st.getBlock();
        if (b instanceof CropBlock c && !c.isMaxAge(st)) {
            level.setBlock(pos, c.getStateForAge(c.getMaxAge()), 2);
            return true;
        }
        if (b instanceof NetherWartBlock && st.getValue(NetherWartBlock.AGE) < 3) {
            level.setBlock(pos, st.setValue(NetherWartBlock.AGE, 3), 2);
            return true;
        }
        if (b instanceof CocoaBlock && st.getValue(CocoaBlock.AGE) < 2) {
            level.setBlock(pos, st.setValue(CocoaBlock.AGE, 2), 2);
            return true;
        }
        if (b instanceof StemBlock || b instanceof SweetBerryBushBlock) return grow(level, pos, st, 4);
        return false;
    }

    // ================= 収穫 =================

    /** 収穫したときの経験値・依頼・おまけ（宴・奇跡） */
    private static void onHarvested(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState st) {
        if (!player.isCreative()) {
            JobService.onAction(player, rule -> rule.action().equals("harvest") && rule.matchesBlock(st));
        }
        SkillDefinition feast = SkillService.passive(player, "harvest_feast");
        if (feast != null) player.getFoodData().eat((int) Math.round(feast.power()), 0.2F);
        SkillDefinition miracle = SkillService.passive(player, "harvest_miracle");
        if (miracle != null && !inMiracle && player.getRandom().nextDouble() < miracle.chance()) {
            miracle(player, level, pos, (int) Math.round(miracle.radius()));
        }
    }

    private static boolean inMiracle;

    /** 豊穣神の加護：周りの作物がすべて一瞬で実る */
    private static void miracle(ServerPlayer player, ServerLevel level, BlockPos center, int r) {
        inMiracle = true;
        try {
            int n = 0;
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
                BlockState st = level.getBlockState(p);
                if (isGrowing(st) && ripen(level, p.immutable(), st)) {
                    n++;
                    if (n % 3 == 0) level.sendParticles(ParticleTypes.WAX_ON, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 3, 0.3, 0.2, 0.3, 0.05);
                }
            }
            Vec3 c = Vec3.atBottomCenterOf(center);
            Vfx.magicCircle(level, c, r, 30, com.chigusa.jsncraft.vfx.Circle.NATURE);
            Vfx.ring(level, c.add(0, 0.1, 0), r * 1.2F, 18, Vfx.GOLD);
            level.playSound(null, center, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.6F);
            player.displayClientMessage(Component.literal("豊穣神の加護！ " + n + "か所が一瞬で実った").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        } finally {
            inMiracle = false;
        }
    }

    /** 実った作物を収穫する（右クリック収穫・大鎌の一閃）。植え直せる物は植え直す */
    public static boolean harvest(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        BlockState st = level.getBlockState(pos);
        if (!isMature(level, pos, st) || !player.mayInteract(level, pos)) return false;
        ItemStack tool = player.getMainHandItem();
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack s : Block.getDrops(st, level, pos, level.getBlockEntity(pos), player, tool)) drops.add(s.copy());
        BlockState again = replanted(st);
        if (again != null) {
            // 種を1つ使って植え直す
            Item seed = st.getBlock().getCloneItemStack(level, pos, st).getItem();
            for (ItemStack s : drops) {
                if (s.is(seed)) {
                    s.shrink(1);
                    break;
                }
            }
            level.levelEvent(2001, pos, Block.getId(st)); // 壊れる粒と音
            level.setBlock(pos, again, 3);
        } else {
            level.destroyBlock(pos, false, player);
        }
        bonus(player, st, drops);
        int rank = CropRanks.apply(level, pos, st, drops, player);
        CropRanks.set(level, pos, again != null ? rank : 1);   // 植え直したら同じランクから
        for (ItemStack s : drops) if (!s.isEmpty()) Block.popResource(level, pos, s);
        onHarvested(player, level, pos, st);
        return true;
    }

    /** 豊作・収穫の幸運・黄金の実り（drops に足す） */
    private static void bonus(ServerPlayer player, BlockState st, List<ItemStack> drops) {
        RandomSource r = player.getRandom();
        double fortune = player.getAttributeValue(ModAttributes.HARVEST_FORTUNE);
        int extra = (int) fortune;
        if (r.nextDouble() < fortune - extra) extra++;
        SkillDefinition bount = SkillService.passive(player, "bountiful_harvest");
        if (bount != null && r.nextDouble() < bount.chance()) extra++;
        if (extra > 0) {
            for (ItemStack s : drops) s.setCount(Math.min(s.getMaxStackSize(), s.getCount() * (1 + extra)));
        }
        SkillDefinition gold = SkillService.passive(player, "golden_harvest");
        if (gold != null && r.nextDouble() < gold.chance()) {
            Item g = st.is(Blocks.CARROTS) ? Items.GOLDEN_CARROT
                    : st.is(Blocks.MELON) ? Items.GLISTERING_MELON_SLICE
                    : r.nextFloat() < 0.3F ? Items.GOLDEN_APPLE : Items.GOLDEN_CARROT;
            drops.add(new ItemStack(g));
            player.displayClientMessage(Component.literal("黄金の実り！").withStyle(ChatFormatting.GOLD), true);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.8F);
        }
    }

    /** ふつうに壊して収穫したとき（経験値・おまけ） */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) event.getLevel();
        if (isMature(level, event.getPos(), event.getState())) onHarvested(player, level, event.getPos(), event.getState());
    }

    @SubscribeEvent
    public static void onDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        if (!isMature(event.getLevel(), event.getPos(), event.getState())) return;
        List<ItemStack> extra = new ArrayList<>();
        List<ItemStack> base = new ArrayList<>();
        for (ItemEntity e : event.getDrops()) base.add(e.getItem());
        int before = base.size();
        bonus(player, event.getState(), base); // 個数は直接ふえる。足された物は後ろに入る
        for (int i = before; i < base.size(); i++) extra.add(base.get(i));
        BlockPos p = event.getPos();
        for (ItemStack s : extra) event.getDrops().add(new ItemEntity(event.getLevel(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, s));
    }

    /** 右クリック収穫・鍬の3×3耕し */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        BlockPos pos = event.getPos();
        BlockState st = level.getBlockState(pos);
        ItemStack held = player.getMainHandItem();

        if (held.getItem() instanceof HoeItem) {
            SkillDefinition till = SkillService.passive(player, "wide_till");
            if (till == null || player.isShiftKeyDown()) return;
            int r = Math.max(1, (int) Math.round(till.radius()));
            level.getServer().execute(() -> {
                if (!level.getBlockState(pos).is(Blocks.FARMLAND)) return; // 真ん中が耕せたときだけ
                int n = 0;
                for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, 0, -r), pos.offset(r, 0, r))) {
                    BlockState s = level.getBlockState(p);
                    if ((s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT_PATH))
                            && level.getBlockState(p.above()).isAir() && player.mayInteract(level, p)) {
                        level.setBlock(p.immutable(), Blocks.FARMLAND.defaultBlockState(), 11);
                        n++;
                    }
                }
                if (n > 0) {
                    level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 0.8F);
                    level.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 15, r, 0.05, r, 0);
                }
            });
            return;
        }

        if (player.isShiftKeyDown() || st.is(Blocks.MELON) || st.is(Blocks.PUMPKIN)) return;
        if (SkillService.passive(player, "right_harvest") == null || !isMature(level, pos, st)) return;
        if (harvest(player, pos)) {
            player.swing(InteractionHand.MAIN_HAND, true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** 畑を荒らさない */
    @SubscribeEvent
    public static void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && SkillService.passive(player, "no_trample") != null) {
            event.setCanceled(true);
        }
    }

    // ================= 毎秒：大地の恵み・豊穣の結界 =================

    private record Ward(UUID owner, ResourceKey<Level> dim, BlockPos center, int radius, int steps, long until) {}

    private static final List<Ward> WARDS = new CopyOnWriteArrayList<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        // 大地の恵み：近くの作物が育つのが速くなる
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SkillDefinition aura = SkillService.passive(player, "growth_aura");
            if (aura == null) continue;
            ServerLevel level = player.serverLevel();
            int r = Math.max(2, (int) Math.round(aura.radius()));
            int n = Math.max(1, aura.amplifier());
            SkillDefinition rain = SkillService.passive(player, "rain_growth");
            if (rain != null && level.isRaining()) n = (int) Math.round(n * (1 + rain.power()));
            RandomSource rnd = player.getRandom();
            BlockPos c = player.blockPosition();
            for (int i = 0; i < n * 3; i++) { // 外れも多いので多めに引く
                BlockPos col = c.offset(rnd.nextInt(2 * r + 1) - r, 0, rnd.nextInt(2 * r + 1) - r);
                for (int dy = 2; dy >= -2; dy--) {
                    BlockPos p = col.above(dy);
                    BlockState st = level.getBlockState(p);
                    if (isGrowing(st)) {
                        st.randomTick(level, p, rnd);
                        break;
                    }
                }
            }
        }
        // 豊穣の結界
        long now = server.overworld().getGameTime();
        for (Ward w : WARDS) {
            if (now > w.until()) {
                WARDS.remove(w);
                continue;
            }
            ServerLevel level = server.getLevel(w.dim());
            if (level == null) continue;
            int r = w.radius();
            for (BlockPos p : BlockPos.betweenClosed(w.center().offset(-r, -1, -r), w.center().offset(r, 2, r))) {
                BlockState st = level.getBlockState(p);
                if (isGrowing(st) && level.random.nextFloat() < 0.5F) grow(level, p.immutable(), st, w.steps());
            }
            Vec3 c = Vec3.atBottomCenterOf(w.center()).add(0, 0.05, 0);
            if ((w.until() - now) / 20 % 2 == 0) Vfx.magicCircle(level, c, r, 40, com.chigusa.jsncraft.vfx.Circle.NATURE);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y + 0.5, c.z, 12, r * 0.5, 0.3, r * 0.5, 0);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TimedFlags.clear(event.getEntity().getUUID());
    }

    // ================= アクティブスキル =================

    /** 恵みの雨：周りの畑を湿らせて、作物を少し育てる */
    public static boolean rainBlessing(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        int steps = Math.max(1, s.amplifier());
        BlockPos c = player.blockPosition();
        int grown = 0;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 2, r))) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.FARMLAND)) {
                level.setBlock(p.immutable(), st.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), 2);
            } else if (isGrowing(st) && grow(level, p.immutable(), st, steps)) {
                grown++;
            }
        }
        // 頭上から雨が降る
        for (int t = 0; t < 30; t += 5) {
            SkillEffects.later(player, t, () -> level.sendParticles(ParticleTypes.FALLING_WATER,
                    c.getX() + 0.5, c.getY() + 4, c.getZ() + 0.5, 80, r * 0.6, 0.3, r * 0.6, 0));
        }
        level.sendParticles(ParticleTypes.SPLASH, c.getX() + 0.5, c.getY() + 0.2, c.getZ() + 0.5, 60, r * 0.6, 0.1, r * 0.6, 0.1);
        Vfx.ring(level, player.position().add(0, 0.05, 0), r * 1.1F, 16, Vfx.AZURE);
        level.playSound(null, c, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.literal("恵みの雨：" + grown + "か所が育った").withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** 種まき上手：周りの空いた畑に、持っている種をまとめて植える */
    public static boolean massSow(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        BlockPos c = player.blockPosition();
        int planted = 0;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 1, r))) {
            BlockPos above = p.above();
            if (!level.getBlockState(above).isAir()) continue;
            BlockState ground = level.getBlockState(p);
            if (!ground.is(Blocks.FARMLAND) && !ground.is(Blocks.SOUL_SAND)) continue;
            ItemStack seed = findSeed(player, level, above);
            if (seed == null) break;
            Block block = ((BlockItem) seed.getItem()).getBlock();
            level.setBlock(above.immutable(), block.defaultBlockState(), 3);
            if (!player.isCreative()) seed.shrink(1);
            planted++;
            if (planted % 4 == 0) level.sendParticles(ParticleTypes.HAPPY_VILLAGER, above.getX() + 0.5, above.getY() + 0.3, above.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0);
        }
        if (planted == 0) {
            player.displayClientMessage(Component.literal("植えられる畑か種がありません"), true);
            return false;
        }
        Vfx.ring(level, player.position().add(0, 0.05, 0), r * 1.1F, 14, Vfx.GREEN);
        level.playSound(null, c, SoundEvents.CROP_PLANTED, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.literal("種まき上手：" + planted + "か所に植えた").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    /** 植えられる種（手に持っている物が優先） */
    private static ItemStack findSeed(ServerPlayer player, ServerLevel level, BlockPos at) {
        ItemStack main = player.getMainHandItem();
        if (isSeedFor(main, level, at)) return main;
        for (ItemStack s : player.getInventory().items) if (isSeedFor(s, level, at)) return s;
        return null;
    }

    private static boolean isSeedFor(ItemStack s, ServerLevel level, BlockPos at) {
        if (s.isEmpty() || !(s.getItem() instanceof BlockItem bi)) return false;
        Block b = bi.getBlock();
        if (!(b instanceof CropBlock || b instanceof StemBlock || b instanceof NetherWartBlock)) return false;
        return b.defaultBlockState().canSurvive(level, at);
    }

    /** 豊穣の舞：広い範囲の作物を一気に育てる */
    public static boolean harvestDance(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        int steps = Math.max(1, s.amplifier());
        BlockPos c = player.blockPosition();
        Vec3 o = player.position().add(0, 0.05, 0);
        Vfx.magicCircle(level, o, r, 40, com.chigusa.jsncraft.vfx.Circle.NATURE);
        level.playSound(null, c, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        // 花びらが舞いながら、輪が3回広がる
        for (int k = 0; k < 3; k++) {
            int d = k * 8;
            float size = r * (0.5F + k * 0.35F);
            SkillEffects.later(player, d, () -> {
                Vfx.ring(level, o, size, 16, d == 8 ? Vfx.GOLD : Vfx.GREEN);
                level.sendParticles(ParticleTypes.CHERRY_LEAVES, o.x, o.y + 2.5, o.z, 50, r * 0.5, 1.0, r * 0.5, 0.02);
                level.playSound(null, c, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8F, 1.0F + d * 0.04F);
            });
        }
        SkillEffects.later(player, 20, () -> {
            int grown = 0;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 2, r))) {
                BlockState st = level.getBlockState(p);
                if (isGrowing(st) && grow(level, p.immutable(), st, steps)) {
                    grown++;
                    if (grown % 2 == 0) level.levelEvent(1505, p.immutable(), 0);
                }
            }
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, o.x, o.y + 1, o.z, 60, r * 0.4, 0.8, r * 0.4, 0.3);
            level.playSound(null, c, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
            player.displayClientMessage(Component.literal("豊穣の舞：" + grown + "か所が育った").withStyle(ChatFormatting.GREEN), true);
        });
        return true;
    }

    /** 大鎌の一閃：周りの実った作物を一振りで刈り取って、植え直す */
    public static boolean scytheSweep(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        BlockPos c = player.blockPosition();
        List<BlockPos> ripe = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -1, -r), c.offset(r, 1, r))) {
            if (p.distSqr(c) > (r + 0.5) * (r + 0.5)) continue;
            if (isMature(level, p, level.getBlockState(p))) ripe.add(p.immutable());
        }
        if (ripe.isEmpty()) {
            player.displayClientMessage(Component.literal("近くに実った作物がありません"), true);
            return false;
        }
        // 自分の周りをぐるっと一周する斬撃
        Vec3 o = player.position();
        for (int k = 0; k < 8; k++) {
            int d = k;
            double a = Math.toRadians(k * 45 + player.getYRot());
            Vec3 at = o.add(Math.cos(a) * r * 0.55, 0.6, Math.sin(a) * r * 0.55);
            SkillEffects.later(player, d, () -> Vfx.slash(level, at, 1.6F + r * 0.15F, Vfx.GOLD, d * 45));
        }
        level.playSound(null, c, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
        SkillEffects.later(player, 4, () -> {
            int n = 0;
            for (BlockPos p : ripe) if (harvest(player, p)) n++;
            Vfx.ring(level, o.add(0, 0.05, 0), r * 1.1F, 14, Vfx.GOLD);
            level.playSound(null, c, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            player.displayClientMessage(Component.literal("大鎌の一閃：" + n + "か所を収穫").withStyle(ChatFormatting.GOLD), true);
        });
        return true;
    }

    /** 豊穣の結界：しばらくの間、結界の中の作物が毎秒育つ */
    public static boolean harvestWard(ServerPlayer player, SkillDefinition s) {
        ServerLevel level = player.serverLevel();
        int r = (int) Math.round(s.radius());
        WARDS.removeIf(w -> w.owner().equals(player.getUUID())); // 1人1つまで
        long until = level.getGameTime() + Math.round(s.duration() * 20);
        WARDS.add(new Ward(player.getUUID(), level.dimension(), player.blockPosition(), r, Math.max(1, s.amplifier()), until));
        Vec3 o = player.position().add(0, 0.05, 0);
        Vfx.magicCircle(level, o, r, 40, com.chigusa.jsncraft.vfx.Circle.NATURE);
        Vfx.ring(level, o, r * 1.2F, 20, Vfx.GREEN);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, o.x, o.y + 1, o.z, 40, r * 0.5, 0.5, r * 0.5, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.3F);
        player.displayClientMessage(s.name().copy().append(" " + Math.round(s.duration()) + "秒").withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    private FarmingService() {}
}
