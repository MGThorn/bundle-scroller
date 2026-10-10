package io.github.mgthorn.bundlescroller.client.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;

/**
 * Taking stacks out of bundles in any open container, with vanilla actions only
 * (bundle item selection and inventory clicks), so no server-side mod is needed.
 */
public class BundleUtils {
	private static final int BUTTON_LEFT = 0;
	private static final int BUTTON_RIGHT = 1;

	/** True when clicks can be sent for this menu: it is the open one and the cursor is empty. */
	public static boolean canClick(Minecraft mc, AbstractContainerMenu menu) {
		LocalPlayer player = mc.player;

		return player != null && mc.gameMode != null && mc.getConnection() != null
				&& player.containerMenu == menu && menu.getCarried().isEmpty();
	}

	/** The menu's slots for the player's main inventory and hotbar (inventory index 0-35). */
	public static List<Slot> getPlayerInventorySlots(AbstractContainerMenu menu, Inventory inventory) {
		List<Slot> slots = new ArrayList<>();

		for (Slot slot : menu.slots) {
			if (slot.container == inventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() <= 35) {
				slots.add(slot);
			}
		}

		return slots;
	}

	/** Like {@link #getPlayerInventorySlots}, plus the offhand if the menu has a slot for it. */
	public static List<Slot> getBundleCandidateSlots(AbstractContainerMenu menu, Inventory inventory) {
		List<Slot> slots = getPlayerInventorySlots(menu, inventory);

		for (Slot slot : menu.slots) {
			if (slot.container == inventory && slot.getContainerSlot() == Inventory.SLOT_OFFHAND) {
				slots.add(slot);
			}
		}

		return slots;
	}

	/**
	 * Takes the stack at {@code itemIndex} out of the bundle in {@code bundleSlot} and puts it into the empty {@code target} slot.
	 *
	 * @return true if the stack is now in the target slot
	 */
	public static boolean takeStackOut(Minecraft mc, AbstractContainerMenu menu, Slot bundleSlot, int itemIndex, Slot target) {
		LocalPlayer player = mc.player;
		ItemStack bundle = bundleSlot.getItem();
		BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);

		if (player == null || contents == null || itemIndex < 0 || itemIndex >= contents.size() || target.hasItem()) {
			return false;
		}

		ItemStack wanted = contents.getItemUnsafe(itemIndex).copy();

		// A bundle only gives out its selected item (the first one if none is selected).
		// Selecting the index that is already selected would clear the selection, so only select when it changes.
		int selected = BundleItem.getSelectedItem(bundle);
		int effectiveSelected = selected < 0 || selected >= contents.size() ? 0 : selected;

		if (effectiveSelected != itemIndex) {
			BundleItem.toggleSelectedItem(bundle, itemIndex);
			mc.getConnection().send(new ServerboundSelectBundleItemPacket(bundleSlot.index, itemIndex));
		}

		// Right-click the bundle with an empty cursor: the selected stack goes onto the cursor
		mc.gameMode.handleInventoryMouseClick(menu.containerId, bundleSlot.index, BUTTON_RIGHT, ClickType.PICKUP, player);

		if (!ItemStack.isSameItemSameComponents(menu.getCarried(), wanted)) {
			// Something else came out: put it back into the bundle
			if (!menu.getCarried().isEmpty()) {
				mc.gameMode.handleInventoryMouseClick(menu.containerId, bundleSlot.index, BUTTON_LEFT, ClickType.PICKUP, player);
			}
			return false;
		}

		// Left-click the empty target slot: the whole stack goes there
		mc.gameMode.handleInventoryMouseClick(menu.containerId, target.index, BUTTON_LEFT, ClickType.PICKUP, player);

		return ItemStack.isSameItemSameComponents(target.getItem(), wanted);
	}
}
