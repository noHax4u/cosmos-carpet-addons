package net.cosmosmc.carpetaddons.client;

import net.cosmosmc.carpetaddons.lightmanipulator.LightManipulatorState;
import net.cosmosmc.carpetaddons.lightmanipulator.network.ManipulatorStatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class CosmosCarpetAddonsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(ManipulatorStatePayload.TYPE,
				(payload, context) -> LightManipulatorState.clientActive = payload.active());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> LightManipulatorState.clientActive = false);
	}
}
