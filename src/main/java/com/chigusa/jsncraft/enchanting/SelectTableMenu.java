package com.chigusa.jsncraft.enchanting;

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
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/** 選べる付与台の画面。data: 0 付与の腕（-1 なら付けられない）、1 刻印師なら 1。ボタン id = 番号 * 8 + レベル */
public class SelectTableMenu extends AbstractContainerMenu {
    @Nullable private final SelectTableBlockEntity be;
    private final Container container;
    private final ContainerData data;

    public SelectTableMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof SelectTableBlockEntity b ? b : null, true);
    }

    public SelectTableMenu(int id, Inventory inv, SelectTableBlockEntity be) {
        this(id, inv, be, false);
    }

    private SelectTableMenu(int id, Inventory inv, @Nullable SelectTableBlockEntity be, boolean client) {
        super(SelectEnchant.MENU.get(), id);
        this.be = be;
        this.container = be != null && !client ? be.items() : new SimpleContainer(2);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(2) : new ContainerData() {
            @Override
            public int get(int i) {
                if (!(player instanceof ServerPlayer sp)) return -1;
                return i == 0 ? EnchantingService.enchantLevel(sp) : EnchantingService.isMaster(sp) ? 1 : 0;
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        addSlot(new Slot(container, 0, 15, 30) {
            @Override
            public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(container, 1, 15, 56) {
            @Override
            public boolean mayPlace(ItemStack s) { return s.is(Items.LAPIS_LAZULI); }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 120 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 178));
        addDataSlots(data);
    }

    public int jobLevel() {
        int v = data.get(0);
        return v > 32767 ? v - 65536 : v;
    }

    public boolean master() { return data.get(1) == 1; }

    public ItemStack target() { return slots.get(0).getItem(); }

    public int lapis() {
        ItemStack s = slots.get(1).getItem();
        return s.is(Items.LAPIS_LAZULI) ? s.getCount() : 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;
        return SelectEnchant.apply(sp, be, id / 8, id % 8);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.LAPIS_LAZULI)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            if (slots.get(0).hasItem()) return ItemStack.EMPTY;
            slots.get(0).setByPlayer(stack.split(1));
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
