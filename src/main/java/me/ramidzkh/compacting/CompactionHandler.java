package me.ramidzkh.compacting;

import java.util.function.Supplier;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.me.cells.BasicCellInventory;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

public class CompactionHandler {

    private static final Multimap<AEKey, Conversion> CONVERSIONS = HashMultimap.create();

    private final BasicCellInventory cell;
    private final Supplier<Object2LongMap<AEKey>> cellItems;

    public CompactionHandler(BasicCellInventory cell, Supplier<Object2LongMap<AEKey>> cellItems) {
        this.cell = cell;
        this.cellItems = cellItems;
    }

    public void getAvailableStacks(KeyCounter out) {
        for (var entry : cellItems.get().object2LongEntrySet()) {
            out.add(entry.getKey(), entry.getLongValue());

            for (var conversion : CONVERSIONS.get(entry.getKey())) {
                out.add(conversion.from, entry.getLongValue() / conversion.intoAmount * conversion.fromAmount);
            }
        }
    }

    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        var extracted = cell.extract(what, amount, mode, source);

        for (var conversion : CONVERSIONS.get(what)) {

            // Extract in steps
            while ((extracted + conversion.intoAmount) <= amount && cell.extract(conversion.from, conversion.fromAmount,
                    Actionable.SIMULATE, source) == conversion.fromAmount) {
                if (mode == Actionable.MODULATE) {
                    cell.extract(conversion.from, conversion.fromAmount, Actionable.MODULATE, source);
                }

                extracted += conversion.intoAmount;
            }
        }

        return extracted;
    }

    private record Conversion(long intoAmount, AEKey from, long fromAmount) {
    }

    private static void register(Item from, long fromAmount, Item to, long toAmount) {
        CONVERSIONS.put(AEItemKey.of(from), new Conversion(fromAmount, AEItemKey.of(to), toAmount));
        CONVERSIONS.put(AEItemKey.of(to), new Conversion(toAmount, AEItemKey.of(from), fromAmount));
    }

    static {
        register(Items.IRON_BLOCK, 1, Items.IRON_INGOT, 9);
        register(Items.IRON_INGOT, 1, Items.IRON_NUGGET, 9);
        // TODO: Implicit conversions
        register(Items.IRON_BLOCK, 1, Items.IRON_NUGGET, 9 * 9);

        register(Items.REDSTONE_BLOCK, 1, Items.REDSTONE, 9);
    }
}
