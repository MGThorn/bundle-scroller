package io.github.mgthorn.bundlescroller.client.pick;

import java.util.List;
import org.apache.commons.lang3.math.Fraction;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import fi.dy.masa.malilib.util.InfoUtils;
import io.github.mgthorn.bundlescroller.Reference;
import io.github.mgthorn.bundlescroller.client.config.Configs;
import io.github.mgthorn.bundlescroller.client.util.BundleUtils;

/**
 * Autopicking from bundles:
 * - the pick hotkey with an empty main hand takes the next item of the {@link PickList} out of a bundle into the hand,
 *   and with an item in the hand puts that item into a bundle;
 * - auto restock refills the main hand from a bundle when the held stack runs out.
 * Only vanilla actions are used (bundle item selection and inventory clicks), so no server-side mod is needed.
 */
public class BundlePicker {
	private static final String MSG = Reference.MOD_ID + ".message.pick.";
	private static final int BUTTON_LEFT = 0;

	// The pick list index of the item taken last, so the next press continues with the following one
	private static int lastPickIndex = -1;

	// Auto restock: the selected slot and its stack at the end of the last tick
	private static int lastSelectedSlot = -1;
	private static ItemStack lastHandStack = ItemStack.EMPTY;

	/** Pick hotkey: put the held item into a bundle, or take the next pick list item out of one. */
	public static void onPickHotkey() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;

		if (player == null || !BundleUtils.canClick(mc, player.inventoryMenu)) {
			return;
		}

		Slot handSlot = getHandSlot(player);

		if (handSlot.hasItem()) {
			putIntoBundle(mc, player, handSlot);
		} else {
			pickNext(mc, player, handSlot);
		}

		// The hand changed on purpose, so auto restock must not react to it
		rememberHand(player);
	}

	private static void pickNext(Minecraft mc, LocalPlayer player, Slot handSlot) {
		List<Item> items = PickList.getInstance().getItems();

		if (items.isEmpty()) {
			InfoUtils.printActionbarMessage(MSG + "list_empty");
			return;
		}

		InventoryMenu menu = player.inventoryMenu;
		List<Slot> bundleSlots = BundleUtils.getBundleCandidateSlots(menu, player.getInventory());

		for (int step = 1; step <= items.size(); step++) {
			int index = Math.floorMod(lastPickIndex + step, items.size());
			Item item = items.get(index);
			BundleHit hit = findInBundles(bundleSlots, stack -> stack.is(item));

			if (hit != null && BundleUtils.takeStackOut(mc, menu, hit.bundleSlot, hit.itemIndex, handSlot)) {
				lastPickIndex = index;
				InfoUtils.printActionbarMessage(MSG + "picked", handSlot.getItem().getHoverName().getString());
				return;
			}
		}

		InfoUtils.printActionbarMessage(MSG + "nothing_found");
	}

	private static void putIntoBundle(Minecraft mc, LocalPlayer player, Slot handSlot) {
		ItemStack held = handSlot.getItem();

		// Bundles in bundles are not supported
		if (held.has(DataComponents.BUNDLE_CONTENTS) || !BundleContents.canItemBeInBundle(held)) {
			InfoUtils.printActionbarMessage(MSG + "cant_put", held.getHoverName().getString());
			return;
		}

		InventoryMenu menu = player.inventoryMenu;
		String name = held.getHoverName().getString();
		int countBefore = held.getCount();
		boolean pickedUp = false;

		for (Slot bundleSlot : BundleUtils.getBundleCandidateSlots(menu, player.getInventory())) {
			if (bundleSlot == handSlot || !hasRoom(bundleSlot.getItem())) {
				continue;
			}

			// Pick the held stack up, then left-click the bundle: as much as fits goes in
			if (!pickedUp) {
				mc.gameMode.handleInventoryMouseClick(menu.containerId, handSlot.index, BUTTON_LEFT, ClickType.PICKUP, player);
				pickedUp = true;
			}

			mc.gameMode.handleInventoryMouseClick(menu.containerId, bundleSlot.index, BUTTON_LEFT, ClickType.PICKUP, player);

			if (menu.getCarried().isEmpty()) {
				break;
			}
		}

		int left = menu.getCarried().getCount();

		// Whatever didn't fit goes back into the hand
		if (pickedUp && !menu.getCarried().isEmpty()) {
			mc.gameMode.handleInventoryMouseClick(menu.containerId, handSlot.index, BUTTON_LEFT, ClickType.PICKUP, player);
		}

		if (!pickedUp || left >= countBefore) {
			InfoUtils.printActionbarMessage(MSG + "no_room", name);
		} else {
			InfoUtils.printActionbarMessage(MSG + "put", name);
		}
	}

	/** Auto restock, every client tick: when the held stack runs out, the same item is taken out of a bundle. */
	public static void onClientTick(Minecraft mc) {
		LocalPlayer player = mc.player;

		if (player == null) {
			lastSelectedSlot = -1;
			lastHandStack = ItemStack.EMPTY;
			return;
		}

		int selected = player.getInventory().getSelectedSlot();
		ItemStack hand = player.getInventory().getItem(selected);

		if (Configs.Generic.AUTO_RESTOCK_FROM_BUNDLE.getBooleanValue()
				&& mc.screen == null
				&& selected == lastSelectedSlot
				&& !lastHandStack.isEmpty()
				&& hand.isEmpty()
				&& BundleUtils.canClick(mc, player.inventoryMenu)) {
			ItemStack wanted = lastHandStack;
			InventoryMenu menu = player.inventoryMenu;
			BundleHit hit = findInBundles(BundleUtils.getBundleCandidateSlots(menu, player.getInventory()),
					stack -> ItemStack.isSameItemSameComponents(stack, wanted));

			if (hit != null) {
				BundleUtils.takeStackOut(mc, menu, hit.bundleSlot, hit.itemIndex, getHandSlot(player));
			}
		}

		rememberHand(player);
	}

	private static void rememberHand(LocalPlayer player) {
		lastSelectedSlot = player.getInventory().getSelectedSlot();
		lastHandStack = player.getInventory().getItem(lastSelectedSlot).copy();
	}

	private static Slot getHandSlot(LocalPlayer player) {
		return player.inventoryMenu.getSlot(InventoryMenu.USE_ROW_SLOT_START + player.getInventory().getSelectedSlot());
	}

	private static boolean hasRoom(ItemStack bundle) {
		BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
		return contents != null && contents.weight().compareTo(Fraction.ONE) < 0;
	}

	@Nullable
	private static BundleHit findInBundles(List<Slot> bundleSlots, java.util.function.Predicate<ItemStack> matches) {
		for (Slot slot : bundleSlots) {
			BundleContents contents = slot.getItem().get(DataComponents.BUNDLE_CONTENTS);

			if (contents == null) {
				continue;
			}

			for (int i = 0; i < contents.size(); i++) {
				if (matches.test(contents.getItemUnsafe(i))) {
					return new BundleHit(slot, i);
				}
			}
		}

		return null;
	}

	private record BundleHit(Slot bundleSlot, int itemIndex) {}
}
