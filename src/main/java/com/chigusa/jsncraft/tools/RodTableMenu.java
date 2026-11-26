package com.chigusa.jsncraft.tools;

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
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** 釣り竿カスタム台の画面。data: 0 見ている人の釣り師レベル（-1 なら釣り師でない） */
public class RodTableMenu extends AbstractContainerMenu {
    @Nullable private final RodTableBlockEntity be;
    private final Container container;
    private final ContainerData data;

    public RodTableMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof RodTableBlockEntity b ? b : null, true);
    }

    public RodTableMenu(int id, Inventory inv, RodTableBlockEntity be) {
        this(id, inv, be, false);
    }

    private RodTableMenu(int id, Inventory inv, @Nullable RodTableBlockEntity be, boolean client) {
        super(ModTools.ROD_TABLE_MENU.get(), id);
        this.be = be;
        this.container = be != null && !client ? be.items() : new SimpleContainer(6);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(1) : new ContainerData() {
            @Override
            public int get(int i) {
                return player instanceof ServerPlayer sp ? JobLevels.angler(sp) : -1;
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        addSlot(new Slot(container, 0, 26, 34) {
            @Override
            public boolean mayPlace(ItemStack s) { return s.getItem() instanceof FishingRodItem; }

            @Override
            public int getMaxStackSize() { return 1; }
        });
        for (RodPart part : RodPart.values()) {
            int i = part.ordinal();
            addSlot(new Slot(container, 1 + i, 70 + i * 18, 34) {
                @Override
                public boolean mayPlace(ItemStack s) { return s.getItem() instanceof RodPartItem pi && pi.part == part; }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int anglerLevel() {
        int v = data.get(0);
        return v > 32767 ? v - 65536 : v;
    }

    public ItemStack rod() { return slots.get(0).getItem(); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;
        return id == 0 ? be.attach(sp) : id == 1 && be.detach(sp);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 6) {
            if (!moveItemStackTo(stack, 6, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof FishingRodItem) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof RodPartItem pi) {
            if (!moveItemStackTo(stack, 1 + pi.part.ordinal(), 2 + pi.part.ordinal(), false)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(be.getBlockPos().getCenter()) < 64);
    }
}
