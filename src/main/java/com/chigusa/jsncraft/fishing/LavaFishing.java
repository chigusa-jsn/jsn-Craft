package com.chigusa.jsncraft.fishing;

import com.chigusa.jsncraft.JsnCraft;
import com.chigusa.jsncraft.skill.SkillService;
import com.chigusa.jsncraft.vfx.Vfx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 溶岩釣り（水辺の釣り師のスキル「溶岩釣り」で開放）。
 * 普通の釣りは溶岩では何も起きないので、浮きを溶岩に浮かせて、魚がかかるまでの時間をこちらで数える。
 * 浮きが沈んだときに竿を引くと、ふつうの釣りと同じミニゲームが始まる。
 */
@EventBusSubscriber(modid = JsnCraft.MODID)
public final class LavaFishing {

    private static final class State {
        int wait;      // 魚がかかるまで（tick）
        int biting;    // 0 より大きい間は「かかっている」
    }

    private static final Map<FishingHook, State> STATES = new WeakHashMap<>();

    public static final ResourceKey<LootTable> LAVA_JUNK =
            ResourceKey.create(Registries.LOOT_TABLE, JsnCraft.id("gameplay/lava_fishing_junk"));
    public static final ResourceKey<LootTable> LAVA_TREASURE =
            ResourceKey.create(Registries.LOOT_TABLE, JsnCraft.id("gameplay/lava_fishing_treasure"));

    /** 浮きの下の溶岩の場所（溶岩の上でなければ null） */
    @Nullable
    private static BlockPos lavaUnder(FishingHook hook) {
        BlockPos p = hook.blockPosition();
        if (hook.level().getFluidState(p).is(FluidTags.LAVA)) return p;
        BlockPos below = p.below();
        if (hook.level().getFluidState(below).is(FluidTags.LAVA) && hook.getY() - below.getY() < 1.4) return below;
        return null;
    }

    private static int newWait(RandomSource r) {
        return 100 + r.nextInt(300);
    }

    @SubscribeEvent
    public static void onHookTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof FishingHook hook) || !(hook.level() instanceof ServerLevel level)) return;
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player)) return;
        BlockPos lava = lavaUnder(hook);
        if (lava == null || SkillService.passive(player, "lava_fishing") == null) {
            STATES.remove(hook);
            return;
        }
        // 溶岩の表面に浮かせる（ふつうは沈んでいくので）
        FluidState fluid = level.getFluidState(lava);
        double surface = lava.getY() + fluid.getHeight(level, lava);
        Vec3 dm = hook.getDeltaMovement();
        double dy = surface - hook.getY() - 0.1;
        hook.setDeltaMovement(dm.x * 0.6, Mth.clamp(dy * 0.3, -0.1, 0.12) + 0.03, dm.z * 0.6);
        hook.clearFire();

        RandomSource r = level.random;
        State st = STATES.computeIfAbsent(hook, h -> {
            State s = new State();
            s.wait = newWait(r);
            return s;
        });
        if (st.biting > 0) {
            if (--st.biting == 0) st.wait = newWait(r); // 引くのが遅かった
            hook.setDeltaMovement(hook.getDeltaMovement().add(0, -0.08, 0));
            if (st.biting % 4 == 0) level.sendParticles(ParticleTypes.LAVA, hook.getX(), surface, hook.getZ(), 2, 0.1, 0, 0.1, 0);
            return;
        }
        st.wait--;
        double bite = FishingService.biteSpeed(player);
        if (bite > 0 && r.nextDouble() < bite) st.wait--;
        // 近づいてくる炎の筋
        if (st.wait < 40 && st.wait > 0) {
            double a = (st.wait * 0.37) % (Math.PI * 2);
            double d = st.wait * 0.06;
            level.sendParticles(ParticleTypes.FLAME, hook.getX() + Math.cos(a) * d, surface + 0.05, hook.getZ() + Math.sin(a) * d,
                    1, 0, 0, 0, 0);
        } else if (r.nextInt(20) == 0) {
            level.sendParticles(ParticleTypes.SMOKE, hook.getX(), surface + 0.1, hook.getZ(), 1, 0.1, 0, 0.1, 0.01);
        }
        if (st.wait <= 0) {
            st.biting = 25;
            level.sendParticles(ParticleTypes.LAVA, hook.getX(), surface, hook.getZ(), 10, 0.2, 0, 0.2, 0);
            level.sendParticles(ParticleTypes.FLAME, hook.getX(), surface + 0.1, hook.getZ(), 15, 0.2, 0.1, 0.2, 0.05);
            level.playSound(null, hook.blockPosition(), SoundEvents.LAVA_POP, SoundSource.NEUTRAL, 1.0F, 0.8F + r.nextFloat() * 0.4F);
            level.playSound(null, hook.blockPosition(), SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.4F, 0.6F);
        }
    }

    /** 浮きが沈んでいるときに竿を引いたら、ミニゲーム開始 */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        FishingHook hook = player.fishing;
        if (hook == null || !(event.getItemStack().getItem() instanceof FishingRodItem)) return;
        State st = STATES.get(hook);
        if (st == null || st.biting <= 0) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        player.swing(event.getHand(), true);
        Vec3 pos = hook.position();
        BlockPos bp = hook.blockPosition();
        STATES.remove(hook);
        hook.discard();
        ItemStack rod = event.getItemStack();
        rod.hurtAndBreak(1, player, event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 1.0F, 0.6F);
        start(player, pos, bp);
    }

    private static void start(ServerPlayer player, Vec3 pos, BlockPos bp) {
        ServerLevel level = player.serverLevel();
        RandomSource r = player.getRandom();
        List<ItemStack> drops = List.of();
        FishingService.Kind kind = FishingService.Kind.FISH;
        ResourceLocation species = null;
        if (r.nextDouble() < FishingService.treasureChance(player) + 0.04) {
            drops = FishingService.roll(level, LAVA_TREASURE, null, player, pos);
            kind = FishingService.Kind.TREASURE;
        } else if (r.nextDouble() >= 0.15) {
            species = FishSpeciesManager.INSTANCE.roll(player, level, bp, "lava");
            FishSpecies sp = species == null ? null : FishSpeciesManager.INSTANCE.get(species);
            if (sp != null) drops = new ArrayList<>(List.of(new ItemStack(BuiltInRegistries.ITEM.get(sp.item()))));
        }
        if (drops.isEmpty()) { // 溶岩のガラクタ
            species = null;
            drops = FishingService.roll(level, LAVA_JUNK, null, player, pos);
            kind = FishingService.Kind.JUNK;
        }
        if (drops.isEmpty()) {
            player.displayClientMessage(Component.literal("何も釣れなかった…").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        Vfx.ring(level, pos.add(0, 0.1, 0), 1.5F, 12, Vfx.RED);
        FishingService.begin(player, new ArrayList<>(drops), kind, species, pos);
    }

    private LavaFishing() {}
}
