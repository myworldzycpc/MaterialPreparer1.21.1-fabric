package io.github.myworldzycpc.material_preparer.client.config;

import net.minecraft.world.item.Item;

public record ItemEntry(Item item, int count) {
    public ItemEntry {
        if (count < 1) count = 1;
    }

    public ItemEntry withItem(Item item) {
        return new ItemEntry(item, this.count);
    }

    public ItemEntry withCount(int count) {
        return new ItemEntry(this.item, count);
    }
}
