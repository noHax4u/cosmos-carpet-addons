package net.cosmosmc.carpetaddons;

import carpet.CarpetExtension;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CosmosCarpetAddons implements ModInitializer, CarpetExtension {
	public static final String MOD_ID = "cosmos-carpet-addons";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Hello Fabric world!");
		CosmosCarpetAddonsServer.init();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
