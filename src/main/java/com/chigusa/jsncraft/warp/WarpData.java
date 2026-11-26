package com.chigusa.jsncraft.warp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** ワープ地点の一覧（OPが登録。ワールドに保存）。 */
public final class WarpData extends SavedData {
    public static final class Point {
        public String name;
        public String dim;
        public double x, y, z;
        public float yaw;
        public int cost;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("name", name);
            t.putString("dim", dim);
            t.putDouble("x", x);
            t.putDouble("y", y);
            t.putDouble("z", z);
            t.putFloat("yaw", yaw);
            t.putInt("cost", cost);
            return t;
        }

        static Point load(CompoundTag t) {
            Point p = new Point();
            p.name = t.getString("name");
            p.dim = t.getString("dim");
            p.x = t.getDouble("x");
            p.y = t.getDouble("y");
            p.z = t.getDouble("z");
            p.yaw = t.getFloat("yaw");
            p.cost = t.getInt("cost");
            return p;
        }
    }

    public final List<Point> points = new ArrayList<>();

    public static WarpData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(WarpData::new, WarpData::load), "jsn_craft_warps");
    }

    static WarpData load(CompoundTag t, HolderLookup.Provider reg) {
        WarpData d = new WarpData();
        for (Tag e : t.getList("points", Tag.TAG_COMPOUND)) d.points.add(Point.load((CompoundTag) e));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider reg) {
        ListTag l = new ListTag();
        for (Point p : points) l.add(p.save());
        t.put("points", l);
        return t;
    }

    public Point find(String name) {
        for (Point p : points) if (p.name.equals(name)) return p;
        return null;
    }
}
