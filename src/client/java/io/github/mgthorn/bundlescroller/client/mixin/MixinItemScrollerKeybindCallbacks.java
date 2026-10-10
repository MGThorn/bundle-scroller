package io.github.mgthorn.bundlescroller.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.event.KeybindCallbacks;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import io.github.mgthorn.bundlescroller.client.compat.ItemScrollerCrafting;

/**
 * Item Scroller mass craft with items from bundles. Runs every tick while mass craft is active,
 * before Item Scroller crafts, so the recipe book and swaps-only modes find the ingredients in the inventory too.
 */
@Mixin(value = KeybindCallbacks.class, remap = false)
public abstract class MixinItemScrollerKeybindCallbacks {
	@Inject(method = "onClientTickMassCraftImpl", at = @At("HEAD"))
	private void bundleScroller$takeIngredientsFromBundles(Minecraft mc, CallbackInfo ci) {
		// The same conditions Item Scroller checks before mass crafting
		if (!(mc.screen instanceof AbstractContainerScreen<?> gui)
				|| mc.screen instanceof CreativeModeInventoryScreen
				|| Configs.GUI_BLACKLIST.contains(gui.getClass().getName())
				|| !(Hotkeys.MASS_CRAFT.getKeybind().isKeybindHeld() || Configs.Generic.MASS_CRAFT_HOLD.getBooleanValue())) {
			return;
		}

		ItemScrollerCrafting.topUpIngredients(gui, RecipeStorage.getInstance().getSelectedRecipe(),
				CraftingHandler.getFirstCraftingOutputSlotForGui(gui), true);
	}
}
