package com.chigusa.jsncraft.cooking;

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

/** 酒樽の画面の中身。 */
public class BrewingMenu extends AbstractContainerMenu {
    /** data: 0 進み具合（0〜1000）/ 1 今の★ / 2 レシピの番号 / 3 状態 */
    public static final int ST_NONE = 0, ST_READY = 1, ST_LEVEL = 2, ST_NOT_CHEF = 3, ST_BREWING = 4, ST_FULL = 5, ST_DONE = 6;

    private final Container container;
    private final ContainerData data;
    @Nullable private final BrewingBarrelBlockEntity be;

    public BrewingMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof BrewingBarrelBlockEntity b ? b : null, true);
    }

    public BrewingMenu(int id, Inventory inv, BrewingBarrelBlockEntity be) {
        this(id, inv, be, false);
    }

    private BrewingMenu(int id, Inventory inv, @Nullable BrewingBarrelBlockEntity be, boolean client) {
        super(ModCooking.BREWING_MENU.get(), id);
        this.be = be;
        this.container = be != null ? be.items() : new SimpleContainer(BrewingBarrelBlockEntity.SIZE);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(4) : new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> be.permille();
                    case 1 -> be.currentQuality();
                    case 2 -> BrewingService.recipeIndex(be);
                    case 3 -> player instanceof ServerPlayer sp ? BrewingService.status(sp, be) : ST_NONE;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
        int[][] ing = {{26, 17}, {44, 17}, {26, 35}, {44, 35}};
        for (int i = 0; i < 4; i++) addSlot(new Slot(container, i, ing[i][0], ing[i][1]));
        addSlot(new Slot(container, BrewingBarrelBlockEntity.OUTPUT, 134, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(data);
    }

    public int permille() { return data.get(0); }
    public int quality() { return data.get(1); }
    public int recipeIndex() { return data.get(2); }
    public int status() { return data.get(3); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;
        if (id == 0) return BrewingService.start(sp, be);
        if (id == 1) return BrewingService.bottle(sp, be);
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = BrewingBarrelBlockEntity.SIZE;
        if (index < own) {
            if (!moveItemStackTo(stack, own, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, BrewingBarrelBlockEntity.INGREDIENTS, false)) {
            return ItemStack.EMPTY;
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
