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

/**
 * 鍛冶台の画面の中身。
 * data: 0 できること（1 強化 / 2 鍛え直し1 / 4 鍛え直し2 / 8 修理）/ 1 成功率（‰）/ 2 強化の上限 / 3 特性の枠の数 / 4 状態
 */
public class ForgeMenu extends AbstractContainerMenu {
    public static final int CAN_ENHANCE = 1, CAN_REFORGE1 = 2, CAN_REFORGE2 = 4, CAN_REPAIR = 8;
    public static final int ST_EMPTY = 0, ST_NOT_SMITH = 1, ST_CANT = 2, ST_OK = 3, ST_MAX = 4;

    private final Container container;
    private final ContainerData data;
    @Nullable private final ForgeBlockEntity be;

    public ForgeMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof ForgeBlockEntity b ? b : null, true);
    }

    public ForgeMenu(int id, Inventory inv, ForgeBlockEntity be) {
        this(id, inv, be, false);
    }

    private ForgeMenu(int id, Inventory inv, @Nullable ForgeBlockEntity be, boolean client) {
        super(ModSmithing.FORGE_MENU.get(), id);
        this.be = be;
        this.container = be != null ? be.items() : new SimpleContainer(2);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(5) : new ContainerData() {
            @Override
            public int get(int i) {
                if (!(player instanceof ServerPlayer sp)) return 0;
                ItemStack item = be.item(), mat = be.material();
                String kinds = SmithData.kinds(item);
                int lv = SmithingService.smithLevel(sp);
                int plus = SmithData.plus(item);
                int max = SmithingService.maxPlus(sp);
                return switch (i) {
                    case 0 -> {
                        if (lv < 0 || item.isEmpty()) yield 0;
                        int f = 0;
                        if (!kinds.isEmpty() && plus < max && ModSmithing.stoneTier(mat) >= SmithingService.tierFor(plus + 1)) f |= CAN_ENHANCE;
                        if (!kinds.isEmpty() && !SmithTraits.pool(mat.getItem(), kinds).isEmpty()) {
                            f |= CAN_REFORGE1;
                            if (SmithingService.traitSlots(sp) >= 2) f |= CAN_REFORGE2;
                        }
                        if (item.isDamaged() && !mat.isEmpty() && item.getItem().isValidRepairItem(item, mat)) f |= CAN_REPAIR;
                        yield f;
                    }
                    case 1 -> kinds.isEmpty() || plus >= 15 ? 0 : (int) Math.round(SmithingService.successRate(sp, plus + 1) * 1000);
                    case 2 -> max;
                    case 3 -> SmithingService.traitSlots(sp);
                    case 4 -> item.isEmpty() ? ST_EMPTY : lv < 0 ? ST_NOT_SMITH : kinds.isEmpty() ? ST_CANT : plus >= max ? ST_MAX : ST_OK;
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
        addSlot(new Slot(container, 0, 26, 26));
        addSlot(new Slot(container, 1, 62, 26));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int flags() { return data.get(0); }
    public int ratePermille() { return data.get(1); }
    public int maxPlus() { return data.get(2); }
    public int traitSlots() { return data.get(3); }
    public int status() { return data.get(4); }

    public ItemStack item() {
        return container.getItem(0);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;
        return switch (id) {
            case 0 -> SmithingService.enhance(sp, be);
            case 1 -> SmithingService.reforge(sp, be, 0);
            case 2 -> SmithingService.reforge(sp, be, 1);
            case 3 -> SmithingService.repair(sp, be);
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            boolean gear = !SmithData.kinds(stack).isEmpty() || stack.isDamageableItem();
            if (gear ? !moveItemStackTo(stack, 0, 1, false) : !moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
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
