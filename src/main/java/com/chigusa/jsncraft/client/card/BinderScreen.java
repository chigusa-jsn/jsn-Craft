package com.chigusa.jsncraft.client.card;

import com.chigusa.jsncraft.card.CardAlbumItem;
import com.chigusa.jsncraft.card.CardBinderItem;
import com.chigusa.jsncraft.card.CardItem;
import com.chigusa.jsncraft.card.CardList;
import com.chigusa.jsncraft.card.CardPayloads;
import com.chigusa.jsncraft.card.CardStats;
import com.chigusa.jsncraft.card.Cards;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 装備用バインダー：上に入っている6枚、下に入れられるカード（持ち物とアルバムから） */
public class BinderScreen extends Screen {
    private static final int CW = 38, CH = 52, W = 320, H = 236;
    private final InteractionHand hand;
    private int left, top, ticks, scroll;

    public BinderScreen(InteractionHand hand) {
        super(Component.literal("装備用バインダー"));
        this.hand = hand;
    }

    private ItemStack binder() {
        var p = Minecraft.getInstance().player;
        return p == null ? ItemStack.EMPTY : p.getItemInHand(hand);
    }

    private void send(int kind, String card) {
        PacketDistributor.sendToServer(new CardPayloads.Action(hand == InteractionHand.OFF_HAND ? 1 : 0, kind, card));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override
    public void tick() {
        ticks++;
        if (!(binder().getItem() instanceof CardBinderItem)) onClose();
    }

    /** 入れられるカード（ID → 枚数）。強い順 */
    private List<Map.Entry<String, Integer>> candidates(List<String> in) {
        Map<String, Integer> m = new LinkedHashMap<>();
        Inventory inv = Minecraft.getInstance().player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            CardList.Card c = CardItem.def(s);
            if (c != null) m.merge(c.id(), s.getCount(), Integer::sum);
            if (s.getItem() instanceof CardAlbumItem) CardAlbumItem.cards(s).forEach((k, v) -> m.merge(k, v, Integer::sum));
        }
        in.forEach(m::remove);
        List<Map.Entry<String, Integer>> out = new ArrayList<>(m.entrySet());
        out.sort(Comparator.comparingInt((Map.Entry<String, Integer> e) -> -Cards.get(e.getKey()).rarity()).thenComparing(Map.Entry::getKey));
        return out;
    }

    private int listX(int i) {
        return left + 10 + (i % 7) * (CW + 4);
    }

    private int listY(int i) {
        return top + 104 + (i / 7) * (CH + 4) - scroll;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(left, top, left + W, top + H, 0xF01E1630);
        g.renderOutline(left, top, W, H, 0xFF9080C0);
        g.drawString(font, "§l装備用バインダー", left + 10, top + 8, 0xFFD0C0FF);
        List<String> in = CardBinderItem.cards(binder());
        List<Component> tip = null;
        for (int i = 0; i < CardBinderItem.SLOTS; i++) {
            int x = left + 10 + i * (CW + 4), y = top + 22;
            if (i < in.size()) {
                CardList.Card c = Cards.get(in.get(i));
                CardArt.draw(g, c, x, y, CW, CH, true, ticks);
                if (mx >= x && mx < x + CW && my >= y && my < y + CH) {
                    tip = CardArt.tooltip(c, 0, true);
                    tip.add(Component.literal("§8クリックでとり出す"));
                }
            } else {
                g.fill(x, y, x + CW, y + CH, 0xFF2A2440);
                g.renderOutline(x, y, CW, CH, 0xFF4A4070);
                g.drawCenteredString(font, "§8空き", x + CW / 2, y + CH / 2 - 4, 0xFFFFFF);
            }
        }
        // 効果の合計
        int tx = left + 10 + 6 * (CW + 4) + 4, ty = top + 22;
        g.drawString(font, "§7効果", tx, ty, 0xFFFFFF);
        ty += 11;
        for (var e : CardBinderItem.totals(binder()).entrySet()) {
            String s = CardStats.describe(e.getKey(), e.getValue());
            g.pose().pushPose();
            g.pose().translate(tx, ty, 0);
            g.pose().scale(0.75F, 0.75F, 1);
            g.drawString(font, "§a" + s, 0, 0, 0xFFFFFF);
            g.pose().popPose();
            ty += 8;
        }
        g.drawString(font, "§7入れられるカード §8（同じカードは1枚まで・Curiosの枠につけると効果）", left + 10, top + 90, 0xFFFFFF);
        var cand = candidates(in);
        g.enableScissor(left + 6, top + 102, left + W - 6, top + H - 6);
        for (int i = 0; i < cand.size(); i++) {
            int x = listX(i), y = listY(i);
            if (y + CH < top + 102 || y > top + H) continue;
            CardList.Card c = Cards.get(cand.get(i).getKey());
            CardArt.draw(g, c, x, y, CW, CH, true, ticks);
            int n = cand.get(i).getValue();
            if (n > 1) g.drawString(font, "×" + n, x + 3, y + 3, 0xFFFFFF);
            if (mx >= x && mx < x + CW && my >= y && my < y + CH && my >= top + 102) {
                g.fill(x, y, x + CW, y + CH, 0x30FFFFFF);
                tip = CardArt.tooltip(c, n, true);
                tip.add(Component.literal(in.size() >= CardBinderItem.SLOTS ? "§cバインダーがいっぱい" : "§8クリックで入れる"));
            }
        }
        g.disableScissor();
        if (cand.isEmpty()) g.drawCenteredString(font, "§8入れられるカードがないよ（カードマシンで買おう）", left + W / 2, top + 140, 0xFFFFFF);
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        List<String> in = CardBinderItem.cards(binder());
        for (int i = 0; i < in.size(); i++) {
            int x = left + 10 + i * (CW + 4), y = top + 22;
            if (mx >= x && mx < x + CW && my >= y && my < y + CH) {
                send(3, in.get(i));
                return true;
            }
        }
        if (my >= top + 102) {
            var cand = candidates(in);
            for (int i = 0; i < cand.size(); i++) {
                int x = listX(i), y = listY(i);
                if (mx >= x && mx < x + CW && my >= y && my < y + CH) {
                    if (in.size() < CardBinderItem.SLOTS) send(2, cand.get(i).getKey());
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int rows = (candidates(CardBinderItem.cards(binder())).size() + 6) / 7;
        int max = Math.max(0, rows * (CH + 4) - (H - 110));
        scroll = Math.max(0, Math.min(max, scroll - (int) (sy * 20)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
