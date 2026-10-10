package io.github.mgthorn.bundlescroller.client.pick;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.data.json.JsonUtils;
import io.github.mgthorn.bundlescroller.Reference;

/**
 * The ordered list of items that the pick hotkey cycles through.
 * Stored as a JSON array of item ids in {@code config/bundle-scroller/pick_list.json}.
 */
public class PickList {
	private static final PickList INSTANCE = new PickList();

	private final List<Item> items = new ArrayList<>();
	private boolean loaded;

	public static PickList getInstance() {
		return INSTANCE;
	}

	private static Path getFile() {
		return FileUtils.getConfigDirectory().resolve(Reference.MOD_ID).resolve("pick_list.json");
	}

	public List<Item> getItems() {
		this.ensureLoaded();
		return Collections.unmodifiableList(this.items);
	}

	public boolean contains(Item item) {
		return this.getItems().contains(item);
	}

	public void add(Item item) {
		this.ensureLoaded();

		if (!this.items.contains(item) && item != Items.AIR) {
			this.items.add(item);
			this.save();
		}
	}

	public void remove(Item item) {
		this.ensureLoaded();

		if (this.items.remove(item)) {
			this.save();
		}
	}

	/** Moves the item one place up (-1) or down (+1) in the list. */
	public void move(Item item, int direction) {
		this.ensureLoaded();
		int index = this.items.indexOf(item);
		int newIndex = index + direction;

		if (index != -1 && newIndex >= 0 && newIndex < this.items.size()) {
			Collections.swap(this.items, index, newIndex);
			this.save();
		}
	}

	/** Moves the item to {@code newIndex} (its position after the move), for drag and drop. */
	public void moveTo(Item item, int newIndex) {
		this.ensureLoaded();
		int index = this.items.indexOf(item);
		newIndex = Math.max(0, Math.min(newIndex, this.items.size() - 1));

		if (index != -1 && index != newIndex) {
			this.items.remove(index);
			this.items.add(newIndex, item);
			this.save();
		}
	}

	private void ensureLoaded() {
		if (this.loaded) {
			return;
		}

		this.loaded = true;
		JsonElement element = JsonUtils.parseJsonFile(getFile());

		if (element != null && element.isJsonArray()) {
			for (JsonElement entry : element.getAsJsonArray()) {
				Identifier id = Identifier.tryParse(entry.getAsString());

				if (id != null) {
					BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> {
						if (item != Items.AIR && !this.items.contains(item)) {
							this.items.add(item);
						}
					});
				}
			}
		}
	}

	private void save() {
		JsonArray array = new JsonArray();

		for (Item item : this.items) {
			array.add(BuiltInRegistries.ITEM.getKey(item).toString());
		}

		Path file = getFile();
		FileUtils.createDirectoriesIfMissing(file.getParent());
		JsonUtils.writeJsonToFile(array, file);
	}
}
