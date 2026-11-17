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

/**
 * 魔紋の祭壇の画面の中身。
 * ボタン：0 刻む / 1 上限突破 / 2 付け替え / 3 前のエンチャント / 4 次のエンチャント / 100+n 魔紋 n を選ぶ / 200+n エンチャント n を選ぶ
 * data：0 選んだ魔紋 / 1 選んだエンチャント / 2 付与の腕のレベル / 3 魔紋の枠 / 4 上限突破の上限 /
 *       5〜9 刻む（状態・ラピス・経験値・触媒・レベル）/ 10〜14 上限突破 / 15〜19 付け替え
 */
public class RuneAltarMenu extends AbstractContainerMenu {
    public static final int BTN_INSCRIBE = 0, BTN_LIMIT = 1, BTN_TRANSFER = 2, BTN_PREV = 3, BTN_NEXT = 4, BTN_RUNE = 100, BTN_ENCH = 200;
    private static final int COUNT = 20;

    private final Container container;
    private final ContainerData data;
    @Nullable private final RuneAltarBlockEntity be;
    private int selRune = -1, selEnch = 0;

    public RuneAltarMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof RuneAltarBlockEntity b ? b : null, true);
    }

    public RuneAltarMenu(int id, Inventory inv, RuneAltarBlockEntity be) {
        this(id, inv, be, false);
    }

    private RuneAltarMenu(int id, Inventory inv, @Nullable RuneAltarBlockEntity be, boolean client) {
        super(ModEnchanting.ALTAR_MENU.get(), id);
        this.be = be;
        this.container = be != null ? be.items() : new SimpleContainer(3);
        Player player = inv.player;
        this.data = client || be == null ? new SimpleContainerData(COUNT) : new ContainerData() {
            @Override
            public int get(int i) {
                if (!(player instanceof ServerPlayer sp)) return 0;
                if (i == 0) return selRune;
                if (i == 1) return selEnch;
                if (i == 2) return EnchantingService.enchantLevel(sp);
                if (i == 3) return EnchantingService.runeSlots(sp);
                if (i == 4) return EnchantingService.limitBonus(sp);
                EnchantingService.Check c = i < 10 ? EnchantingService.checkRune(sp, be, Runes.get(selRune))
                        : i < 15 ? EnchantingService.checkLimit(sp, be, selEnch)
                        : EnchantingService.checkTransfer(sp, be, selEnch);
                return switch ((i - 5) % 5) {
                    case 0 -> c.status();
                    case 1 -> c.lapis();
                    case 2 -> c.xp();
                    case 3 -> c.catalyst();
                    default -> c.target();
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
        int ox = 8;
        addSlot(new Slot(container, 0, ox + 8, 22));
        addSlot(new Slot(container, 1, ox + 8, 46) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.LAPIS_LAZULI);
            }
        });
        addSlot(new Slot(container, 2, ox + 44, 34));
        int invX = 48, invY = 146;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, invX + c * 18, invY + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, invX + c * 18, invY + 58));
        addDataSlots(data);
    }

    public int selectedRune() { return data.get(0); }
    public int selectedEnch() { return data.get(1); }
    public int enchantLevel() { return data.get(2); }
    public int runeSlots() { return data.get(3); }
    public int limitBonus() { return data.get(4); }

    /** 0 刻む / 1 上限突破 / 2 付け替え の、n 番目の値（0 状態 1 ラピス 2 経験値 3 触媒 4 レベル） */
    public int check(int mode, int n) {
        return data.get(5 + mode * 5 + n);
    }

    public ItemStack item() { return container.getItem(0); }
    public ItemStack catalyst() { return container.getItem(2); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (be == null || !(player instanceof ServerPlayer sp)) return false;
        if (id >= BTN_ENCH) {
            selEnch = id - BTN_ENCH;
            return true;
        }
        if (id >= BTN_RUNE) {
            if (Runes.get(id - BTN_RUNE) == null) return false;
            selRune = id - BTN_RUNE;
            return true;
        }
        return switch (id) {
            case BTN_INSCRIBE -> EnchantingService.inscribe(sp, be, Runes.get(selRune));
            case BTN_LIMIT -> EnchantingService.limitBreak(sp, be, selEnch);
            case BTN_TRANSFER -> {
                boolean ok = EnchantingService.transfer(sp, be, selEnch);
                if (ok) selEnch = 0;
                yield ok;
            }
            case BTN_PREV -> {
                int n = Math.max(1, RuneData.enchantments(be.item()).size());
                selEnch = Math.floorMod(selEnch - 1, n);
                yield true;
            }
            case BTN_NEXT -> {
                int n = Math.max(1, RuneData.enchantments(be.item()).size());
                selEnch = Math.floorMod(selEnch + 1, n);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.is(Items.LAPIS_LAZULI)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else {
            boolean gear = !RuneData.kinds(stack).isEmpty() || (stack.isEnchanted() && !stack.is(Items.ENCHANTED_BOOK));
            if (gear && !container.getItem(0).isEmpty()) gear = false;
            if (gear ? !moveItemStackTo(stack, 0, 1, false) : !moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
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
