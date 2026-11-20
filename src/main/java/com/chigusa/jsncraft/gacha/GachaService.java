package com.chigusa.jsncraft.gacha;

import com.chigusa.jsncraft.casino.CasinoBlock;
import com.chigusa.jsncraft.casino.CasinoGame;
import com.chigusa.jsncraft.casino.CasinoLuck;
import com.chigusa.jsncraft.casino.Money;
import com.chigusa.jsncraft.compat.PhoneBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** ガチャガチャ：1回 or 10連（10連はスーパーレア以上が1つ確定）。100回レジェンドが出なければ次はレジェンド */
public final class GachaService {
    private static final String PITY = "jsn_gacha_pity";

    public static void pull(ServerPlayer p, GachaPayloads.Pull req) {
        if (p.blockPosition().distSqr(req.pos()) > 64 || !(p.level().getBlockState(req.pos()).getBlock() instanceof CasinoBlock b) || b.game != CasinoGame.GACHA) return;
        GachaPool pool = GachaPool.get(p.server);
        int count = req.count() == 10 ? 10 : req.count() == 1 ? 1 : 0;
        List<ItemStack> items = new ArrayList<>();
        List<Integer> rar = new ArrayList<>();
        if (count > 0) {
            if (!Money.pay(p, (long) pool.price * count)) {
                p.displayClientMessage(Component.literal("お金が足りないよ"), true);
                count = 0;
            }
        }
        int pity = p.getPersistentData().getInt(PITY);
        double luck = CasinoLuck.edge(p);
        boolean goodSeen = false;
        for (int i = 0; i < count; i++) {
            pity++;
            int r = pool.rollRarity(p.getRandom(), luck);
            if (pity >= pool.pity) r = 4;                          // 天井
            if (count == 10 && i == 9 && !goodSeen && r < 2) r = 2;   // 10連の最後はスーパーレア以上
            if (r >= 2) goodSeen = true;
            if (r == 4) pity = 0;
            ItemStack s = pool.roll(p.getRandom(), r);
            if (s.isEmpty()) continue;
            items.add(s.copy());
            rar.add(r);
            if (!p.getInventory().add(s)) p.drop(s, false);
            if (r >= 3) PhoneBridge.news(p, p.getName().getString() + " がガチャガチャで" + GachaPool.RARITY[r] + "「" + s.getHoverName().getString() + "」を引いた！", "casino");
        }
        p.getPersistentData().putInt(PITY, pity);
        if (count > 0) {
            CasinoLuck.played(p, (long) pool.price * count, 0);
            p.level().playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
        List<ItemStack> prev = new ArrayList<>();
        List<Integer> prevR = new ArrayList<>();
        if (count == 0) {
            List<List<ItemStack>> pv = pool.preview();
            for (int t = 0; t < pv.size(); t++) for (ItemStack s : pv.get(t)) {
                prev.add(s);
                prevR.add(t);
            }
        }
        List<Float> rates = new ArrayList<>();
        for (double d : pool.rates) rates.add((float) d);
        PacketDistributor.sendToPlayer(p, new GachaPayloads.Result(items, rar, pool.pity - pity, Money.walletInChips(p), pool.price, prev, prevR, rates));
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        pull(p, new GachaPayloads.Pull(pos, 0));
    }

    private GachaService() {}
}
