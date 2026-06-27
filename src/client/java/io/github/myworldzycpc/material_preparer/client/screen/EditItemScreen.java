package io.github.myworldzycpc.material_preparer.client.screen;

import dev.isxander.yacl3.api.ListOptionEntry;
import io.github.myworldzycpc.material_preparer.client.config.ItemEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.mc;

public class EditItemScreen extends ScreenHasParent {
    private final ListOptionEntry<ItemEntry> option;
    private EditBox searchBox;
    private EditBox countBox;
    private Item selectedItem;
    private int selectedCount;
    private List<Item> filteredItems;
    private List<Item> allItems;
    private int scrollOffset = 0;
    private static final int ITEMS_PER_ROW = 8;
    private static final int ITEM_SIZE = 18;
    private static final int GRID_TOP = 70;
    private int gridLeft; // 动态计算的网格左边界（居中）

    public EditItemScreen(Screen parent, ListOptionEntry<ItemEntry> option) {
        super(parent, Component.translatable("gui.material_preparer.edit_item.title"));
        this.option = option;
        ItemEntry entry = option.pendingValue();
        this.selectedItem = entry.item();
        this.selectedCount = entry.count();
    }

    @Override
    protected void init() {
        super.init();

        // 计算网格居中位置
        int gridWidth = ITEMS_PER_ROW * ITEM_SIZE;
        gridLeft = (width - gridWidth) / 2;

        allItems = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != Items.AIR) {
                allItems.add(item);
            }
        }
        filteredItems = new ArrayList<>(allItems);

        searchBox = new EditBox(mc.font, width / 2 - 100, 40, 200, 20, Component.translatable("gui.material_preparer.edit_item.search"));
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);

        countBox = new EditBox(mc.font, width / 2 - 50, height - 50, 100, 20, Component.translatable("gui.material_preparer.edit_item.count"));
        countBox.setResponder(val -> {
            String expected = val.replaceAll("[^0-9]", "");
            if (!val.equals(expected)) {
                countBox.setValue(expected);
            }
        });
        countBox.setValue(String.valueOf(selectedCount));
        addRenderableWidget(countBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.material_preparer.edit_item.save"), btn -> save())
                .bounds(width / 2 - 100, height - 25, 90, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.material_preparer.edit_item.cancel"), btn -> onClose())
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
        int count;
        try {
            count = Integer.parseInt(countBox.getValue());
            if (count < 1) count = 1;
        } catch (NumberFormatException e) {
            count = 1;
        }
        ItemEntry newEntry = new ItemEntry(selectedItem, count);
        option.requestSet(newEntry);
        onClose();
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.centeredText(mc.font, this.title, width / 2, 10, 0xFFFFFF);
        graphics.text(mc.font, Component.translatable("gui.material_preparer.edit_item.search_label"), searchBox.getX() - mc.font.width(Component.translatable("gui.material_preparer.edit_item.search_label")) - 5, 45, 0xAAAAAA);
        graphics.text(mc.font, Component.translatable("gui.material_preparer.edit_item.count_label"), width / 2 - 80, height - 45, 0xAAAAAA);

        drawItemGrid(graphics, mouseX, mouseY);

        if (selectedItem != null) {
            String id = BuiltInRegistries.ITEM.getKey(selectedItem).toString();
            graphics.centeredText(mc.font, Component.translatable("gui.material_preparer.edit_item.selected", id), width / 2, height - 80, 0xFFFF55);
        }
    }

    private void drawItemGrid(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int startX = gridLeft;
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

            graphics.item(new ItemStack(item), x + 1, y + 1);

            if (isHovered && selectedItem != item) {
                graphics.itemDecorations(mc.font, new ItemStack(item), mouseX, mouseY);
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
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        double mouseX = event.x();
        double mouseY = event.y();
        if (button == 0) {
            int startX = gridLeft;
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
        return super.mouseClicked(event, doubleClick);
    }
}
