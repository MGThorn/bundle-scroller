package io.github.mgthorn.bundlescroller.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import fi.dy.masa.malilib.event.InitializationHandler;
import io.github.mgthorn.bundlescroller.client.pick.BundlePicker;

public class BundleScrollerClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());

		// Auto restock from bundles
		ClientTickEvents.END_CLIENT_TICK.register(BundlePicker::onClientTick);
	}
}
