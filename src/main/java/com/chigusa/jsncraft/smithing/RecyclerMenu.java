package com.chigusa.jsncraft.smithing;

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

/** 分解機の画面。data: 0 進み具合 / 1 動いているか / 2 見ている人が鍛冶師か / 3 戻る割合(%) / 4 止まった理由 */
public class RecyclerMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    @Nullable private final RecyclerBlockEntity be;

    public RecyclerMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof RecyclerBlockEntity b ? b : null, true);
    }

    public RecyclerMenu(int id, Inventory inv, RecyclerBlockEntity be) {
        this(id, inv, be, false);
    }

    private RecyclerMenu(int id, Inventory inv, @Nullable RecyclerBlockEntity be, boolean client) {
        super(ModSmithing.RECYCLER_MENU.get(), id);
        this.be = be;
        this.container = be != null && !client ? be.items() : new SimpleContainer(RecyclerBlockEntity.IN + RecyclerBlockEntity.OUT);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(5) : new ContainerData() {
            @Override
            public int get(int i) {
                int lv = player instanceof ServerPlayer sp ? SmithingService.smithLevel(sp) : -1;
                return switch (i) {
                    case 0 -> be.progress();
                    case 1 -> be.running() ? 1 : 0;
                    case 2 -> lv >= 0 ? 1 : 0;
                    case 3 -> (int) Math.round(RecyclerService.rate(be.running() ? be.smithLevel : lv) * 100);
                    case 4 -> be.stopReason;
                    default -> 0;
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
        for (int r = 0; r < 2; r++) for (int c = 0; c < 3; c++) addSlot(new Slot(container, c + r * 3, 8 + c * 18, 22 + r * 18));
        for (int r = 0; r < 2; r++) for (int c = 0; c < 3; c++) addSlot(new Slot(container, RecyclerBlockEntity.IN + c + r * 3, 116 + c * 18, 22 + r * 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int progress() { return data.get(0); }
    public boolean running() { return data.get(1) != 0; }
    public boolean viewerIsSmith() { return data.get(2) != 0; }
    public int ratePercent() { return data.get(3); }
    public int stopReason() { return data.get(4); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp) || id != 0) return false;
        if (be.running()) {
            be.stop(0);
            return true;
        }
        if (SmithingService.smithLevel(sp) < 0 || !be.hasInput()) return false;
        be.start(sp);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = RecyclerBlockEntity.IN + RecyclerBlockEntity.OUT;
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, RecyclerBlockEntity.IN, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return be == null || (!be.isRemoved() && player.distanceToSqr(be.getBlockPos().getCenter()) < 64);
    }
}
