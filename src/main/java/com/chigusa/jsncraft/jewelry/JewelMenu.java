package com.chigusa.jsncraft.jewelry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 細工台の画面の中身。
 * ボタン：0 作る / 1 磨く / 2 はめ込む / 3 特別を作る / 100+n 種類 n を選ぶ / 200+n 特別 n を選ぶ
 * data：0 選んだ種類 / 1 選んだ特別 / 2 細工の腕 / 3〜6 状態（作る・研磨・はめ込み・特別）/ 7 宝飾師か
 */
public class JewelMenu extends AbstractContainerMenu {
    public static final int BTN_MAKE = 0, BTN_POLISH = 1, BTN_SOCKET = 2, BTN_UNIQUE = 3, BTN_UNSOCKET = 4, BTN_KIND = 100, BTN_PICK_UNIQUE = 200;
    private static final int COUNT = 9;

    private final Container container;
    private final ContainerData data;
    @Nullable private final JewelTableBlockEntity be;
    private int kind = -1, unique = -1;

    public JewelMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof JewelTableBlockEntity b ? b : null, true);
    }

    public JewelMenu(int id, Inventory inv, JewelTableBlockEntity be) {
        this(id, inv, be, false);
    }

    private JewelMenu(int id, Inventory inv, @Nullable JewelTableBlockEntity be, boolean client) {
        super(ModJewelry.TABLE_MENU.get(), id);
        this.be = be;
        this.container = be != null ? be.items() : new SimpleContainer(4);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(COUNT) : new ContainerData() {
            @Override
            public int get(int i) {
                if (!(player instanceof ServerPlayer sp)) return 0;
                return switch (i) {
                    case 0 -> kind;
                    case 1 -> unique;
                    case 2 -> JewelryService.jewelLevel(sp);
                    case 3 -> JewelryService.checkMake(sp, be, kind >= 0 ? Jewels.KINDS.get(kind) : null);
                    case 4 -> JewelryService.checkPolish(sp, be);
                    case 5 -> JewelryService.checkSocket(sp, be);
                    case 6 -> JewelryService.checkUnique(sp, be, unique >= 0 ? Jewels.UNIQUES.get(unique) : null);
                    case 7 -> JewelryService.isMaster(sp) ? 1 : 0;
                    case 8 -> JewelryService.checkUnsocket(sp, be);
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return COUNT;
            }
        };
        addSlot(new Slot(container, 0, 16, 24));
        for (int i = 0; i < 3; i++) addSlot(new Slot(container, 1 + i, 16 + i * 18, 52));
        int invX = 48, invY = 146;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, invX + c * 18, invY + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, invX + c * 18, invY + 58));
        addDataSlots(data);
    }

    public int kind() { return data.get(0); }
    public int unique() { return data.get(1); }
    public int jewelLevel() { return data.get(2); }
    public int status(int mode) { return data.get(3 + mode); }
    public boolean master() { return data.get(7) == 1; }
    public int unsocketStatus() { return data.get(8); }

    public ItemStack slotItem(int i) {
        return container.getItem(i);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;
        if (id >= BTN_PICK_UNIQUE) {
            if (id - BTN_PICK_UNIQUE >= Jewels.UNIQUES.size()) return false;
            unique = id - BTN_PICK_UNIQUE;
            return true;
        }
        if (id >= BTN_KIND) {
            if (id - BTN_KIND >= Jewels.KINDS.size()) return false;
            kind = id - BTN_KIND;
            return true;
        }
        return switch (id) {
            case BTN_MAKE -> kind >= 0 && JewelryService.make(sp, be, Jewels.KINDS.get(kind));
            case BTN_POLISH -> JewelryService.polish(sp, be);
            case BTN_SOCKET -> JewelryService.socket(sp, be);
            case BTN_UNSOCKET -> JewelryService.unsocket(sp, be);
            case BTN_UNIQUE -> unique >= 0 && JewelryService.makeUnique(sp, be, Jewels.UNIQUES.get(unique));
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 4) {
            if (!moveItemStackTo(stack, 4, slots.size(), true)) return ItemStack.EMPTY;
        } else if (ModJewelry.gemOf(stack.getItem(), false) != null || stack.getItem() instanceof OrbItem) {
            if (!moveItemStackTo(stack, 1, 4, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(be.getBlockPos().getCenter()) < 64);
    }
}
