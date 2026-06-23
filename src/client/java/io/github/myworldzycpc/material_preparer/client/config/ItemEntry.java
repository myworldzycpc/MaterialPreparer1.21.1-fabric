package io.github.myworldzycpc.material_preparer.client.config;

import net.minecraft.world.item.Item;

public class ItemEntry {
    public Item item = null;
    public int count = 1;

    public ItemEntry(Item item, int count) {
        this.item = item;
        this.count = count;
    }
}
