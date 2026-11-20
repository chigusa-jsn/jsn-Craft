package com.chigusa.jsncraft.client.card;

import com.chigusa.jsncraft.card.CardPayloads;
import com.chigusa.jsncraft.card.CardService;
import com.chigusa.jsncraft.card.CardList;
import com.chigusa.jsncraft.card.Cards;
import com.chigusa.jsncraft.card.CardStats;
import com.chigusa.jsncraft.client.casino.CasinoClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** カードマシン：パックを買って、1枚ずつめくる */
public class CardMachineScreen extends Screen {
    private static final int W = 300, H = 200, CW = 48, CH = 66;
    private final BlockPos pos;
    private int left, top, ticks;
    private Button[] buy = new Button[2];
    private List<String> cards = List.of();
    private int flipStart = -1;
    private boolean waiting;

    public CardMachineScreen(BlockPos pos) {
        super(Component.literal("カードマシン"));
        this.pos = pos;
    }

    public static void onPack(CardPayloads.Pack p) {
        CasinoClient.wallet = p.wallet();
        if (Minecraft.getInstance().screen instanceof CardMachineScreen s) s.pack(p);
    }

    private void pack(CardPayloads.Pack p) {
        waiting = false;
        if (!p.cards().isEmpty()) {
            cards = p.cards();
            flipStart = ticks + 10;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F));
        }
        update();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        for (int i = 0; i < 2; i++) {
            int k = i;
            buy[i] = addRenderableWidget(Button.builder(Component.literal((i == 1 ? "§e" : "") + CardService.PACK[i] + "（" + com.chigusa.jsncraft.client.gacha.GachaScreen.moneyText(CardService.PRICE[i]) + "）"),
                    b -> buy(k)).bounds(left + 10 + i * 142, top + H - 28, 138, 20).build());
        }
        update();
    }

    private void update() {
        for (int i = 0; i < 2; i++) if (buy[i] != null) buy[i].active = !waiting && CasinoClient.wallet >= CardService.PRICE[i] && !flipping();
    }

    private boolean flipping() {
        return flipStart >= 0 && ticks < flipStart + cards.size() * 12 + 6;
    }

    private void buy(int k) {
        waiting = true;
        cards = List.of();
        update();
        PacketDistributor.sendToServer(new CardPayloads.Buy(pos, k));
    }

    @Override
    public void tick() {
        ticks++;
        if (flipStart >= 0) {
            int t = ticks - flipStart;
            if (t >= 0 && t % 12 == 6 && t / 12 < cards.size()) {
                CardList.Card c = Cards.get(cards.get(t / 12));
                if (c != null) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                        c.rarity() >= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : c.rarity() >= 2 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.ITEM_PICKUP,
                        c.rarity() >= 3 ? 1.2F : 1.0F + c.rarity() * 0.2F, 0.6F));
            }
        }
        update();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (flipping()) {
            flipStart = ticks - cards.size() * 12 - 6;   // クリックで全部めくる
            update();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(left, top, left + W, top + H, 0xF0141A30);
        g.renderOutline(left, top, W, H, 0xFF5A7AF0);
        g.drawString(font, "§lカードマシン", left + 10, top + 8, 0xFFA0B8FF);
        String w = "お金：" + com.chigusa.jsncraft.client.gacha.GachaScreen.moneyText(CasinoClient.wallet);
        g.drawString(font, "§7" + w, left + W - 10 - font.width(w), top + 8, 0xFFFFFF);
        int cx0 = left + (W - (CW * 5 + 6 * 4)) / 2, cy = top + 30;
        if (cards.isEmpty()) {
            g.drawCenteredString(font, waiting ? "§7…" : "§7パックを買うと、5枚のカードが出てくるよ", left + W / 2, top + 60, 0xFFFFFF);
            g.drawCenteredString(font, "§8ノーマル：最後の1枚はレア以上", left + W / 2, top + 84, 0xFFFFFF);
            g.drawCenteredString(font, "§8プレミアム：レア以上だけ・最後の1枚はスーパーレア以上", left + W / 2, top + 96, 0xFFFFFF);
            g.drawCenteredString(font, "§8カードはアルバムで集めて、バインダーに入れると強くなる", left + W / 2, top + 118, 0xFFFFFF);
        }
        List<net.minecraft.network.chat.Component> tip = null;
        for (int i = 0; i < cards.size(); i++) {
            CardList.Card c = Cards.get(cards.get(i));
            if (c == null) continue;
            int x = cx0 + i * (CW + 6);
            float t = flipStart < 0 ? 99 : (ticks + pt - flipStart - i * 12) / 6F;   // 0〜1 でめくる
            float sx;
            boolean face = t >= 0.5F;
            sx = t <= 0 ? 1 : t >= 1 ? 1 : Math.abs(Mth.cos(t * Mth.PI));
            g.pose().pushPose();
            g.pose().translate(x + CW / 2F, cy + CH / 2F, 0);
            g.pose().scale(Math.max(0.02F, sx), 1, 1);
            g.pose().translate(-CW / 2F, -CH / 2F, 0);
            if (face) CardArt.draw(g, c, 0, 0, CW, CH, true, ticks);
            else back(g, 0, 0);
            g.pose().popPose();
            if (face && t >= 1 && c.rarity() >= 2) {
                int col = CardStats.COLOR[c.rarity()];
                int a = (int) (60 + 50 * Mth.sin((ticks + pt) * 0.25F));
                g.renderOutline(x - 2, cy - 2, CW + 4, CH + 4, a << 24 | (col & 0xFFFFFF));
            }
            if (face && t >= 1) {
                g.drawCenteredString(font, CardStats.FMT[c.rarity()] + CardStats.RARITY[c.rarity()], x + CW / 2, cy + CH + 4, 0xFFFFFF);
                if (mx >= x && mx < x + CW && my >= cy && my < cy + CH) tip = CardArt.tooltip(c, 0, true);
            }
        }
        if (!cards.isEmpty() && !flipping()) g.drawCenteredString(font, "§8カードは持ち物に入ったよ", left + W / 2, top + H - 44, 0xFFFFFF);
        if (tip != null) g.renderComponentTooltip(font, tip, mx, my);
    }

    private void back(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + CW, y + CH, 0xFF5A7AF0);
        g.fill(x + 2, y + 2, x + CW - 2, y + CH - 2, 0xFF2A3A8A);
        for (int i = 0; i < 6; i++) g.fill(x + 6 + i * 6, y + 8, x + 8 + i * 6, y + CH - 8, 0x30FFFFFF);
        g.drawCenteredString(font, "§l?", x + CW / 2, y + CH / 2 - 4, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
