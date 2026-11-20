package com.chigusa.jsncraft.card;

import com.chigusa.jsncraft.casino.CasinoBlock;
import com.chigusa.jsncraft.casino.CasinoGame;
import com.chigusa.jsncraft.casino.CasinoLuck;
import com.chigusa.jsncraft.casino.Money;
import com.chigusa.jsncraft.compat.PhoneBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** カードマシンでパックを買う・アルバムとバインダーの出し入れ */
public final class CardService {
    /** パック：値段（チップ＝鉄コイン）と、レア度の確率、最後の1枚の最低レア度 */
    public static final int[] PRICE = {20, 100};
    public static final String[] PACK = {"ノーマルパック", "プレミアムパック"};
    static final double[][] RATES = {{60, 28, 9, 2.5, 0.5}, {0, 50, 35, 12, 3}};
    static final int[] LAST_MIN = {1, 2};
    public static final int PER_PACK = 5;

    public static void buy(ServerPlayer p, CardPayloads.Buy req) {
        if (p.blockPosition().distSqr(req.pos()) > 64 || !(p.level().getBlockState(req.pos()).getBlock() instanceof CasinoBlock b) || b.game != CasinoGame.CARD) return;
        int k = req.pack() == 1 ? 1 : 0;
        List<String> got = new ArrayList<>();
        if (Money.pay(p, PRICE[k])) {
            double luck = CasinoLuck.edge(p);
            for (int i = 0; i < PER_PACK; i++) {
                int r = rarity(p, RATES[k], luck);
                if (i == PER_PACK - 1) r = Math.max(r, LAST_MIN[k]);
                final int rr = r;
                List<CardList.Card> pool = CardList.ALL.stream().filter(c -> c.rarity() == rr).toList();
                CardList.Card c = pool.get(p.getRandom().nextInt(pool.size()));
                got.add(c.id());
                ItemStack s = CardItem.of(c.id());
                if (!p.getInventory().add(s)) p.drop(s, false);
                if (c.rarity() >= 3)
                    PhoneBridge.news(p, p.getName().getString() + " がカードパックから" + CardStats.RARITY[c.rarity()] + "「" + CardItem.name(c).getString() + "」を当てた！", "casino");
            }
            CasinoLuck.played(p, PRICE[k], 0);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1F, 1.2F);
        } else {
            p.displayClientMessage(Component.literal("お金が足りないよ"), true);
        }
        PacketDistributor.sendToPlayer(p, new CardPayloads.Pack(got, Money.walletInChips(p)));
    }

    private static int rarity(ServerPlayer p, double[] rates, double luck) {
        double[] w = rates.clone();
        for (int i = 2; i < 5; i++) w[i] *= 1 + luck * 2;
        double sum = 0;
        for (double v : w) sum += v;
        double x = p.getRandom().nextDouble() * sum;
        for (int i = 0; i < 5; i++) {
            x -= w[i];
            if (x < 0) return i;
        }
        return 0;
    }

    public static void action(ServerPlayer p, CardPayloads.Action a) {
        InteractionHand hand = a.hand() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = p.getItemInHand(hand);
        var inv = p.getInventory();
        switch (a.kind()) {
            case 0 -> {   // アルバムに全部しまう
                if (!(held.getItem() instanceof CardAlbumItem)) return;
                Map<String, Integer> m = CardAlbumItem.cards(held);
                int n = 0;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    CardList.Card c = CardItem.def(s);
                    if (c == null) continue;
                    m.merge(c.id(), s.getCount(), Integer::sum);
                    n += s.getCount();
                    inv.setItem(i, ItemStack.EMPTY);
                }
                CardAlbumItem.setCards(held, m);
                if (n > 0) p.displayClientMessage(Component.literal("カードを " + n + " 枚しまったよ"), true);
            }
            case 1 -> {   // アルバムから1枚出す
                if (!(held.getItem() instanceof CardAlbumItem)) return;
                Map<String, Integer> m = CardAlbumItem.cards(held);
                if (m.getOrDefault(a.card(), 0) <= 0) return;
                m.merge(a.card(), -1, Integer::sum);
                CardAlbumItem.setCards(held, m);
                give(p, CardItem.of(a.card()));
            }
            case 2 -> {   // バインダーに入れる（持ち物か、持っているアルバムから）
                if (!(held.getItem() instanceof CardBinderItem) || Cards.get(a.card()) == null) return;
                List<String> cs = new ArrayList<>(CardBinderItem.cards(held));
                if (cs.size() >= CardBinderItem.SLOTS || cs.contains(a.card())) return;
                if (!takeOne(p, a.card())) return;
                cs.add(a.card());
                CardBinderItem.setCards(held, cs);
            }
            case 3 -> {   // バインダーから出す
                if (!(held.getItem() instanceof CardBinderItem)) return;
                List<String> cs = new ArrayList<>(CardBinderItem.cards(held));
                if (!cs.remove(a.card())) return;
                CardBinderItem.setCards(held, cs);
                give(p, CardItem.of(a.card()));
            }
            default -> {}
        }
    }

    private static boolean takeOne(ServerPlayer p, String id) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof CardItem && CardItem.id(s).equals(id)) {
                s.shrink(1);
                return true;
            }
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof CardAlbumItem) {
                Map<String, Integer> m = CardAlbumItem.cards(s);
                if (m.getOrDefault(id, 0) > 0) {
                    m.merge(id, -1, Integer::sum);
                    CardAlbumItem.setCards(s, m);
                    return true;
                }
            }
        }
        return false;
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    private CardService() {}
}
