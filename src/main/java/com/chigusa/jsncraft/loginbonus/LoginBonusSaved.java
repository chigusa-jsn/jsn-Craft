package com.chigusa.jsncraft.loginbonus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * ゲームの中で編集したログインボーナス（ワールドに保存）。
 * これがあると login_bonus.json より優先する。
 */
public final class LoginBonusSaved extends SavedData {
    public static final int SLOTS = 9;

    public static final class Day {
        public final List<ItemStack> items = new ArrayList<>();
        public int money;
    }

    public final List<Day> days = new ArrayList<>();
    public int streakEvery = 7;
    public int streakMoney = 100;
    public boolean edited;

    public static LoginBonusSaved get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LoginBonusSaved::new, LoginBonusSaved::load), "jsn_craft_login_bonus");
    }

    static LoginBonusSaved load(CompoundTag t, HolderLookup.Provider reg) {
        LoginBonusSaved d = new LoginBonusSaved();
        d.edited = t.getBoolean("edited");
        d.streakEvery = t.contains("streak_every") ? t.getInt("streak_every") : 7;
        d.streakMoney = t.getInt("streak_money");
        for (Tag dt : t.getList("days", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) dt;
            Day day = new Day();
            day.money = c.getInt("money");
            for (Tag it : c.getList("items", Tag.TAG_COMPOUND)) {
                ItemStack s = ItemStack.parseOptional(reg, (CompoundTag) it);
                if (!s.isEmpty()) day.items.add(s);
            }
            d.days.add(day);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider reg) {
        t.putBoolean("edited", edited);
        t.putInt("streak_every", streakEvery);
        t.putInt("streak_money", streakMoney);
        ListTag list = new ListTag();
        for (Day day : days) {
            CompoundTag c = new CompoundTag();
            c.putInt("money", day.money);
            ListTag items = new ListTag();
            for (ItemStack s : day.items) if (!s.isEmpty()) items.add(s.save(reg));
            c.put("items", items);
            list.add(c);
        }
        t.put("days", list);
        return t;
    }
}
