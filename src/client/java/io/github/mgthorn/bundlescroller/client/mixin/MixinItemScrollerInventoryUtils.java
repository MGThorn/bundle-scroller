package io.github.mgthorn.bundlescroller.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import fi.dy.masa.itemscroller.recipes.RecipePattern;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import io.github.mgthorn.bundlescroller.client.compat.ItemScrollerCrafting;

/**
 * Item Scroller crafting with items from bundles. {@code tryMoveItemsToCraftingGridSlots} fills the crafting grid
 * from the inventory for recipe clicks, scroll crafting, "craft everything" and the fallback mass craft mode.
 */
@Mixin(value = InventoryUtils.class, remap = false)
public abstract class MixinItemScrollerInventoryUtils {
	@Inject(method = "tryMoveItemsToCraftingGridSlots", at = @At("HEAD"))
	private static void bundleScroller$takeIngredientsFromBundles(RecipePattern recipe, Slot slot,
																  AbstractContainerScreen<? extends AbstractContainerMenu> gui,
																  boolean fillStacks, CallbackInfoReturnable<Boolean> cir) {
		ItemScrollerCrafting.topUpIngredients(gui, recipe, slot, fillStacks);
	}
}
