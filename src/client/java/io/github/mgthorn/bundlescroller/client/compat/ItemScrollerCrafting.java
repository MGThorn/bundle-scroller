package io.github.mgthorn.bundlescroller.client.compat;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.RecipePattern;
import fi.dy.masa.itemscroller.util.ClickPacketBuffer;
import io.github.mgthorn.bundlescroller.client.config.Configs;
import io.github.mgthorn.bundlescroller.client.util.BundleUtils;

/**
 * Item Scroller crafting with items from bundles: before Item Scroller fills the crafting grid from the inventory,
 * missing ingredients are taken out of bundles into free inventory slots.
 */
public class ItemScrollerCrafting {
	/**
	 * Makes sure the player inventory (plus the crafting grid) holds enough of every ingredient of the recipe:
	 * enough for one craft, or for a full stack in every grid slot when {@code fillStacks} is set.
	 */
	public static void topUpIngredients(AbstractContainerScreen<?> gui, @Nullable RecipePattern recipe,
										@Nullable Slot outputSlot, boolean fillStacks) {
		Minecraft mc = Minecraft.getInstance();

		if (!Configs.Generic.BUNDLE_CRAFTING.getBooleanValue() || recipe == null || outputSlot == null
				|| recipe.getResult().isEmpty() || mc.player == null) {
			return;
		}

		// While Item Scroller replays buffered click packets, it cancels every other click
		if (ClickPacketBuffer.shouldCancelWindowClicks()) {
			return;
		}

		AbstractContainerMenu menu = gui.getMenu();
		CraftingHandler.SlotRange range = CraftingHandler.getCraftingGridSlots(gui, outputSlot);

		if (range == null || !BundleUtils.canClick(mc, menu)) {
			return;
		}

		Inventory inventory = mc.player.getInventory();
		List<Slot> inventorySlots = BundleUtils.getPlayerInventorySlots(menu, inventory);
		List<Slot> bundleSlots = BundleUtils.getBundleCandidateSlots(menu, inventory);
		// Keep one slot free for the crafting results when filling whole stacks
		int reservedFreeSlots = fillStacks ? 1 : 0;

		for (Ingredient ingredient : getIngredients(recipe)) {
			int wanted = ingredient.perCraft * (fillStacks ? ingredient.stack.getMaxStackSize() : 1);
			int available = countMatching(inventorySlots, ingredient.stack) + countInGrid(menu, range, ingredient.stack);

			while (available < wanted) {
				Slot target = findFreeSlot(inventorySlots, reservedFreeSlots);
				BundleHit hit = target != null ? findInBundles(bundleSlots, ingredient.stack) : null;

				if (hit == null || !BundleUtils.takeStackOut(mc, menu, hit.bundleSlot, hit.itemIndex, target)) {
					break;
				}

				available += target.getItem().getCount();
			}
		}
	}

	private static List<Ingredient> getIngredients(RecipePattern recipe) {
		List<Ingredient> ingredients = new ArrayList<>();

		outer:
		for (ItemStack stack : recipe.getRecipeItems()) {
			if (stack.isEmpty()) {
				continue;
			}

			for (Ingredient ingredient : ingredients) {
				if (ItemStack.isSameItemSameComponents(ingredient.stack, stack)) {
					ingredient.perCraft++;
					continue outer;
				}
			}

			ingredients.add(new Ingredient(stack));
		}

		return ingredients;
	}

	private static int countMatching(List<Slot> slots, ItemStack stack) {
		int count = 0;

		for (Slot slot : slots) {
			if (ItemStack.isSameItemSameComponents(slot.getItem(), stack)) {
				count += slot.getItem().getCount();
			}
		}

		return count;
	}

	private static int countInGrid(AbstractContainerMenu menu, CraftingHandler.SlotRange range, ItemStack stack) {
		int count = 0;

		for (int i = range.getFirst(); i <= range.getLast() && i < menu.slots.size(); i++) {
			ItemStack gridStack = menu.slots.get(i).getItem();

			if (ItemStack.isSameItemSameComponents(gridStack, stack)) {
				count += gridStack.getCount();
			}
		}

		return count;
	}

	// Main inventory first so the hotbar stays as it is, then the hotbar
	@Nullable
	private static Slot findFreeSlot(List<Slot> inventorySlots, int reserved) {
		List<Slot> free = new ArrayList<>();

		for (Slot slot : inventorySlots) {
			if (!slot.hasItem() && slot.getContainerSlot() >= 9) free.add(slot);
		}

		for (Slot slot : inventorySlots) {
			if (!slot.hasItem() && slot.getContainerSlot() < 9) free.add(slot);
		}

		return free.size() > reserved ? free.get(0) : null;
	}

	@Nullable
	private static BundleHit findInBundles(List<Slot> bundleSlots, ItemStack stack) {
		for (Slot slot : bundleSlots) {
			BundleContents contents = slot.getItem().get(DataComponents.BUNDLE_CONTENTS);

			if (contents == null) {
				continue;
			}

			for (int i = 0; i < contents.size(); i++) {
				if (ItemStack.isSameItemSameComponents(contents.getItemUnsafe(i), stack)) {
					return new BundleHit(slot, i);
				}
			}
		}

		return null;
	}

	private static class Ingredient {
		private final ItemStack stack;
		private int perCraft = 1;

		private Ingredient(ItemStack stack) {
			this.stack = stack;
		}
	}

	private record BundleHit(Slot bundleSlot, int itemIndex) {}
}
