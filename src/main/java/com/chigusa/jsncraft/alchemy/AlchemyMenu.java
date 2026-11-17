package com.chigusa.jsncraft.alchemy;

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

/**
 * 調合台の画面の中身。
 * ボタン：0 作る / 1 まとめて作る / 2 錬金する / 100+n 錬金のレシピ n を選ぶ
 * data：0 選んだ錬金 / 1 調合の腕 / 2 追加素材の枠 / 3 調合の状態 / 4 まとめて作れる数 / 5 薬の種類 /
 *       6 錬金の状態 / 7 錬金の経験値 / 8 賢者か / 9 香が作れるか / 10 薬の形
 */
public class AlchemyMenu extends AbstractContainerMenu {
    public static final int BTN_BREW = 0, BTN_BREW_ALL = 1, BTN_TRANSMUTE = 2, BTN_RECIPE = 100;
    private static final int COUNT = 11;

    private final Container container;
    private final ContainerData data;
    @Nullable private final AlchemyTableBlockEntity be;
    private int selected = -1;

    public AlchemyMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof AlchemyTableBlockEntity b ? b : null, true);
    }

    public AlchemyMenu(int id, Inventory inv, AlchemyTableBlockEntity be) {
        this(id, inv, be, false);
    }

    private AlchemyMenu(int id, Inventory inv, @Nullable AlchemyTableBlockEntity be, boolean client) {
        super(ModAlchemy.TABLE_MENU.get(), id);
        this.be = be;
        this.container = be != null ? be.items() : new SimpleContainer(5);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(COUNT) : new ContainerData() {
            @Override
            public int get(int i) {
                if (!(player instanceof ServerPlayer sp)) return 0;
                return switch (i) {
                    case 0 -> selected;
                    case 1 -> AlchemyService.alchemyLevel(sp);
                    case 2 -> AlchemyService.additiveSlots(sp);
                    case 3 -> AlchemyService.checkBrew(sp, be).status();
                    case 4 -> AlchemyService.checkBrew(sp, be).max();
                    case 5 -> {
                        var t = AlchemyService.checkBrew(sp, be).type();
                        yield t == null ? -1 : t.index();
                    }
                    case 6 -> AlchemyService.checkTransmute(sp, be, Transmutes.get(selected)).status();
                    case 7 -> AlchemyService.checkTransmute(sp, be, Transmutes.get(selected)).xp();
                    case 8 -> AlchemyService.isSage(sp) ? 1 : 0;
                    case 9 -> AlchemyService.canDust(sp) ? 1 : 0;
                    case 10 -> AlchemyService.checkBrew(sp, be).form();
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
        addSlot(new Slot(container, 0, 16, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.GLASS_BOTTLE);
            }
        });
        addSlot(new Slot(container, 1, 44, 24));
        for (int i = 0; i < 3; i++) addSlot(new Slot(container, 2 + i, 16 + i * 18, 52));
        int invX = 48, invY = 146;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, invX + c * 18, invY + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, invX + c * 18, invY + 58));
        addDataSlots(data);
    }

    public int selected() { return data.get(0); }
    public int alchemyLevel() { return data.get(1); }
    public int additiveSlots() { return data.get(2); }
    public int brewStatus() { return data.get(3); }
    public int brewMax() { return data.get(4); }
    public int brewType() { return data.get(5); }
    public int transStatus() { return data.get(6); }
    public int transXp() { return data.get(7); }
    public boolean sage() { return data.get(8) == 1; }
    public boolean canDust() { return data.get(9) == 1; }
    public int brewForm() { return data.get(10); }

    public ItemStack slotItem(int i) {
        return container.getItem(i);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;
        if (id >= BTN_RECIPE) {
            if (Transmutes.get(id - BTN_RECIPE) == null) return false;
            selected = id - BTN_RECIPE;
            return true;
        }
        return switch (id) {
            case BTN_BREW -> AlchemyService.brew(sp, be, false);
            case BTN_BREW_ALL -> AlchemyService.brew(sp, be, true);
            case BTN_TRANSMUTE -> AlchemyService.transmute(sp, be, Transmutes.get(selected));
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 5) {
            if (!moveItemStackTo(stack, 5, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.GLASS_BOTTLE)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (Elixirs.byMain(stack.getItem()) != null && (container.getItem(1).isEmpty() || container.getItem(1).is(stack.getItem()))) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 1, 5, false)) return ItemStack.EMPTY;
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
