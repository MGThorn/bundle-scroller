package io.github.mgthorn.bundlescroller.client.mixin;

import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import io.github.mgthorn.bundlescroller.client.config.Configs;

/**
 * Bundle dragging: with a bundle on the cursor,
 * - holding left-click and dragging puts the items of every non-empty slot passed over into the bundle,
 *   and releasing over an empty slot puts the bundle down there;
 * - holding right-click and dragging takes one item out of the bundle into every empty slot passed over.
 * Each step is a normal vanilla click, so the server validates it like any other click.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen<T extends AbstractContainerMenu> extends Screen {
	@Shadow @Final protected T menu;

	@Shadow @Nullable protected abstract Slot getHoveredSlot(double x, double y);

	@Shadow protected abstract void slotClicked(Slot slot, int slotId, int button, ClickType type);

	@Unique private static final int BUTTON_LEFT = 0;
	@Unique private static final int BUTTON_RIGHT = 1;

	@Unique private int bundleDragButton = -1;
	@Unique private final Set<Slot> bundleDragVisited = new HashSet<>();
	@Unique @Nullable private Slot bundleDragStartSlot;
	@Unique private boolean bundleDragClicked;

	protected MixinAbstractContainerScreen(Component title) {
		super(title);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void bundleScroller$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
		int button = event.button();

		if (!Configs.Generic.BUNDLE_DRAG.getBooleanValue()
				|| (Object) this instanceof CreativeModeInventoryScreen
				|| (button != BUTTON_LEFT && button != BUTTON_RIGHT)
				|| !this.bundleScroller$isCarryingBundle()) {
			return;
		}

		// Clicks outside of any slot (e.g. dropping the bundle) stay vanilla
		Slot slot = this.getHoveredSlot(event.x(), event.y());

		if (slot == null) {
			return;
		}

		this.bundleDragButton = button;
		this.bundleDragVisited.clear();
		this.bundleDragStartSlot = slot;
		this.bundleDragClicked = false;
		this.bundleScroller$visitSlot(slot);
		cir.setReturnValue(true);
	}

	@Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
	private void bundleScroller$onMouseDragged(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
		if (this.bundleDragButton == -1 || event.button() != this.bundleDragButton) {
			return;
		}

		Slot slot = this.getHoveredSlot(event.x(), event.y());

		if (slot != null && !this.bundleDragVisited.contains(slot)) {
			this.bundleScroller$visitSlot(slot);
		}

		cir.setReturnValue(true);
	}

	@Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
	private void bundleScroller$onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
		if (this.bundleDragButton == -1 || event.button() != this.bundleDragButton) {
			return;
		}

		Slot slot = this.getHoveredSlot(event.x(), event.y());

		if (slot != null && this.bundleScroller$isCarryingBundle()) {
			if (this.bundleDragButton == BUTTON_LEFT && !slot.hasItem()) {
				// Put the bundle down on the empty slot
				this.slotClicked(slot, slot.index, BUTTON_LEFT, ClickType.PICKUP);
			} else if (!this.bundleDragClicked && slot == this.bundleDragStartSlot) {
				// A plain click that the drag didn't handle (e.g. right-click on a filled slot) keeps its vanilla behaviour
				this.slotClicked(slot, slot.index, this.bundleDragButton, ClickType.PICKUP);
			}
		}

		this.bundleDragButton = -1;
		this.bundleDragVisited.clear();
		this.bundleDragStartSlot = null;
		cir.setReturnValue(true);
	}

	@Unique
	private void bundleScroller$visitSlot(Slot slot) {
		this.bundleDragVisited.add(slot);

		if (!this.bundleScroller$isCarryingBundle()) {
			return;
		}

		// Left only picks up from filled slots, right only puts items into empty slots
		boolean shouldClick = this.bundleDragButton == BUTTON_LEFT ? slot.hasItem() : !slot.hasItem();

		if (shouldClick) {
			this.slotClicked(slot, slot.index, this.bundleDragButton, ClickType.PICKUP);
			this.bundleDragClicked = true;
		}
	}

	@Unique
	private boolean bundleScroller$isCarryingBundle() {
		return this.menu.getCarried().getItem() instanceof BundleItem;
	}
}
