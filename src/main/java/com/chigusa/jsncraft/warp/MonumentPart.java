package com.chigusa.jsncraft.warp;

import net.minecraft.util.StringRepresentable;

/** モニュメントの部分：真ん中の台・ふちの段・角のオベリスク・柱・柱の上 */
public enum MonumentPart implements StringRepresentable {
    CENTER, EDGE, CORNER, PILLAR, TOP;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
