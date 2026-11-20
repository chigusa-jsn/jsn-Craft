package com.chigusa.jsncraft.client.gacha;

import net.minecraft.world.entity.Entity;

/** フィギュア用にとってあるモンスターを、カードの絵でも使う */
public final class FigureRendererAccess {
    public static Entity entity(String id) {
        return FigureRenderer.entity(id);
    }

    private FigureRendererAccess() {}
}
