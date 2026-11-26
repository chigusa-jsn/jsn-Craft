package com.chigusa.jsncraft.loginbonus;

import com.chigusa.jsncraft.cooking.ModCooking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * ログインボーナスの編集（OPだけ）。上の9マスに置いたアイテムが、その日のご褒美になる（置いた物はコピーされて、なくならない）。
 * data: 0 何日目、1 日数、2 その日のお金、3 連続のおまけの間隔、4 連続のおまけのお金
 * ボタン: 0/1 前の日・次の日、2..5 お金 -10 +10 -100 +100、6 日を足す、7 最後の日を消す、8/9 おまけ -10 +10、10/11 間隔 -1 +1
 */
public class LoginBonusEditMenu extends AbstractContainerMenu {
    public static final DeferredHolder<MenuType<?>, MenuType<LoginBonusEditMenu>> TYPE = ModCooking.MENUS.register("login_bonus_edit",
            () -> new MenuType<>(LoginBonusEditMenu::new, FeatureFlags.VANILLA_SET));

    public static void init() {}

    @Nullable private final MinecraftServer server;
    private final SimpleContainer slots = new SimpleContainer(LoginBonusSaved.SLOTS);
    private final ContainerData data;
    private int day;

    public LoginBonusEditMenu(int id, Inventory inv) {
        this(id, inv, null);
    }

    public LoginBonusEditMenu(int id, Inventory inv, @Nullable MinecraftServer server) {
        super(TYPE.get(), id);
        this.server = server;
        if (server == null) {
            data = new SimpleContainerData(5);
        } else {
            data = new ContainerData() {
                @Override
                public int get(int i) {
                    LoginBonusSaved s = saved();
                    return switch (i) {
                        case 0 -> day;
                        case 1 -> s.days.size();
                        case 2 -> day < s.days.size() ? s.days.get(day).money : 0;
                        case 3 -> s.streakEvery;
                        default -> s.streakMoney;
                    };
                }

                @Override
                public void set(int i, int v) {
                }

                @Override
                public int getCount() {
                    return 5;
                }
            };
            load();
        }
        for (int c = 0; c < LoginBonusSaved.SLOTS; c++) addSlot(new Slot(slots, c, 8 + c * 18, 52));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 104 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 162));
        addDataSlots(data);
    }

    private LoginBonusSaved saved() {
        return LoginBonusSaved.get(server);
    }

    public int day() { return data.get(0); }
    public int dayCount() { return data.get(1); }
    public int money() { return data.get(2); }
    public int streakEvery() { return data.get(3); }
    public int streakMoney() { return data.get(4); }

    private void load() {
        LoginBonusSaved s = saved();
        slots.clearContent();
        if (day < s.days.size()) {
            List<ItemStack> items = s.days.get(day).items;
            for (int i = 0; i < Math.min(items.size(), LoginBonusSaved.SLOTS); i++) slots.setItem(i, items.get(i).copy());
        }
    }

    private void store() {
        if (server == null) return;
        LoginBonusSaved s = saved();
        if (day >= s.days.size()) return;
        List<ItemStack> items = s.days.get(day).items;
        items.clear();
        for (int i = 0; i < LoginBonusSaved.SLOTS; i++) if (!slots.getItem(i).isEmpty()) items.add(slots.getItem(i).copy());
        s.setDirty();
        LoginBonus.invalidate();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (server == null || !player.hasPermissions(2)) return false;
        LoginBonusSaved s = saved();
        store();
        switch (id) {
            case 0 -> day = Math.max(0, day - 1);
            case 1 -> day = Math.min(s.days.size() - 1, day + 1);
            case 2, 3, 4, 5 -> {
                int d = id == 2 ? -10 : id == 3 ? 10 : id == 4 ? -100 : 100;
                LoginBonusSaved.Day dd = s.days.get(day);
                dd.money = Math.max(0, Math.min(30000, dd.money + d));
            }
            case 6 -> {
                if (s.days.size() < 63) {
                    s.days.add(new LoginBonusSaved.Day());
                    day = s.days.size() - 1;
                }
            }
            case 7 -> {
                if (s.days.size() > 1) {
                    s.days.remove(s.days.size() - 1);
                    day = Math.min(day, s.days.size() - 1);
                }
            }
            case 8, 9 -> s.streakMoney = Math.max(0, Math.min(30000, s.streakMoney + (id == 8 ? -10 : 10)));
            case 10, 11 -> s.streakEvery = Math.max(1, Math.min(28, s.streakEvery + (id == 10 ? -1 : 1)));
            default -> {
                return false;
            }
        }
        s.edited = true;
        s.setDirty();
        LoginBonus.invalidate();
        load();
        return true;
    }

    /**
     * ご褒美のマスは「見本」：持っているアイテムで左クリック → そのままコピー、右クリック → 1個だけコピー。
     * 手ぶらで左クリック → 消す、右クリック → 1個ふやす。持ち物のアイテムはへらない。
     */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (slotId < 0 || slotId >= LoginBonusSaved.SLOTS) {
            super.clicked(slotId, button, type, player);
            return;
        }
        if (type != net.minecraft.world.inventory.ClickType.PICKUP && type != net.minecraft.world.inventory.ClickType.QUICK_MOVE) return;
        ItemStack carried = getCarried();
        ItemStack cur = slots.getItem(slotId);
        if (type == net.minecraft.world.inventory.ClickType.QUICK_MOVE) {
            slots.setItem(slotId, ItemStack.EMPTY);
        } else if (!carried.isEmpty()) {
            slots.setItem(slotId, carried.copyWithCount(button == 1 ? 1 : carried.getCount()));
        } else if (!cur.isEmpty()) {
            if (button == 1) cur.setCount(Math.min(cur.getMaxStackSize(), cur.getCount() + 1));
            else slots.setItem(slotId, ItemStack.EMPTY);
        }
        store();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        store();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        if (index >= LoginBonusSaved.SLOTS) {
            // 持ち物から：コピーを空いているマスに置く（持ち物はそのまま）
            for (int i = 0; i < LoginBonusSaved.SLOTS; i++) {
                if (slots.getItem(i).isEmpty()) {
                    slots.setItem(i, stack.copy());
                    store();
                    break;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.hasPermissions(2);
    }
}
