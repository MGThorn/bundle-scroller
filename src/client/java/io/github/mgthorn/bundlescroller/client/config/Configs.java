package io.github.mgthorn.bundlescroller.client.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import io.github.mgthorn.bundlescroller.Reference;

public class Configs {
	public static class Generic {
		private static final String GENERIC_KEY = Reference.MOD_ID + ".config.generic";

		// Hold a mouse button with a bundle on the cursor and drag over slots to fill or empty it
		public static final ConfigBoolean BUNDLE_DRAG = new ConfigBoolean("bundleDrag", true).apply(GENERIC_KEY);

		public static final List<IConfigBase> OPTIONS = ImmutableList.of(
				BUNDLE_DRAG
		);
	}
}
