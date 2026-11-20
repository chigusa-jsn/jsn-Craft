package com.chigusa.jsncraft.client.card;

import com.chigusa.jsncraft.card.CardAlbumItem;
import com.chigusa.jsncraft.card.CardItem;
import com.chigusa.jsncraft.card.CardList;
import com.chigusa.jsncraft.card.CardPayloads;
import com.chigusa.jsncraft.card.CardStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;

/** カードアルバム：全部のカードを並べて、持っているものは絵、まだのものは影 */
public class AlbumScreen extends Screen {
    private static final int COLS = 8, ROWS = 3, CW = 34, CH = 47, W = COLS * (CW + 4) + 20, H = ROWS * (CH + 6) + 70;
    private final InteractionHand hand;
    private int left, top, page, ticks;
    private Button prev, next;

    public AlbumScreen(InteractionHand hand) {
        super(Component.literal("カードアルバム"));
        this.hand = hand;
    }

    private ItemStack album() {
        var p = Minecraft.getInstance().player;
        return p == null ? ItemStack.EMPTY : p.getItemInHand(hand);
    }

    private int pages() {
        return (CardList.ALL.size() + COLS * ROWS - 1) / (COLS * ROWS);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        prev = addRenderableWidget(Button.builder(Component.literal("◀"), b -> page = Math.max(0, page - 1)).bounds(left + 10, top + H - 26, 20, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal("▶"), b -> page = Math.min(pages() - 1, page + 1)).bounds(left + 80, top + H - 26, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("持ち物のカードを全部しまう"), b -> send(0, ""))
                .bounds(left + W - 150, top + H - 26, 140, 18).build());
    }

    private void send(int kind, String card) {
        PacketDistributor.sendToServer(new CardPayloads.Action(hand == InteractionHand.OFF_HAND ? 1 : 0, kind, card));
    }

    @Override
    public void tick() {
        ticks++;
        if (!(album().getItem() instanceof CardAlbumItem)) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(left, top, left + W, top + H, 0xF02A1A14);
        g.renderOutline(left, top, W, H, 0xFFC08040);
        Map<String, Integer> have = CardAlbumItem.cards(album());
        g.drawString(font, "§lカードアルバム", left + 10, top + 8, 0xFFF0D0A0);
        int[] byR = new int[5], allR = new int[5];
        for (CardList.Card c : CardList.ALL) {
            allR[c.rarity()]++;
            if (have.containsKey(c.id())) byR[c.rarity()]++;
        }
        String done = "集めた種類 " + have.size() + " / " + CardList.ALL.size() + "（" + (have.size() * 100 / Math.max(1, CardList.ALL.size())) + "%）";
        g.drawString(font, "§e" + done, left + W - 10 - font.width(done), top + 8, 0xFFFFFF);
        int sx = left + 10;
        for (int r = 0; r < 5; r++) {
            String s = CardStats.FMT[r] + "★" + (r + 1) + " §f" + byR[r] + "/" + allR[r];
            g.drawString(font, s, sx, top + 20, 0xFFFFFF);
            sx += font.width(s) + 10;
        }
        List<Component> tip = null;
        int per = COLS * ROWS;
        for (int i = 0; i < per; i++) {
            int idx = page * per + i;
            if (idx >= CardList.ALL.size()) break;
            CardList.Card c = CardList.ALL.get(idx);
            int x = left + 10 + (i % COLS) * (CW + 4), y = top + 34 + (i / COLS) * (CH + 6);
            int n = have.getOrDefault(c.id(), 0);
            CardArt.draw(g, c, x, y, CW, CH, n > 0, ticks);
            if (n > 1) {
                String s = "×" + n;
                g.fill(x + CW - font.width(s) - 3, y + 2, x + CW - 2, y + 11, 0xC0000000);
                g.drawString(font, s, x + CW - font.width(s) - 2, y + 3, 0xFFFFFF);
            }
            if (mx >= x && mx < x + CW && my >= y && my < y + CH) {
                g.fill(x, y, x + CW, y + CH, 0x30FFFFFF);
                tip = CardArt.tooltip(c, n, n > 0);
                if (n > 0) tip.add(Component.literal("§8クリックで1枚とり出す"));
            }
        }
        g.drawCenteredString(font, (page + 1) + " / " + pages(), left + 55, top + H - 21, 0xFFFFFF);
        prev.active = page > 0;
        next.active = page < pages() - 1;
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int per = COLS * ROWS;
        Map<String, Integer> have = CardAlbumItem.cards(album());
        for (int i = 0; i < per; i++) {
            int idx = page * per + i;
            if (idx >= CardList.ALL.size()) break;
            int x = left + 10 + (i % COLS) * (CW + 4), y = top + 34 + (i / COLS) * (CH + 6);
            if (mx >= x && mx < x + CW && my >= y && my < y + CH) {
                String id = CardList.ALL.get(idx).id();
                if (have.getOrDefault(id, 0) > 0) send(1, id);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        page = Math.max(0, Math.min(pages() - 1, page - (int) Math.signum(sy)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
