package com.chigusa.jsncraft.client.gacha;

import com.chigusa.jsncraft.client.casino.CasinoClient;
import com.chigusa.jsncraft.gacha.GachaPayloads;
import com.chigusa.jsncraft.gacha.GachaPool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * ガチャガチャの画面。ハンドルを回す → カプセルが落ちる → パカッと開いて景品。
 * 結果はサーバーで決まって、画面は演出だけ。
 */
public class GachaScreen extends Screen {
    private static final int W = 300, H = 210;
    private static final int[] CAPS = {0xFFE05050, 0xFF50A0E0, 0xFFF0D040, 0xFF60C060, 0xFFE070C0, 0xFFF09040, 0xFFA070F0};
    private final BlockPos pos;
    private int left, top, ticks;
    private Button one, ten, list;

    private List<ItemStack> items = List.of();
    private List<Integer> rar = List.of();
    private List<ItemStack> preview = new ArrayList<>();
    private List<Integer> previewR = new ArrayList<>();
    private List<Float> rates = List.of(60F, 28F, 9F, 2.5F, 0.5F);
    private int pityLeft = 100, price = 10;
    /** 演出：0=待ち 1=ハンドル 2=落ちる 3=開く 4=結果 */
    private int phase, phaseT, shown;
    private boolean waiting, showList;

    public GachaScreen(BlockPos pos) {
        super(Component.literal("ガチャガチャ"));
        this.pos = pos;
    }

    public static void onResult(GachaPayloads.Result r) {
        CasinoClient.wallet = r.wallet();
        if (Minecraft.getInstance().screen instanceof GachaScreen s) s.result(r);
    }

    private void result(GachaPayloads.Result r) {
        waiting = false;
        pityLeft = r.pityLeft();
        price = r.price();
        rates = r.rates();
        if (!r.preview().isEmpty()) {
            preview = r.preview();
            previewR = r.previewRarity();
        }
        if (!r.items().isEmpty()) {
            items = r.items();
            rar = r.rarity();
            phase = 1;
            phaseT = 0;
            shown = 0;
        }
        updateButtons();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        int bx = left + 160;
        one = addRenderableWidget(Button.builder(Component.literal("1回まわす"), b -> pull(1)).bounds(bx, top + 132, 128, 20).build());
        ten = addRenderableWidget(Button.builder(Component.literal("§e10連（SR以上1つ確定）"), b -> pull(10)).bounds(bx, top + 156, 128, 20).build());
        list = addRenderableWidget(Button.builder(Component.literal("なにが出る？"), b -> showList = !showList).bounds(bx, top + 180, 128, 18).build());
        updateButtons();
    }

    private void updateButtons() {
        boolean busy = waiting || (phase > 0 && phase < 4);
        if (one != null) {
            one.active = !busy && CasinoClient.wallet >= price;
            ten.active = !busy && CasinoClient.wallet >= price * 10L;
        }
    }

    private void pull(int n) {
        if (waiting) return;
        waiting = true;
        showList = false;
        phase = 0;
        items = List.of();
        updateButtons();
        PacketDistributor.sendToServer(new GachaPayloads.Pull(pos, n));
    }

    /** お金の書き方：ライトマンなら「金コイン1枚」、なければ「エメラルド1個」 */
    public static String moneyText(long chips) {
        return money(chips);
    }

    static String money(long chips) {
        if (!CasinoClient.lightman) return chips % 10 == 0 ? "エメラルド" + chips / 10 + "個" : chips + "枚";
        long copper = chips * 10;
        String[] n = {"銅", "鉄", "金", "エメラルド", "ダイヤ", "ネザライト"};
        long[] v = {1, 10, 100, 1000, 10000, 100000};
        StringBuilder sb = new StringBuilder();
        for (int i = 5; i >= 0; i--) if (copper >= v[i]) {
            long k = copper / v[i];
            copper -= k * v[i];
            sb.append(sb.length() > 0 ? "+" : "").append(n[i]).append(k);
        }
        return sb.length() == 0 ? "0" : sb + "コイン";
    }

    private int best() {
        int b = 0;
        for (int r : rar) b = Math.max(b, r);
        return b;
    }

    private static void sound(SoundEvent s, float pitch, float vol) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, vol));
    }

    @Override
    public void tick() {
        ticks++;
        if (phase == 0 || phase == 4) return;
        phaseT++;
        if (phase == 1) {
            if (phaseT % 6 == 0) sound(SoundEvents.CHAIN_HIT, 1.4F + (phaseT / 6 % 2) * 0.2F, 0.6F);
            if (phaseT >= 30) next();
        } else if (phase == 2) {
            if (phaseT == 14) sound(SoundEvents.WOOD_HIT, 1.2F, 0.8F);
            if (phaseT >= 22) next();
        } else if (phase == 3) {
            if (phaseT == 1) {
                int b = best();
                sound(b >= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : b >= 2 ? SoundEvents.PLAYER_LEVELUP : SoundEvents.EXPERIENCE_ORB_PICKUP, b >= 3 ? 1.0F : 1.4F, 0.7F);
            }
            // 10連は1つずつ見せる
            if (phaseT % 4 == 0 && shown < items.size()) {
                int r = rar.get(shown);
                sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8F + r * 0.25F, 0.5F);
                shown++;
            }
            if (shown >= items.size() && phaseT > 12) next();
        }
    }

    private void next() {
        phase++;
        phaseT = 0;
        if (phase == 4) shown = items.size();
        updateButtons();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // 演出中はクリックで飛ばす
        if (phase > 0 && phase < 4 && mx >= left && mx < left + 150) {
            phase = 4;
            shown = items.size();
            updateButtons();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(left, top, left + W, top + H, 0xF0201828);
        g.renderOutline(left, top, W, H, 0xFFF080B0);
        g.drawString(font, "§lガチャガチャ", left + 160, top + 8, 0xFFF0A0C0);
        machine(g, left + 10, top + 10, pt);
        // 右の情報
        int x = left + 160, y = top + 24;
        g.drawString(font, "§7お金：§f" + money(CasinoClient.wallet), x, y, 0xFFFFFF);
        y += 12;
        g.drawString(font, "§71回：§e" + money(price), x, y, 0xFFFFFF);
        y += 12;
        g.drawString(font, "§7レジェンド確定まで：§d" + Math.max(1, pityLeft) + "回", x, y, 0xFFFFFF);
        y += 16;
        for (int i = 4; i >= 0; i--) {
            float rate = i < rates.size() ? rates.get(i) : 0;
            g.fill(x, y + 1, x + 6, y + 7, GachaPool.COLOR[i]);
            g.drawString(font, GachaPool.RARITY[i], x + 10, y, GachaPool.COLOR[i]);
            String rs = (rate == (int) rate ? String.valueOf((int) rate) : String.valueOf(rate)) + "%";
            g.drawString(font, "§7" + rs, x + 128 - font.width(rs), y, 0xFFFFFF);
            y += 11;
        }
        if (waiting) g.drawCenteredString(font, "§7…", left + 75, top + H - 14, 0xFFFFFF);
        if (phase >= 3) results(g, mx, my);
        if (showList) previewList(g, mx, my);
    }

    /** ガチャガチャの本体：丸いガラスにカプセル、下に本体とハンドルと取り出し口 */
    private void machine(GuiGraphics g, int x, int y, float pt) {
        int cx = x + 65, cy = y + 52, r = 46;
        // 台
        g.fill(x + 15, y + 96, x + 115, y + 186, 0xFFD04070);
        g.fill(x + 15, y + 96, x + 115, y + 100, 0xFFF080A0);
        g.fill(x + 10, y + 186, x + 120, y + 192, 0xFF8A2048);
        // ガラスとカプセル
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.sqrt(r * r - dy * dy);
            g.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, 0x50A0D0FF);
        }
        float shake = phase == 1 ? Mth.sin((phaseT + pt) * 1.2F) * 2 : 0;
        int n = 0;
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 7; col++) {
                int px = cx - 36 + col * 11 + (row % 2) * 5, py = cy + 36 - row * 10;
                if ((px - cx) * (px - cx) + (py - cy) * (py - cy) > (r - 6) * (r - 6)) continue;
                if (row > 3 && (col + row) % 3 == 0) continue;
                int c = CAPS[(n++ * 5 + row) % CAPS.length];
                int sx = (int) (shake * ((col % 2) * 2 - 1));
                capsule(g, px + sx, py, 4, c);
            }
        // ひかり
        for (int i = 0; i < 12; i++) g.fill(cx - 30 + i, cy - 30 + i / 2, cx - 28 + i, cy - 28 + i / 2, 0x80FFFFFF);
        // ラベル
        g.fill(x + 30, y + 106, x + 100, y + 120, 0xFFFFF0F4);
        g.drawCenteredString(font, "§c§lGACHA", x + 65, y + 109, 0xFFFFFF);
        // ハンドル（回る）
        int hx = x + 65, hy = y + 140;
        g.fill(hx - 16, hy - 16, hx + 16, hy + 16, 0xFFE0E0E8);
        g.fill(hx - 14, hy - 14, hx + 14, hy + 14, 0xFFB8B8C8);
        float ang = phase == 1 ? (phaseT + pt) / 30F * Mth.TWO_PI : 0;
        for (int i = -12; i <= 12; i++) {
            int px = hx + (int) (Mth.cos(ang) * i), py = hy + (int) (Mth.sin(ang) * i);
            g.fill(px - 2, py - 2, px + 2, py + 2, 0xFF404050);
        }
        // 取り出し口
        int ox = x + 48, oy = y + 162;
        g.fill(ox, oy, ox + 34, oy + 20, 0xFF301018);
        g.fill(ox + 2, oy + 2, ox + 32, oy + 18, 0xFF180810);
        // 落ちてくるカプセル
        if (phase >= 2 && !items.isEmpty()) {
            int c = GachaPool.COLOR[best()];
            if (phase == 2) {
                float t = Math.min(1, (phaseT + pt) / 14F);
                int py = (int) Mth.lerp(t * t, cy + 30, oy + 10);
                int bounce = phaseT > 14 ? (int) (Math.abs(Mth.sin((phaseT - 14 + pt) * 0.8F)) * 4 * (22 - phaseT) / 8F) : 0;
                capsule(g, ox + 17, py - bounce, 8, c);
            } else if (phase == 3 || phase == 4) {
                capsule(g, ox + 17, oy + 10, 8, c);
            }
            if (best() >= 3 && phase >= 2) {
                // 金・虹の光
                int a = (int) (80 + 60 * Mth.sin((ticks + pt) * 0.3F));
                for (int i = 0; i < 8; i++) {
                    double an = i * Math.PI / 4 + (ticks + pt) * 0.05;
                    int ex = ox + 17 + (int) (Math.cos(an) * 16), ey = oy + 10 + (int) (Math.sin(an) * 16);
                    g.fill(ex - 1, ey - 1, ex + 2, ey + 2, a << 24 | (c & 0xFFFFFF));
                }
            }
        }
    }

    private static void capsule(GuiGraphics g, int x, int y, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.sqrt(r * r - dy * dy + 0.5);
            g.fill(x - hw, y + dy, x + hw, y + dy + 1, dy < 0 ? color : 0xFFF4F4F4);
        }
        g.fill(x - r, y, x + r, y + 1, 0x60000000);
        g.fill(x - r / 2, y - r + 1, x - r / 2 + 2, y - r + 3, 0xC0FFFFFF);
    }

    private void results(GuiGraphics g, int mx, int my) {
        // 左の上に重ねて結果を出す
        int x = left + 8, y = top + 8, w = 144, h = 150;
        g.fill(x, y, x + w, y + h, 0xE8100C18);
        g.renderOutline(x, y, w, h, GachaPool.COLOR[best()]);
        if (items.size() == 1) {
            int r = rar.get(0);
            if (shown > 0) {
                int c = GachaPool.COLOR[r];
                for (int i = 0; i < 12; i++) {
                    double an = i * Math.PI / 6 + ticks * 0.04;
                    g.fill(x + w / 2 + (int) (Math.cos(an) * 30), y + 60 + (int) (Math.sin(an) * 30), x + w / 2 + (int) (Math.cos(an) * 30) + 2,
                            y + 60 + (int) (Math.sin(an) * 30) + 2, c);
                }
                g.pose().pushPose();
                g.pose().translate(x + w / 2F - 16, y + 44, 0);
                g.pose().scale(2, 2, 1);
                g.renderItem(items.get(0), 0, 0);
                g.pose().popPose();
                g.drawCenteredString(font, "§l" + GachaPool.RARITY[r], x + w / 2, y + 14, c);
                List<net.minecraft.util.FormattedCharSequence> lines = font.split(items.get(0).getHoverName().copy().append(items.get(0).getCount() > 1 ? " ×" + items.get(0).getCount() : ""), w - 10);
                int ly = y + 100;
                for (var l : lines) {
                    g.drawCenteredString(font, l, x + w / 2, ly, 0xFFFFFF);
                    ly += 10;
                }
            }
        } else {
            g.drawCenteredString(font, "§l10連の結果", x + w / 2, y + 6, 0xFFFFFF);
            for (int i = 0; i < shown && i < items.size(); i++) {
                int ix = x + 8 + (i % 5) * 27, iy = y + 26 + (i / 5) * 40;
                int c = GachaPool.COLOR[rar.get(i)];
                g.fill(ix - 2, iy - 2, ix + 22, iy + 22, c & 0x60FFFFFF | 0x60000000);
                g.renderOutline(ix - 2, iy - 2, 24, 24, c);
                g.renderItem(items.get(i), ix + 2, iy + 2);
                g.renderItemDecorations(font, items.get(i), ix + 2, iy + 2);
                g.drawCenteredString(font, GachaPool.SHORT[rar.get(i)], ix + 10, iy + 24, c);
                if (mx >= ix - 2 && mx < ix + 22 && my >= iy - 2 && my < iy + 22) hover = items.get(i);
            }
        }
        if (phase == 4) g.drawCenteredString(font, "§8もう一度まわせるよ", x + w / 2, y + h - 12, 0xFFFFFF);
        if (hover != null) {
            g.renderTooltip(font, hover, mx, my);
            hover = null;
        }
    }

    private ItemStack hover;

    private void previewList(GuiGraphics g, int mx, int my) {
        int x = left + 8, y = top + 8, w = 144, h = 194;
        g.fill(x, y, x + w, y + h, 0xF4100C18);
        g.renderOutline(x, y, w, h, 0xFFF080B0);
        g.drawCenteredString(font, "§lなにが出る？（一部）", x + w / 2, y + 4, 0xFFFFFF);
        int yy = y + 16;
        int lastR = -1, col = 0;
        ItemStack hv = null;
        for (int r = 4; r >= 0; r--) {
            int c = GachaPool.COLOR[r];
            g.drawString(font, GachaPool.RARITY[r], x + 4, yy, c);
            yy += 9;
            col = 0;
            for (int i = 0; i < preview.size(); i++) {
                if (previewR.get(i) != r) continue;
                if (col == 8) {
                    col = 0;
                    yy += 17;
                }
                int ix = x + 4 + col * 17;
                g.renderItem(preview.get(i), ix, yy);
                if (mx >= ix && mx < ix + 16 && my >= yy && my < yy + 16) hv = preview.get(i);
                col++;
            }
            yy += 19;
        }
        if (hv != null) g.renderTooltip(font, hv, mx, my);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
