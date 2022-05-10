package me.ramidzkh.compacting;

import java.util.List;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import appeng.api.upgrades.Upgrades;
import appeng.core.CreativeTab;
import appeng.core.definitions.AEItems;
import appeng.core.localization.GuiText;

public class CompactingDrives {

    public static final String MOD_ID = "compacting_drives";

    public static final Item COMPACTING_CARD = Registry.register(Registry.ITEM, id("compacting_card"),
            Upgrades.createUpgradeCardItem(new Item.Properties().tab(CreativeTab.INSTANCE)));

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static void initialize() {
        var group = GuiText.StorageCells.getTranslationKey();

        for (var cell : List.of(AEItems.ITEM_CELL_1K, AEItems.ITEM_CELL_4K, AEItems.ITEM_CELL_16K,
                AEItems.ITEM_CELL_64K)) {
            Upgrades.add(COMPACTING_CARD, cell, 1, group);
        }
    }
}
