package com.chigusa.jsncraft.npc;

import com.chigusa.jsncraft.job.SkillBoardService;
import com.chigusa.jsncraft.network.OpenTabPayload;
import com.chigusa.jsncraft.quest.QuestService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 受付嬢NPC。その場に立って、近くのプレイヤーの方を向く。
 * 右クリックで依頼の画面が開く。攻撃されても倒れない。
 * 片付けるときは、クリエイティブでスニークしながら殴る（または /kill）。
 */
public class ReceptionistEntity extends PathfinderMob {

    public ReceptionistEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.literal("受付嬢"));
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 1.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new OpenTabPayload(OpenTabPayload.TAB_QUEST));
            QuestService.sendList(sp);
            SkillBoardService.sendSnapshot(sp, true);
            playSound(SoundEvents.VILLAGER_AMBIENT, 0.6F, 1.6F);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    // ---- 倒れない・動かない ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player p && p.isCreative() && p.isShiftKeyDown()) {
            discard(); // クリエイティブ＋スニークで殴ると片付けられる
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }
}
