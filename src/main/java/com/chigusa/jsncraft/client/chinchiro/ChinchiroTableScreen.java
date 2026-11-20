package com.chigusa.jsncraft.client.chinchiro;

import com.chigusa.jsncraft.chinchiro.ChinchiroPayloads;
import com.chigusa.jsncraft.chinchiro.ChinchiroTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

/** チンチロ対戦の卓：まんなかにどんぶり、下に4つの席、右に流れ */
public class ChinchiroTableScreen extends Screen {
    private static final int W = 340, H = 236;
    private static final String[] PHASE = {"待ち", "子がかける時間", "親がふる", "子がふる", "精算"};
    private final BlockPos pos;
    private ChinchiroPayloads.State st;
    private long receivedAt;
    private int left, top, ticks, lastRollId = -1, animUntil;
    private final RandomSource rnd = RandomSource.create();
    private String buttonsKey = "";
    private static long closedAt;

    public ChinchiroTableScreen(BlockPos pos) {
        super(Component.literal("チンチロ対戦"));
        this.pos = pos;
    }

    public static void onState(ChinchiroPayloads.State s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ChinchiroTableScreen c && c.pos.equals(s.pos())) c.update(s);
        else if (mc.screen == null && System.currentTimeMillis() - closedAt > 1500) {
            ChinchiroTableScreen c = new ChinchiroTableScreen(s.pos());
            mc.setScreen(c);
            c.update(s);
        }
    }

    private void update(ChinchiroPayloads.State s) {
        if (st != null && s.rollId() != lastRollId && s.wait() > 0) {
            animUntil = ticks + Math.min(30, s.wait());
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BONE_BLOCK_HIT, 1.6F, 0.7F));
        }
        lastRollId = s.rollId();
        st = s;
        receivedAt = System.currentTimeMillis();
        rebuild();
    }

    private int mySeat() {
        if (st == null) return -1;
        for (int i = 0; i < st.flags().size(); i++) if ((st.flags().get(i) & 2) != 0) return i;
        return -1;
    }

    private void act(int a, int v) {
        PacketDistributor.sendToServer(new ChinchiroPayloads.Act(pos, a, v));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        buttonsKey = "";
        rebuild();
    }

    /** 状態に合わせてボタンを並べなおす */
    private void rebuild() {
        if (st == null || width == 0) return;
        int me = mySeat();
        boolean myTurn = me >= 0 && st.turn() == me && (st.phase() == ChinchiroTableBlockEntity.PARENT || st.phase() == ChinchiroTableBlockEntity.CHILD) && st.wait() == 0;
        boolean canBet = me >= 0 && st.phase() == ChinchiroTableBlockEntity.BETTING && me != st.parent() && st.bets().get(me) == 0;
        String key = st.phase() + "|" + me + "|" + myTurn + "|" + canBet;
        if (key.equals(buttonsKey)) return;
        buttonsKey = key;
        clearWidgets();
        int y = top + H - 26, x = left + 10;
        if (me < 0) {
            addRenderableWidget(Button.builder(Component.literal("§l座る"), b -> act(1, 0)).bounds(x, y, 80, 20).build());
        } else {
            if (st.phase() == ChinchiroTableBlockEntity.WAITING) {
                addRenderableWidget(Button.builder(Component.literal("§a§l勝負を始める"), b -> act(3, 0)).bounds(x, y, 90, 20).build());
                addRenderableWidget(Button.builder(Component.literal("CPUを入れる"), b -> act(6, 0)).bounds(x + 94, y, 76, 20).build());
                addRenderableWidget(Button.builder(Component.literal("CPUを帰す"), b -> act(7, 0)).bounds(x + 174, y, 70, 20).build());
            }
            if (canBet) {
                int bx = x;
                for (int b : ChinchiroTableBlockEntity.BETS) {
                    addRenderableWidget(Button.builder(Component.literal("§e" + b + "枚"), btn -> act(4, b)).bounds(bx, y, 56, 20).build());
                    bx += 60;
                }
            }
            if (myTurn) addRenderableWidget(Button.builder(Component.literal("§6§lふる！"), b -> act(5, 0)).bounds(x, y, 120, 20).build());
            addRenderableWidget(Button.builder(Component.literal("立つ"), b -> act(2, 0)).bounds(left + W - 60, y, 50, 20).build());
        }
    }

    @Override
    public void tick() {
        ticks++;
        if (ticks % 5 == 0) act(0, 0);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        g.fill(left, top, left + W, top + H, 0xF0201810);
        g.renderOutline(left, top, W, H, 0xFFC09040);
        g.drawString(font, "§6§l⚀ チンチロ対戦", left + 10, top + 8, 0xFFFFFF);
        if (st == null) return;
        String chips = "チップ：" + st.chips() + "枚";
        g.drawString(font, "§7" + chips, left + W - 10 - font.width(chips), top + 8, 0xFFFFFF);
        int tl = Math.max(0, st.timer() - (int) ((System.currentTimeMillis() - receivedAt) / 50));
        String ph = "§e" + PHASE[Math.max(0, Math.min(4, st.phase()))];
        if (st.phase() == ChinchiroTableBlockEntity.BETTING || ((st.phase() == 2 || st.phase() == 3) && st.wait() == 0)) ph += " §7" + (tl / 20 + 1) + "秒";
        if ((st.phase() == 2 || st.phase() == 3) && st.turn() >= 0) ph += " §f― " + st.names().get(st.turn()) + " の番";
        g.drawString(font, ph, left + 10, top + 20, 0xFFFFFF);
        // どんぶり
        int bx = left + 10, by = top + 34, bw = 200, bh = 82;
        g.fill(bx, by, bx + bw, by + bh, 0xFF3A2010);
        g.fill(bx + 6, by + 6, bx + bw - 6, by + bh - 6, 0xFFE8E0D0);
        g.fill(bx + 12, by + 12, bx + bw - 12, by + bh - 12, 0xFFF4F0E6);
        int show = st.turn() >= 0 ? st.turn() : lastRoller();
        if (show >= 0) {
            boolean rolling = ticks < animUntil;
            for (int k = 0; k < 3; k++) {
                int v = rolling ? 1 + rnd.nextInt(6) : st.dice().get(show * 3 + k);
                die(g, bx + 52 + k * 34 + (rolling ? rnd.nextInt(5) - 2 : 0), by + 22 + (rolling ? rnd.nextInt(5) - 2 : 0), v);
            }
            g.drawCenteredString(font, "§0" + st.names().get(show), bx + bw / 2, by + 10, 0xFFFFFF);
            if (!rolling && !st.hands().get(show).isEmpty()) g.drawCenteredString(font, "§4§l" + st.hands().get(show), bx + bw / 2, by + 56, 0xFFFFFF);
        } else {
            g.drawCenteredString(font, "§8座って、勝負を始めよう", bx + bw / 2, by + bh / 2 - 4, 0xFFFFFF);
        }
        // 流れ
        int lx = left + 218, ly = top + 34;
        g.fill(lx, ly, left + W - 10, ly + bh, 0x60000000);
        int yy = ly + 4;
        for (String l : st.log()) {
            String c = font.plainSubstrByWidth(l, left + W - 14 - lx - 4);
            g.drawString(font, "§7" + c, lx + 4, yy, 0xFFFFFF);
            yy += 11;
        }
        // 席
        int sw = (W - 20 - 9) / 4;
        for (int i = 0; i < 4; i++) {
            int sx = left + 10 + i * (sw + 3), sy = top + 122;
            int f = st.flags().get(i);
            boolean empty = (f & 4) != 0, me = (f & 2) != 0;
            g.fill(sx, sy, sx + sw, sy + 80, me ? 0xFF3A3020 : 0xFF2A2018);
            if (st.turn() == i && (st.phase() == 2 || st.phase() == 3)) g.renderOutline(sx, sy, sw, 80, ticks / 5 % 2 == 0 ? 0xFFF0D040 : 0xFFA08020);
            if (empty) {
                g.drawCenteredString(font, "§8空き", sx + sw / 2, sy + 34, 0xFFFFFF);
                continue;
            }
            boolean parent = i == st.parent() && st.phase() != 0;
            String name = font.plainSubstrByWidth(st.names().get(i), sw - (parent ? 18 : 6));
            g.drawString(font, (me ? "§e" : "§f") + name, sx + 4, sy + 4, 0xFFFFFF);
            if (parent) {
                g.fill(sx + sw - 14, sy + 2, sx + sw - 2, sy + 13, 0xFFC03030);
                g.drawString(font, "親", sx + sw - 13, sy + 4, 0xFFFFFF);
            }
            if ((f & 1) != 0) g.drawString(font, "§8CPU", sx + 4, sy + 14, 0xFFFFFF);
            long bet = st.bets().get(i);
            if (!parent && st.phase() != 0) g.drawString(font, bet > 0 ? "§eかけ " + bet : "§8かけなし", sx + 4, sy + 24, 0xFFFFFF);
            boolean rollingHere = ticks < animUntil && st.turn() == i;
            for (int k = 0; k < 3; k++) {
                int v = st.dice().get(i * 3 + k);
                if (rollingHere) v = 1 + rnd.nextInt(6);
                smallDie(g, sx + 4 + k * 15, sy + 36, v);
            }
            String hand = st.hands().get(i);
            if (!hand.isEmpty() && !rollingHere) g.drawString(font, "§b" + font.plainSubstrByWidth(hand, sw - 6), sx + 4, sy + 52, 0xFFFFFF);
            long net = st.nets().get(i);
            if (net != 0) g.drawString(font, (net > 0 ? "§a+" : "§c") + net, sx + 4, sy + 64, 0xFFFFFF);
        }
        g.drawString(font, "§8ピンゾロ5倍・ゾロ目3倍・シゴロ2倍・ヒフミ2倍払い／親は6の目以上で総取り、1の目以下で総払い", left + 10, top + 206, 0xFFFFFF);
    }

    private int lastRoller() {
        for (int i = 0; i < 4; i++) if ((st.flags().get(i) & 4) == 0 && st.dice().get(i * 3) > 0 && i == st.parent()) return i;
        return -1;
    }

    private void die(GuiGraphics g, int x, int y, int v) {
        g.fill(x - 1, y - 1, x + 25, y + 25, 0xFF1A1A1A);
        g.fill(x, y, x + 24, y + 24, v == 0 ? 0xFF6A6A6A : 0xFFF8F4EA);
        pips(g, x, y, v, 24);
    }

    private void smallDie(GuiGraphics g, int x, int y, int v) {
        g.fill(x - 1, y - 1, x + 13, y + 13, 0xFF1A1A1A);
        g.fill(x, y, x + 12, y + 12, v == 0 ? 0xFF4A4A4A : 0xFFF8F4EA);
        pips(g, x, y, v, 12);
    }

    private static void pips(GuiGraphics g, int x, int y, int v, int size) {
        if (v == 0) return;
        float k = size / 24F;
        int col = v == 1 ? 0xFFD01818 : 0xFF1A1A1A;
        int s = Math.max(2, Math.round((v == 1 ? 6 : 4) * k));
        int[][] p = switch (v) {
            case 1 -> new int[][]{{12, 12}};
            case 2 -> new int[][]{{6, 6}, {18, 18}};
            case 3 -> new int[][]{{6, 6}, {12, 12}, {18, 18}};
            case 4 -> new int[][]{{6, 6}, {18, 6}, {6, 18}, {18, 18}};
            case 5 -> new int[][]{{6, 6}, {18, 6}, {12, 12}, {6, 18}, {18, 18}};
            default -> new int[][]{{6, 5}, {18, 5}, {6, 12}, {18, 12}, {6, 19}, {18, 19}};
        };
        for (int[] q : p) {
            int cx = x + Math.round(q[0] * k), cy = y + Math.round(q[1] * k);
            g.fill(cx - s / 2, cy - s / 2, cx + (s + 1) / 2, cy + (s + 1) / 2, col);
        }
    }

    @Override
    public void removed() {
        closedAt = System.currentTimeMillis();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
