package com.chigusa.jsncraft.client.card;

import com.chigusa.jsncraft.card.CardItem;
import com.chigusa.jsncraft.card.CardList;
import com.chigusa.jsncraft.card.CardStats;
import com.chigusa.jsncraft.client.gacha.FigureRendererAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** カードの絵：レア度の色のふち・モンスター・名前・星 */
public final class CardArt {

    /** owned=false なら影（？）だけ */
    public static void draw(GuiGraphics g, CardList.Card c, int x, int y, int w, int h, boolean owned, int ticks) {
        int col = CardStats.COLOR[c.rarity()];
        g.fill(x, y, x + w, y + h, col);
        // 上のほうは光らせる（ウルトラレア以上はキラキラ）
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF1A1A24);
        int artH = h - 16;
        int top = 0xFF000000 | mix(col, 0x202030, 0.75F), bot = 0xFF101018;
        g.fillGradient(x + 3, y + 3, x + w - 3, y + 3 + artH - 3, top, bot);
        if (owned) {
            Entity e = FigureRendererAccess.entity(c.entity());
            if (e instanceof LivingEntity le) {
                float big = Math.max(le.getBbHeight(), le.getBbWidth());
                float scale = Math.min((artH - 6) / Math.max(0.3F, big), (w - 6) / Math.max(0.3F, le.getBbWidth() * 1.1F)) * 0.9F;
                float ob = le.yBodyRot, oh = le.yHeadRot, obo = le.yBodyRotO, oho = le.yHeadRotO, oy = le.getYRot();
                le.yBodyRot = le.yBodyRotO = 180 + 25;
                le.yHeadRot = le.yHeadRotO = 180 + 25;
                le.setYRot(180 + 25);
                // 切りぬきは画面の座標で指定するので、いまの拡大・移動を計算に入れる（カードがめくれる途中など）
                org.joml.Matrix4f m = g.pose().last().pose();
                org.joml.Vector3f a = m.transformPosition(new org.joml.Vector3f(x + 3, y + 3, 0)), b = m.transformPosition(new org.joml.Vector3f(x + w - 3, y + artH, 0));
                g.pose().pushPose();
                g.pose().setIdentity();
                g.enableScissor((int) Math.floor(Math.min(a.x, b.x)), (int) Math.floor(Math.min(a.y, b.y)), (int) Math.ceil(Math.max(a.x, b.x)), (int) Math.ceil(Math.max(a.y, b.y)));
                g.pose().popPose();
                try {
                    InventoryScreen.renderEntityInInventory(g, x + w / 2F, y + 3 + (artH - 3) / 2F + le.getBbHeight() * scale / 2F, scale,
                            new Vector3f(0, 0, 0), new Quaternionf().rotateZ((float) Math.PI), null, le);
                } catch (Throwable ignored) {
                }
                g.disableScissor();
                le.yBodyRot = ob;
                le.yHeadRot = oh;
                le.yBodyRotO = obo;
                le.yHeadRotO = oho;
                le.setYRot(oy);
            }
            if (c.rarity() >= 3) {
                // キラキラ
                for (int i = 0; i < 4; i++) {
                    int sx = x + 4 + (int) ((ticks * 1.7 + i * 37) % (w - 8)), sy = y + 4 + (int) ((i * 23 + ticks * 0.6) % (artH - 6));
                    g.fill(sx, sy, sx + 1, sy + 1, 0xC0FFFFFF);
                }
            }
        } else {
            var f = Minecraft.getInstance().font;
            g.drawCenteredString(f, "§8？", x + w / 2, y + artH / 2 - 2, 0xFFFFFF);
        }
        // 下：星と名前
        var f = Minecraft.getInstance().font;
        String stars = "★".repeat(c.rarity() + 1);
        g.pose().pushPose();
        g.pose().translate(x + w / 2F, y + h - 13, 0);
        g.pose().scale(0.5F, 0.5F, 1);
        g.drawCenteredString(f, stars, 0, 0, col);
        String name = owned ? CardItem.name(c).getString() : "？？？";
        int maxW = (w - 4) * 2;
        if (f.width(name) > maxW) name = f.plainSubstrByWidth(name, maxW - f.width("…")) + "…";
        g.drawCenteredString(f, name, 0, 11, 0xFFFFFF);
        g.pose().popPose();
    }

    public static List<Component> tooltip(CardList.Card c, int count, boolean owned) {
        List<Component> t = new ArrayList<>();
        if (!owned) {
            t.add(Component.literal("§8？？？"));
            t.add(Component.literal(CardStats.FMT[c.rarity()] + "★".repeat(c.rarity() + 1) + " " + CardStats.RARITY[c.rarity()]));
            return t;
        }
        t.add(Component.literal(CardStats.FMT[c.rarity()]).append(CardItem.name(c)));
        t.add(Component.literal(CardStats.FMT[c.rarity()] + "★".repeat(c.rarity() + 1) + " " + CardStats.RARITY[c.rarity()]));
        t.add(Component.literal("§a" + CardStats.describe(c.stat(), CardStats.amount(c))));
        if (count > 0) t.add(Component.literal("§7持っている数：" + count));
        return t;
    }

    static int mix(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return (int) (ar + (br - ar) * t) << 16 | (int) (ag + (bg - ag) * t) << 8 | (int) (ab + (bb - ab) * t);
    }

    private CardArt() {}
}
