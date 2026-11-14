package com.example.autofarmer;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.List;

/** Lista obsługiwanych "nasion" i powiązania z blokami roślin. */
public final class FarmerSeeds {
    private FarmerSeeds() {}

    public static final List<Item> SEEDS = List.of(
            Items.WHEAT_SEEDS,
            Items.BEETROOT_SEEDS,
            Items.CARROT,
            Items.POTATO,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS
    );

    public static boolean isAllowed(Item item) {
        return SEEDS.contains(item);
    }

    /** Blok rośliny sadzony z danego nasiona. */
    public static Block cropBlock(Item seed) {
        if (seed == Items.WHEAT_SEEDS) return Blocks.WHEAT;
        if (seed == Items.BEETROOT_SEEDS) return Blocks.BEETROOTS;
        if (seed == Items.CARROT) return Blocks.CARROTS;
        if (seed == Items.POTATO) return Blocks.POTATOES;
        if (seed == Items.MELON_SEEDS) return Blocks.MELON_STEM;
        if (seed == Items.PUMPKIN_SEEDS) return Blocks.PUMPKIN_STEM;
        return null;
    }

    /** Nasiono, które "zjada" ponowne zasadzenie po zbiorze dojrzałej rośliny. */
    public static Item seedForCrop(Block crop) {
        if (crop == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (crop == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (crop == Blocks.CARROTS) return Items.CARROT;
        if (crop == Blocks.POTATOES) return Items.POTATO;
        return null;
    }

    public static boolean isStem(Item seed) {
        return seed == Items.MELON_SEEDS || seed == Items.PUMPKIN_SEEDS;
    }

    /** Owoc (blok), który zbieramy dla danego nasiona łodygowego. */
    public static Block fruitFor(Item seed) {
        if (seed == Items.MELON_SEEDS) return Blocks.MELON;
        if (seed == Items.PUMPKIN_SEEDS) return Blocks.PUMPKIN;
        return null;
    }
}
