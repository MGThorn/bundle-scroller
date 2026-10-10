package io.github.mgthorn.bundlescroller.client.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.mgthorn.bundlescroller.Reference;
import io.github.mgthorn.bundlescroller.client.compat.MinemanEasyEat;

public class Configs {
	public static class Generic {
		private static final String GENERIC_KEY = Reference.MOD_ID + ".config.generic";

		// Hold a mouse button with a bundle on the cursor and drag over slots to fill or empty it
		public static final ConfigBoolean BUNDLE_DRAG = new ConfigBoolean("bundleDrag", true).apply(GENERIC_KEY);

		// Garitone Mineman's easy eat takes food out of a bundle when there is none in the inventory.
		// Without Mineman it does nothing, and the hover text shows a red warning.
		public static final ConfigBoolean AUTO_EAT_FROM_BUNDLE = new ConfigBoolean("autoEatFromBundle", true) {
			@Override
			public String getComment() {
				if (MinemanEasyEat.isLoaded()) {
					return super.getComment();
				}

				return super.getComment() + "\n" + GuiBase.TXT_RED + StringUtils.translate(Reference.MOD_ID + ".config.warning.mineman_missing") + GuiBase.TXT_RST;
			}
		}.apply(GENERIC_KEY);

		// Item Scroller's crafting features take missing ingredients out of bundles
		public static final ConfigBoolean BUNDLE_CRAFTING = new ConfigBoolean("bundleCrafting", true).apply(GENERIC_KEY);

		// When the held stack runs out, the same item is taken out of a bundle into the hand
		public static final ConfigBoolean AUTO_RESTOCK_FROM_BUNDLE = new ConfigBoolean("autoRestockFromBundle", false).apply(GENERIC_KEY);
		// Whether dropping the last item with the drop key also triggers auto restock
		public static final ConfigBoolean AUTO_RESTOCK_AFTER_DROP = new ConfigBoolean("autoRestockAfterDrop", false).apply(GENERIC_KEY);

		public static final List<IConfigBase> OPTIONS = ImmutableList.of(
				BUNDLE_DRAG,
				AUTO_EAT_FROM_BUNDLE,
				BUNDLE_CRAFTING,
				AUTO_RESTOCK_FROM_BUNDLE,
				AUTO_RESTOCK_AFTER_DROP
		);
	}
}
