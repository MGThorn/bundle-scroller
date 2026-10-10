package io.github.mgthorn.bundlescroller.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.InventoryOverlay;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.mgthorn.bundlescroller.Reference;
import io.github.mgthorn.bundlescroller.client.pick.PickList;

/**
 * Item picker for the pick list: a searchable grid of all items on the left,
 * and the pick list in cycle order on the right.
 * <p>
 * Grid: left-click adds the item, right-click removes it.
 * List rows: drag and drop to reorder, or the move up, move down and remove buttons.
 */
public class GuiPickListEditor extends GuiBase {
	private static final String KEY = Reference.MOD_ID + ".gui.pick_list_editor.";
	private static final int SLOT_SIZE = 18;
	private static final int CONTENT_Y = 50;
	private static final int ENTRY_HEIGHT = 20;

	@Nullable private static List<ItemStack> allItems;

	private final List<ItemStack> filteredItems = new ArrayList<>();
	private GuiTextFieldGeneric searchField;
	private String searchText = "";
	private boolean searchFocused = true;
	private int gridScroll;
	private int entryScroll;
	// The list entry being dragged to a new position, or null
	@Nullable private Item draggedItem;

	public GuiPickListEditor(@Nullable Screen parent) {
		this.title = StringUtils.translate(Reference.MOD_ID + ".gui.title.pick_list_editor");
		this.setParent(parent);
		this.updateFilter();
	}

	private static List<ItemStack> getAllItems() {
		if (allItems == null) {
			allItems = new ArrayList<>();

			for (Item item : BuiltInRegistries.ITEM) {
				if (item != Items.AIR) {
					allItems.add(new ItemStack(item));
				}
			}
		}

		return allItems;
	}

	private void updateFilter() {
		String filter = this.searchText.trim().toLowerCase(Locale.ROOT);
		this.filteredItems.clear();

		for (ItemStack stack : getAllItems()) {
			if (filter.isEmpty()
					|| stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter)
					|| BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().contains(filter)) {
				this.filteredItems.add(stack);
			}
		}

		this.gridScroll = 0;
	}

	// ---- Layout ----

	private int getGridColumns() {
		return Math.max(1, (int) ((this.getScreenWidth() - 30) * 0.55) / SLOT_SIZE);
	}

	private int getGridRows() {
		return Math.max(1, (this.getScreenHeight() - CONTENT_Y - 36) / SLOT_SIZE);
	}

	private int getGridWidth() {
		return this.getGridColumns() * SLOT_SIZE;
	}

	private int getMaxGridScroll() {
		int rows = (this.filteredItems.size() + this.getGridColumns() - 1) / this.getGridColumns();
		return Math.max(0, rows - this.getGridRows());
	}

	private int getEntriesX() {
		return 10 + this.getGridWidth() + 12;
	}

	private int getEntriesWidth() {
		return this.getScreenWidth() - this.getEntriesX() - 10;
	}

	private int getVisibleEntries() {
		return Math.max(1, (this.getScreenHeight() - CONTENT_Y - 36) / ENTRY_HEIGHT);
	}

	// ---- Widgets ----

	@Override
	public void initGui() {
		super.initGui();

		this.searchField = new GuiTextFieldGeneric(10, 28, this.getGridWidth(), 16, this.font);
		this.searchField.setValue(this.searchText);
		this.searchField.setFocused(this.searchFocused);
		this.addTextField(this.searchField, field -> {
			if (!field.getValue().equals(this.searchText)) {
				this.searchText = field.getValue();
				this.updateFilter();
			}

			return true;
		});

		this.createEntryButtons();
		this.createBottomButtons();
	}

	private void createEntryButtons() {
		List<Item> items = PickList.getInstance().getItems();
		this.entryScroll = Math.max(0, Math.min(this.entryScroll, items.size() - this.getVisibleEntries()));
		int end = Math.min(items.size(), this.entryScroll + this.getVisibleEntries());
		int right = this.getEntriesX() + this.getEntriesWidth();

		for (int i = this.entryScroll; i < end; i++) {
			Item item = items.get(i);
			int y = CONTENT_Y + (i - this.entryScroll) * ENTRY_HEIGHT + 1;

			this.addButton(new ButtonGeneric(right - 18, y, 18, 18, "x", StringUtils.translate(KEY + "remove")),
					(btn, mouseButton) -> this.changeList(() -> PickList.getInstance().remove(item)));
			this.addButton(new ButtonGeneric(right - 38, y, 18, 18, "v", StringUtils.translate(KEY + "move_down")),
					(btn, mouseButton) -> this.changeList(() -> PickList.getInstance().move(item, 1)));
			this.addButton(new ButtonGeneric(right - 58, y, 18, 18, "^", StringUtils.translate(KEY + "move_up")),
					(btn, mouseButton) -> this.changeList(() -> PickList.getInstance().move(item, -1)));
		}
	}

	private void createBottomButtons() {
		String label = StringUtils.translate(KEY + "done");
		ButtonGeneric button = new ButtonGeneric(10, this.getScreenHeight() - 26, this.getStringWidth(label) + 10, 20, label);
		this.addButton(button, (btn, mouseButton) -> GuiBase.openGui(this.getParent()));
	}

	/**
	 * Applies a change to the list and rebuilds the entry buttons, keeping the search state.
	 */
	private void changeList(Runnable change) {
		change.run();
		this.searchFocused = this.searchField.isFocused();
		this.initGui();
	}

	// ---- Input ----

	@Nullable
	private ItemStack getGridItemAt(double mouseX, double mouseY) {
		int col = (int) Math.floor((mouseX - 10) / SLOT_SIZE);
		int row = (int) Math.floor((mouseY - CONTENT_Y) / SLOT_SIZE);

		if (mouseX < 10 || mouseY < CONTENT_Y || col >= this.getGridColumns() || row >= this.getGridRows()) {
			return null;
		}

		int index = (this.gridScroll + row) * this.getGridColumns() + col;
		return index < this.filteredItems.size() ? this.filteredItems.get(index) : null;
	}

	/** The list index of the entry row under the mouse (not over its buttons), or -1. */
	private int getEntryIndexAt(double mouseX, double mouseY) {
		int x = this.getEntriesX();
		int row = (int) Math.floor((mouseY - CONTENT_Y) / ENTRY_HEIGHT);
		int index = this.entryScroll + row;

		if (mouseX < x || mouseX >= x + this.getEntriesWidth() - 60 || mouseY < CONTENT_Y
				|| row >= this.getVisibleEntries() || index >= PickList.getInstance().getItems().size()) {
			return -1;
		}

		return index;
	}

	/** Where a dragged entry would be inserted: the gap between two rows that is closest to the mouse. */
	private int getDropGapAt(double mouseY) {
		int size = PickList.getInstance().getItems().size();
		int visibleEnd = Math.min(size, this.entryScroll + this.getVisibleEntries());
		int gap = this.entryScroll + (int) Math.floor((mouseY - CONTENT_Y + ENTRY_HEIGHT / 2.0) / ENTRY_HEIGHT);

		return Math.max(this.entryScroll, Math.min(gap, visibleEnd));
	}

	@Override
	public boolean onMouseClicked(MouseButtonEvent click, boolean doubleClick) {
		if (super.onMouseClicked(click, doubleClick)) {
			return true;
		}

		// Start dragging a list entry
		int entryIndex = this.getEntryIndexAt(click.x(), click.y());

		if (entryIndex != -1 && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			this.draggedItem = PickList.getInstance().getItems().get(entryIndex);
			return true;
		}

		ItemStack stack = this.getGridItemAt(click.x(), click.y());

		if (stack != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			this.changeList(() -> PickList.getInstance().add(stack.getItem()));
			return true;
		}

		if (stack != null && click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			this.changeList(() -> PickList.getInstance().remove(stack.getItem()));
			return true;
		}

		return false;
	}

	@Override
	public boolean onMouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
		// The drag itself is drawn from the mouse position, so only keep other widgets from reacting
		return this.draggedItem != null || super.onMouseDragged(click, deltaX, deltaY);
	}

	@Override
	public boolean onMouseReleased(MouseButtonEvent click) {
		if (this.draggedItem == null) {
			return super.onMouseReleased(click);
		}

		Item item = this.draggedItem;
		this.draggedItem = null;

		// Dropped over the list: insert it at the gap under the mouse
		if (click.x() >= this.getEntriesX()) {
			int from = PickList.getInstance().getItems().indexOf(item);
			int gap = this.getDropGapAt(click.y());
			int to = gap > from ? gap - 1 : gap;
			this.changeList(() -> PickList.getInstance().moveTo(item, to));
		}

		return true;
	}

	@Override
	public boolean onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount != 0 && mouseY >= CONTENT_Y) {
			int delta = -(int) Math.signum(verticalAmount);

			if (mouseX < this.getEntriesX()) {
				this.gridScroll = Math.max(0, Math.min(this.gridScroll + delta, this.getMaxGridScroll()));
			}
			else {
				this.entryScroll += delta;
				this.changeList(() -> {});
			}

			return true;
		}

		return super.onMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	// ---- Rendering ----

	// The entry rows are drawn as part of the background, so that their buttons render on top of them
	@Override
	protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY) {
		super.drawScreenBackground(ctx, mouseX, mouseY);
		this.drawEntries(ctx);
	}

	@Override
	protected void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks) {
		ItemStack hovered = this.drawGrid(ctx, mouseX, mouseY);

		if (this.draggedItem != null) {
			this.drawDrag(ctx, mouseX, mouseY);
		} else if (hovered != null) {
			InventoryOverlay.renderStackToolTip(ctx, mouseX, mouseY, hovered);
		}
	}

	// The dragged entry follows the mouse, and a line shows where it will be inserted
	private void drawDrag(GuiContext ctx, int mouseX, int mouseY) {
		int x = this.getEntriesX();
		int width = this.getEntriesWidth();

		if (mouseX >= x) {
			int lineY = CONTENT_Y + (this.getDropGapAt(mouseY) - this.entryScroll) * ENTRY_HEIGHT - 1;
			RenderUtils.drawRect(ctx, x, lineY, width, 2, 0xFF55FF55);
		}

		ItemStack stack = new ItemStack(this.draggedItem);
		int y = mouseY - ENTRY_HEIGHT / 2;
		RenderUtils.drawRect(ctx, mouseX - 10, y, width - 60, ENTRY_HEIGHT, 0xC0505050);
		RenderUtils.drawOutline(ctx, mouseX - 10, y, width - 60, ENTRY_HEIGHT, 0xFF55FF55);
		ctx.renderItem(stack, mouseX - 8, y + 2);
		this.drawStringWithShadow(ctx, this.trimToWidth(stack.getHoverName().getString(), width - 90), mouseX + 12, y + 6, 0xFFFFFFFF);
	}

	@Nullable
	private ItemStack drawGrid(GuiContext ctx, int mouseX, int mouseY) {
		int cols = this.getGridColumns();
		int rows = this.getGridRows();
		ItemStack hovered = this.getGridItemAt(mouseX, mouseY);

		RenderUtils.drawRect(ctx, 9, CONTENT_Y - 1, cols * SLOT_SIZE + 2, rows * SLOT_SIZE + 2, 0xA0000000);

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				int index = (this.gridScroll + row) * cols + col;

				if (index >= this.filteredItems.size()) {
					break;
				}

				ItemStack stack = this.filteredItems.get(index);
				int x = 10 + col * SLOT_SIZE;
				int y = CONTENT_Y + row * SLOT_SIZE;

				if (stack == hovered) {
					RenderUtils.drawRect(ctx, x, y, SLOT_SIZE, SLOT_SIZE, 0x60FFFFFF);
				}

				// Mark items that are already in the list
				if (PickList.getInstance().contains(stack.getItem())) {
					RenderUtils.drawOutline(ctx, x, y, SLOT_SIZE, SLOT_SIZE, 0xFF55FF55);
				}

				ctx.renderItem(stack, x + 1, y + 1);
			}
		}

		String count = StringUtils.translate(KEY + "item_count", this.filteredItems.size());
		this.drawString(ctx, count, 10, CONTENT_Y + rows * SLOT_SIZE + 4, 0xFFA0A0A0);

		return hovered;
	}

	private void drawEntries(GuiContext ctx) {
		int x = this.getEntriesX();
		int width = this.getEntriesWidth();
		List<Item> items = PickList.getInstance().getItems();

		String header = StringUtils.translate(KEY + "entries_header", items.size());
		this.drawStringWithShadow(ctx, header, x, 33, 0xFFFFFFFF);

		if (items.isEmpty()) {
			this.drawString(ctx, StringUtils.translate(KEY + "empty"), x + 2, CONTENT_Y + 6, 0xFFA0A0A0);
			return;
		}

		int end = Math.min(items.size(), this.entryScroll + this.getVisibleEntries());

		for (int i = this.entryScroll; i < end; i++) {
			ItemStack stack = new ItemStack(items.get(i));
			int y = CONTENT_Y + (i - this.entryScroll) * ENTRY_HEIGHT;

			RenderUtils.drawRect(ctx, x, y, width, ENTRY_HEIGHT, (i % 2 == 0) ? 0xA0303030 : 0xA0101010);
			ctx.renderItem(stack, x + 2, y + 2);

			// The entry being dragged stays in place, greyed out
			int color = items.get(i) == this.draggedItem ? 0xFF707070 : 0xFFFFFFFF;
			String name = this.trimToWidth((i + 1) + ". " + stack.getHoverName().getString(), width - 22 - 62);
			this.drawStringWithShadow(ctx, name, x + 22, y + 6, color);
		}
	}

	private String trimToWidth(String text, int maxWidth) {
		if (this.getStringWidth(text) <= maxWidth) {
			return text;
		}

		while (!text.isEmpty() && this.getStringWidth(text + "...") > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}

		return text + "...";
	}
}
