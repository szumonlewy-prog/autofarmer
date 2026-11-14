package com.example.autofarmer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class AutoFarmerMod implements ModInitializer {
    public static final String MOD_ID = "autofarmer";

    public static Block FARMER_BLOCK;
    public static Item FARMER_ITEM;
    public static BlockEntityType<FarmerBlockEntity> FARMER_BE;

    @Override
    public void onInitialize() {
        Identifier id = Identifier.of(MOD_ID, "farmer");

        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
        FARMER_BLOCK = Registry.register(Registries.BLOCK, blockKey,
                new FarmerBlock(AbstractBlock.Settings.create()
                        .registryKey(blockKey)
                        .strength(2.0f)
                        .sounds(BlockSoundGroup.WOOD)));

        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
        FARMER_ITEM = Registry.register(Registries.ITEM, itemKey,
                new BlockItem(FARMER_BLOCK, new Item.Settings()
                        .registryKey(itemKey)
                        .useBlockPrefixedTranslationKey()));

        FARMER_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id,
                FabricBlockEntityTypeBuilder.create(FarmerBlockEntity::new, FARMER_BLOCK).build());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> e.add(FARMER_ITEM));

        // Ten sam pakiet w obie strony
        PayloadTypeRegistry.playS2C().register(ConfigPayload.ID, ConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ConfigPayload.ID, ConfigPayload.CODEC);

        // Serwer odbiera zapis konfiguracji z GUI
        ServerPlayNetworking.registerGlobalReceiver(ConfigPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            BlockPos pos = payload.pos();
            if (player.squaredDistanceTo(Vec3d.ofCenter(pos)) > 100.0) return;
            if (player.getWorld().getBlockEntity(pos) instanceof FarmerBlockEntity be) {
                be.applyConfig(payload);
                player.sendMessage(Text.literal("Auto Farmer: zapisano ustawienia"), true);
            }
        });
    }
}
