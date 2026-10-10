package io.github.mgthorn.bundlescroller.client.compat;

import java.util.function.Predicate;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

/**
 * Auto eat from bundle, for Garitone Mineman's easy eat / auto eat.
 * When easy eat finds no food in the inventory, food is taken out of a bundle into a free slot.
 * Only vanilla actions are used (bundle item selection and inventory clicks), so no server-side mod is needed.
 */
public class MinemanEasyEat {
	public static final String MOD_ID = "mineman";

	private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);

	private static final int OFFHAND_INDEX = Inventory.SLOT_OFFHAND;
	private static final int BUTTON_LEFT = 0;
	private static final int BUTTON_RIGHT = 1;

	public static boolean isLoaded() {
		return LOADED;
	}

	/**
	 * Takes a food stack out of a bundle into a free inventory slot.
	 * The slot easy eat eats from is only used when no other slot is free.
	 *
	 * @return the inventory index (0-35) of the food, or -1 if nothing was taken out
	 */
	public static int takeFoodFromBundle(Minecraft mc, int targetHotbarSlot, Predicate<ItemStack> isFood) {
		LocalPlayer player = mc.player;

		if (player == null || mc.gameMode == null || mc.getConnection() == null) {
			return -1;
		}

		// The clicks use the player inventory's slot numbers, so no other container may be open,
		// and the cursor has to be empty to take an item out of a bundle
		AbstractContainerMenu menu = player.containerMenu;

		if (menu != player.inventoryMenu || !menu.getCarried().isEmpty()) {
			return -1;
		}

		Inventory inventory = player.getInventory();
		int freeIndex = findFreeIndex(inventory, targetHotbarSlot);

		if (freeIndex == -1) {
			return -1;
		}

		for (int bundleIndex : bundleCandidateIndices()) {
			ItemStack bundle = inventory.getItem(bundleIndex);
			BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);

			if (contents == null) {
				continue;
			}

			for (int itemIndex = 0; itemIndex < contents.size(); itemIndex++) {
				if (isFood.test(contents.getItemUnsafe(itemIndex))) {
					return takeOut(mc, player, menu, bundleIndex, itemIndex, freeIndex, isFood);
				}
			}
		}

		return -1;
	}

	private static int takeOut(Minecraft mc, LocalPlayer player, AbstractContainerMenu menu,
							   int bundleIndex, int itemIndex, int freeIndex, Predicate<ItemStack> isFood) {
		ItemStack bundle = player.getInventory().getItem(bundleIndex);
		int bundleSlot = toContainerSlot(bundleIndex);

		// A bundle only gives out its selected item (the first one if none is selected).
		// Selecting the index that is already selected would clear the selection, so only select when it changes.
		int size = bundle.get(DataComponents.BUNDLE_CONTENTS).size();
		int selected = BundleItem.getSelectedItem(bundle);
		int effectiveSelected = selected < 0 || selected >= size ? 0 : selected;

		if (effectiveSelected != itemIndex) {
			BundleItem.toggleSelectedItem(bundle, itemIndex);
			mc.getConnection().send(new ServerboundSelectBundleItemPacket(bundleSlot, itemIndex));
		}

		// Right-click the bundle with an empty cursor: the selected stack goes onto the cursor
		mc.gameMode.handleInventoryMouseClick(menu.containerId, bundleSlot, BUTTON_RIGHT, ClickType.PICKUP, player);

		if (!isFood.test(menu.getCarried())) {
			// Something else came out: put it back into the bundle
			if (!menu.getCarried().isEmpty()) {
				mc.gameMode.handleInventoryMouseClick(menu.containerId, bundleSlot, BUTTON_LEFT, ClickType.PICKUP, player);
			}
			return -1;
		}

		// Left-click the free slot: the whole food stack goes there
		mc.gameMode.handleInventoryMouseClick(menu.containerId, toContainerSlot(freeIndex), BUTTON_LEFT, ClickType.PICKUP, player);

		return isFood.test(player.getInventory().getItem(freeIndex)) ? freeIndex : -1;
	}

	// Main inventory first so the hotbar stays as it is, then the hotbar, and the target slot last
	private static int findFreeIndex(Inventory inventory, int targetHotbarSlot) {
		for (int i = 9; i <= 35; i++) {
			if (inventory.getItem(i).isEmpty()) return i;
		}

		for (int i = 0; i <= 8; i++) {
			if (i != targetHotbarSlot && inventory.getItem(i).isEmpty()) return i;
		}

		if (targetHotbarSlot >= 0 && targetHotbarSlot <= 8 && inventory.getItem(targetHotbarSlot).isEmpty()) {
			return targetHotbarSlot;
		}

		return -1;
	}

	private static int[] bundleCandidateIndices() {
		int[] indices = new int[37];

		for (int i = 0; i <= 35; i++) {
			indices[i] = i;
		}

		indices[36] = OFFHAND_INDEX;
		return indices;
	}

	// Inventory index → slot number in InventoryMenu: hotbar 0-8 → 36-44, main 9-35 → 9-35, offhand 40 → 45
	private static int toContainerSlot(int inventoryIndex) {
		if (inventoryIndex == OFFHAND_INDEX) return 45;
		return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
	}
}
