package com.chigusa.jsncraft.furnace;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
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

/** 職業の炉の画面。data: 0 進み / 1 動いているか / 2 燃料の残り / 3 燃料の最大 / 4 止まった理由 / 5 見ている人が動かせるか / 6 多めに取れる倍率(%) / 7 種類 */
public class MachineMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    @Nullable private final MachineBlockEntity be;

    public MachineMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof MachineBlockEntity b ? b : null, true);
    }

    public MachineMenu(int id, Inventory inv, MachineBlockEntity be) {
        this(id, inv, be, false);
    }

    private MachineMenu(int id, Inventory inv, @Nullable MachineBlockEntity be, boolean client) {
        super(ModFurnaces.MENU.get(), id);
        this.be = be;
        this.container = be != null && !client ? be.items() : new SimpleContainer(7);
        Player player = inv.player;
        MachineKind kind = be != null ? be.kind() : MachineKind.BIG_FURNACE;
        this.data = client || be == null ? new SimpleContainerData(8) : new ContainerData() {
            @Override
            public int get(int i) {
                int lv = player instanceof ServerPlayer sp ? kind.level(sp) : -1;
                return switch (i) {
                    case 0 -> be.progress;
                    case 1 -> be.running ? 1 : 0;
                    case 2 -> Math.min(32000, be.burn);
                    case 3 -> Math.min(32000, be.burnMax);
                    case 4 -> be.stopReason;
                    case 5 -> lv >= 0 ? 1 : 0;
                    case 6 -> (int) Math.round(kind.bonus(be.running ? be.jobLevel : lv) * 100);
                    case 7 -> kind.ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return 8;
            }
        };
        for (int i = 0; i < 3; i++) addSlot(new Slot(container, i, 26 + i * 18, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !(player.level() instanceof ServerLevel sl) || !MachineBlockEntity.result(sl, kind, stack).isEmpty();
            }
        });
        for (int i = 0; i < 3; i++) addSlot(new Slot(container, 3 + i, 116 + i * 18, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new Slot(container, MachineBlockEntity.FUEL, 44, 56) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getBurnTime(RecipeType.SMELTING) > 0;
            }

            @Override
            public boolean isActive() {
                return data.get(7) == MachineKind.BIG_FURNACE.ordinal();
            }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int progress() { return data.get(0); }
    public boolean running() { return data.get(1) != 0; }
    public int burn() { return data.get(2); }
    public int burnMax() { return data.get(3); }
    public int stopReason() { return data.get(4); }
    public boolean viewerCan() { return data.get(5) != 0; }
    public int bonusPercent() { return data.get(6); }
    public MachineKind kind() { return MachineKind.byId(data.get(7)); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp) || id != 0) return false;
        if (be.running()) {
            be.stop(0);
            return true;
        }
        if (be.kind().level(sp) < 0 || !be.hasInput()) return false;
        be.start(sp);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = 7;
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean fuel = kind() == MachineKind.BIG_FURNACE && stack.getBurnTime(RecipeType.SMELTING) > 0;
            if (!moveItemStackTo(stack, 0, 3, false) && !(fuel && moveItemStackTo(stack, 6, 7, false))) return ItemStack.EMPTY;
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
