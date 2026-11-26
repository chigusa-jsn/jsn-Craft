package com.chigusa.jsncraft.farming;

import com.chigusa.jsncraft.job.JobLevels;
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

/** 交配台の画面。data: 0 交配できるか（check の値）/ 1 見ている人が農家か */
public class CrossbreedMenu extends AbstractContainerMenu {
    @Nullable private final CrossbreedBlockEntity be;
    private final Container container;
    private final ContainerData data;

    public CrossbreedMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof CrossbreedBlockEntity b ? b : null, true);
    }

    public CrossbreedMenu(int id, Inventory inv, CrossbreedBlockEntity be) {
        this(id, inv, be, false);
    }

    private CrossbreedMenu(int id, Inventory inv, @Nullable CrossbreedBlockEntity be, boolean client) {
        super(ModFarming.CROSSBREED_MENU.get(), id);
        this.be = be;
        this.container = be != null && !client ? be.items() : new SimpleContainer(3);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(2) : new ContainerData() {
            @Override
            public int get(int i) {
                return i == 0 ? be.check() : (player instanceof ServerPlayer sp && JobLevels.farmer(sp) >= 0 ? 1 : 0);
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        addSlot(new Slot(container, 0, 30, 30) {
            @Override
            public boolean mayPlace(ItemStack s) { return ModFarming.isSeed(s); }
        });
        addSlot(new Slot(container, 1, 66, 30) {
            @Override
            public boolean mayPlace(ItemStack s) { return ModFarming.isSeed(s); }
        });
        addSlot(new Slot(container, 2, 130, 30) {
            @Override
            public boolean mayPlace(ItemStack s) { return false; }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int state() { return data.get(0); }
    public boolean farmer() { return data.get(1) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return id == 0 && be != null && player instanceof ServerPlayer sp && be.breed(sp);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!ModFarming.isSeed(stack)) return ItemStack.EMPTY;
            // 1つずつ左右に分けて入れる
            ItemStack one = stack.split(1);
            if (!moveItemStackTo(one, 0, 1, false) && !moveItemStackTo(one, 1, 2, false)) { stack.grow(one.getCount()); return ItemStack.EMPTY; }
            if (!stack.isEmpty()) {
                ItemStack two = stack.split(1);
                if (!moveItemStackTo(two, 1, 2, false) && !moveItemStackTo(two, 0, 1, false)) stack.grow(two.getCount());
            }
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
