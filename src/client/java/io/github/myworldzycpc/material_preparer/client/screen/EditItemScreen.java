package io.github.myworldzycpc.material_preparer.client.screen;

import io.github.myworldzycpc.material_preparer.client.config.ItemEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.mc;

public class EditItemScreen extends ScreenHasParent {
    private final ItemEntry entry;
    private EditBox searchBox;
    private EditBox countBox;
    private Item selectedItem;
    private List<Item> filteredItems;
    private List<Item> allItems;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_ROW = 8;
    private static final int ITEM_SIZE = 18;
    private static final int GRID_LEFT = 20;
    private static final int GRID_TOP = 70;

    public EditItemScreen(Screen parent, ItemEntry entry) {
        super(parent, Component.literal("Edit Item"));
        this.entry = entry;
        this.selectedItem = entry.item;
    }

    @Override
    protected void init() {
        super.init();

        allItems = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                allItems.add(item);
            }
        }
        filteredItems = new ArrayList<>(allItems);

        searchBox = new EditBox(mc.font, width / 2 - 100, 40, 200, 20, Component.literal("Search"));
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        countBox = new EditBox(mc.font, width / 2 - 50, height - 50, 100, 20, Component.literal("Count"));
        countBox.setFilter(input -> input.matches("\\d*"));
        countBox.setValue(String.valueOf(entry.count));
        addRenderableWidget(countBox);

        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> save())
                .bounds(width / 2 - 100, height - 25, 90, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> onClose())
                .bounds(width / 2 + 10, height - 25, 90, 20)
                .build());
    }

    private void onSearchChanged(String search) {
        filteredItems.clear();
        if (search.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            String lower = search.toLowerCase();
            for (Item item : allItems) {
                String id = BuiltInRegistries.ITEM.getKey(item).toString().toLowerCase();
                if (id.contains(lower)) {
                    filteredItems.add(item);
                }
            }
        }
        scrollOffset = 0;
    }

    private void save() {
        entry.item = selectedItem;
        try {
            entry.count = Integer.parseInt(countBox.getValue());
            if (entry.count < 1) entry.count = 1;
        } catch (NumberFormatException e) {
            entry.count = 1;
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(mc.font, this.title, width / 2, 10, 0xFFFFFF);
        graphics.drawString(mc.font, Component.literal("Search:"), GRID_LEFT, 45, 0xAAAAAA);
        graphics.drawString(mc.font, Component.literal("Count:"), width / 2 - 80, height - 45, 0xAAAAAA);

        drawItemGrid(graphics, mouseX, mouseY);

        if (selectedItem != null) {
            String id = BuiltInRegistries.ITEM.getKey(selectedItem).toString();
            graphics.drawCenteredString(mc.font, Component.literal("Selected: " + id), width / 2, height - 80, 0xFFFF55);
        }
    }

    private void drawItemGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int startX = GRID_LEFT;
        int startY = GRID_TOP;
        int rowsVisible = Math.max(1, (height - GRID_TOP - 100) / ITEM_SIZE);

        for (int i = scrollOffset; i < filteredItems.size(); i++) {
            int gridIndex = i - scrollOffset;
            int row = gridIndex / ITEMS_PER_ROW;
            int col = gridIndex % ITEMS_PER_ROW;
            if (row >= rowsVisible) break;

            int x = startX + col * ITEM_SIZE;
            int y = startY + row * ITEM_SIZE;

            Item item = filteredItems.get(i);
            boolean isSelected = item == selectedItem;
            boolean isHovered = mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE;

            int bgColor = isSelected ? 0x80FFFF00 : (isHovered ? 0x40FFFFFF : 0x20000000);
            graphics.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, bgColor);

            graphics.renderItem(new ItemStack(item), x + 1, y + 1);

            if (isHovered && selectedItem != item) {
                graphics.renderTooltip(mc.font, new ItemStack(item), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseY >= GRID_TOP && mouseY <= height - 100) {
            int rowsVisible = Math.max(1, (height - GRID_TOP - 100) / ITEM_SIZE);
            int maxScroll = Math.max(0, filteredItems.size() - rowsVisible * ITEMS_PER_ROW);
            scrollOffset = (int) Math.clamp(scrollOffset - scrollY * ITEMS_PER_ROW, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int startX = GRID_LEFT;
            int startY = GRID_TOP;
            int rowsVisible = Math.max(1, (height - GRID_TOP - 100) / ITEM_SIZE);

            for (int i = scrollOffset; i < filteredItems.size(); i++) {
                int gridIndex = i - scrollOffset;
                int row = gridIndex / ITEMS_PER_ROW;
                int col = gridIndex % ITEMS_PER_ROW;
                if (row >= rowsVisible) break;

                int x = startX + col * ITEM_SIZE;
                int y = startY + row * ITEM_SIZE;

                if (mouseX >= x && mouseX < x + ITEM_SIZE && mouseY >= y && mouseY < y + ITEM_SIZE) {
                    selectedItem = filteredItems.get(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
