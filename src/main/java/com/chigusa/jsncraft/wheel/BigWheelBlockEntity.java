package com.chigusa.jsncraft.wheel;

import com.chigusa.jsncraft.casino.CasinoLuck;
import com.chigusa.jsncraft.casino.Chips;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ビッグホイール本体：1分かけを受けつけ → 8秒回る → 4秒結果 → また受けつけ。
 * 回り方（いつから・どこからどこまで）を見ている人に送って、画面ではそれを見て回す。
 */
public class BigWheelBlockEntity extends BlockEntity {
    public static final int BET_TICKS = 1200, SPIN_TICKS = 160, RESULT_TICKS = 80;
    public static final int BETTING = 0, SPINNING = 1, RESULT = 2;

    public int phase = BETTING;
    public long phaseStart;
    public double fromAngle, toAngle;
    public int result = -1;   // 止まったマス
    public final Map<UUID, long[]> bets = new LinkedHashMap<>();
    public final Map<UUID, Long> pending = new HashMap<>();   // いなかった人の当たり
    public final List<Integer> history = new ArrayList<>();

    public BigWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModWheel.WHEEL_BE.get(), pos, state);
    }

    public int phaseLength() {
        return phase == BETTING ? BET_TICKS : phase == SPINNING ? SPIN_TICKS : RESULT_TICKS;
    }

    public int ticksLeft(long now) {
        return (int) Math.max(0, phaseStart + phaseLength() - now);
    }

    /** いまの角度（画面用） */
    public double angle(long now, float partial) {
        if (phase != SPINNING) return toAngle;
        double t = Math.min(1, (now - phaseStart + partial) / SPIN_TICKS);
        double e = 1 - Math.pow(1 - t, 3);
        return fromAngle + (toAngle - fromAngle) * e;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BigWheelBlockEntity be) {
        long now = level.getGameTime();
        if (be.phaseStart == 0 || be.phaseStart > now) be.phaseStart = now;
        if (now < be.phaseStart + be.phaseLength()) return;
        switch (be.phase) {
            case BETTING -> {
                be.result = level.random.nextInt(WheelLayout.SEGMENTS.length);
                be.fromAngle = ((be.toAngle % 360) + 360) % 360;
                double target = ((-be.result * WheelLayout.STEP - be.fromAngle) % 360 + 360) % 360;
                be.toAngle = be.fromAngle + 360 * 5 + target;
                be.phase = SPINNING;
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1F, 1.2F);
            }
            case SPINNING -> {
                be.phase = RESULT;
                be.payout((ServerLevel) level);
            }
            default -> be.phase = BETTING;
        }
        be.phaseStart = now;
        be.sync();
    }

    private void payout(ServerLevel level) {
        int value = WheelLayout.SEGMENTS[result];
        int k = WheelLayout.index(value);
        history.add(0, value);
        while (history.size() > 12) history.remove(history.size() - 1);
        boolean anyWin = false;
        for (var e : bets.entrySet()) {
            long[] b = e.getValue();
            long total = 0;
            for (long v : b) total += v;
            long won = b[k] * (value + 1);
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null) {
                if (won > 0) {
                    Chips.give(p, won);
                    p.sendSystemMessage(Component.literal("§6ビッグホイール：" + value + " で当たり！ チップ +" + won + "枚"));
                    anyWin = true;
                } else if (total > 0) {
                    p.displayClientMessage(Component.literal("§7ビッグホイール：" + value + "。はずれ…"), true);
                }
                CasinoLuck.played(p, total, won);
                if (won < total) CasinoLuck.refund(p, total - won);
            } else if (won > 0) {
                pending.merge(e.getKey(), won, Long::sum);
            }
        }
        bets.clear();
        level.playSound(null, worldPosition, anyWin ? SoundEvents.PLAYER_LEVELUP : SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1F, value >= 10 ? 1.4F : 1F);
        setChanged();
    }

    public void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        writeShared(tag);
        CompoundTag b = new CompoundTag();
        bets.forEach((u, v) -> b.put(u.toString(), new LongArrayTag(v)));
        tag.put("bets", b);
        CompoundTag pd = new CompoundTag();
        pending.forEach((u, v) -> pd.putLong(u.toString(), v));
        tag.put("pending", pd);
    }

    private void writeShared(CompoundTag tag) {
        tag.putInt("phase", phase);
        tag.putLong("start", phaseStart);
        tag.putDouble("from", fromAngle);
        tag.putDouble("to", toAngle);
        tag.putInt("result", result);
        tag.putIntArray("history", history);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        phase = tag.getInt("phase");
        phaseStart = tag.getLong("start");
        fromAngle = tag.getDouble("from");
        toAngle = tag.getDouble("to");
        result = tag.getInt("result");
        history.clear();
        for (int v : tag.getIntArray("history")) history.add(v);
        if (tag.contains("bets")) {
            bets.clear();
            CompoundTag b = tag.getCompound("bets");
            for (String k : b.getAllKeys()) {
                long[] v = b.getLongArray(k);
                if (v.length == 5) bets.put(UUID.fromString(k), v);
            }
        }
        if (tag.contains("pending")) {
            pending.clear();
            CompoundTag pd = tag.getCompound("pending");
            for (String k : pd.getAllKeys()) pending.put(UUID.fromString(k), pd.getLong(k));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag t = new CompoundTag();
        writeShared(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
