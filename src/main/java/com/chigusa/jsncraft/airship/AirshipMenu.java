package com.chigusa.jsncraft.airship;

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
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/** 飛空艇の画面：燃料の枠・荷物・仕舞うボタン。data: 0 燃料 / 1 持ち主か */
public class AirshipMenu extends AbstractContainerMenu {
    @Nullable private final Airship ship;
    private final Container container;
    private final ContainerData data;
    private final int rows;

    public AirshipMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null, buf.readVarInt(), buf.readVarInt());
    }

    public AirshipMenu(int id, Inventory inv, Airship ship) {
        this(id, inv, ship, ship.getId(), ship.tier());
    }

    private AirshipMenu(int id, Inventory inv, @Nullable Airship ship, int entityId, int tier) {
        super(ModAirships.MENU.get(), id);
        this.ship = ship;
        this.rows = Math.max(1, Math.min(3, tier));
        this.container = ship != null ? ship.items() : new SimpleContainer(28);
        Player player = inv.player;
        this.data = ship == null ? new SimpleContainerData(2) : new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> ship.fuel();
                    case 1 -> ship.isOwner(player) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        addSlot(new Slot(container, 0, 8, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getBurnTime(RecipeType.SMELTING) > 0;
            }
        });
        for (int r = 0; r < rows; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(container, 1 + c + r * 9, 8 + c * 18, 44 + r * 18));
        int py = 44 + rows * 18 + 14;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, py + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, py + 58));
        addDataSlots(data);
    }

    public int rows() { return rows; }
    /** 燃料（ContainerData は 16bit で送られるので、秒にして返す） */
    public int fuelSeconds() { return (data.get(0) & 0xFFFF) / 20; }
    public boolean owner() { return data.get(1) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && ship != null && player instanceof ServerPlayer sp && ship.isAlive()) {
            sp.closeContainer();
            return ship.packUp(sp);
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int mine = 1 + rows * 9;
        if (index < mine) {
            if (!moveItemStackTo(stack, mine, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean fuel = stack.getBurnTime(RecipeType.SMELTING) > 0;
            if (!(fuel && moveItemStackTo(stack, 0, 1, false)) && !moveItemStackTo(stack, 1, mine, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return ship == null || (ship.isAlive() && player.distanceToSqr(ship) < 100);
    }
}
