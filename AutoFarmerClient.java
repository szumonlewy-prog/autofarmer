package com.example.autofarmer.client;

import com.example.autofarmer.ConfigPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AutoFarmerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ConfigPayload.ID,
                (payload, context) -> context.client().setScreen(new FarmerScreen(payload)));
    }
}
