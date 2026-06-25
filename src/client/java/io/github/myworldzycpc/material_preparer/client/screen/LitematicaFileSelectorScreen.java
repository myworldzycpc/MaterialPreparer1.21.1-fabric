package io.github.myworldzycpc.material_preparer.client.screen;

import io.github.myworldzycpc.material_preparer.client.config.ItemEntry;
import io.github.myworldzycpc.material_preparer.client.util.LitematicaLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.itemList;
import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.mc;
import static io.github.myworldzycpc.material_preparer.client.config.ItemListSerializer.saveToCsv;

/**
 * Litematica 文件选择屏幕
 * 用于在游戏内选择 .litematic 文件并加载物品列表
 * 支持子目录导航
 */
public class LitematicaFileSelectorScreen extends ScreenHasParent {
    private static final Logger LOGGER = LoggerFactory.getLogger("MaterialPreparer");
    
    private File rootDirectory;
    private File currentDirectory;
    
    // 列表项：可以是文件夹或文件
    private static class ListItem {
        enum Type { FOLDER, FILE }
        Type type;
        File file;
        String displayName;
        
        ListItem(Type type, File file) {
            this.type = type;
            this.file = file;
            this.displayName = file.getName();
        }
    }
    
    private List<ListItem> listItems;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 20;
    private static final int TITLE_HEIGHT = 15;
    private static final int PATH_HEIGHT = 25;
    private static final int LIST_TOP = TITLE_HEIGHT + PATH_HEIGHT + 5;
    private static final int LIST_BOTTOM_OFFSET = 60;
    private int listWidth;
    private int listLeft;
    
    private Button parentFolderButton;

    public LitematicaFileSelectorScreen(Screen parent) {
        super(parent, Component.translatable("gui.material_preparer.select_litematica.title"));
        
        // 初始化根目录：游戏目录下的 schematics 文件夹
        rootDirectory = new File(mc.gameDirectory, "schematics");
        if (!rootDirectory.exists()) {
            // 如果不存在，尝试 Litematica mod 的 schematics 目录
            rootDirectory = new File(new File(mc.gameDirectory, "config"), "litematica/schematics");
        }
        
        currentDirectory = rootDirectory;
        loadCurrentDirectory();
    }

    /**
     * 加载当前目录的内容
     */
    private void loadCurrentDirectory() {
        listItems = new ArrayList<>();
        selectedIndex = -1;
        scrollOffset = 0;
        
        if (!currentDirectory.exists() || !currentDirectory.isDirectory()) {
            return;
        }
        
        File[] files = currentDirectory.listFiles();
        if (files == null) return;
        
        // 先添加文件夹，再添加文件
        List<ListItem> folders = new ArrayList<>();
        List<ListItem> fileItems = new ArrayList<>();
        
        for (File file : files) {
            if (file.isDirectory()) {
                folders.add(new ListItem(ListItem.Type.FOLDER, file));
            } else if (file.getName().toLowerCase().endsWith(".litematic")) {
                fileItems.add(new ListItem(ListItem.Type.FILE, file));
            }
        }
        
        // 文件夹按名称排序
        folders.sort((a, b) -> a.displayName.compareToIgnoreCase(b.displayName));
        // 文件按修改时间排序，最新的在前
        fileItems.sort((a, b) -> Long.compare(b.file.lastModified(), a.file.lastModified()));
        
        listItems.addAll(folders);
        listItems.addAll(fileItems);
    }

    @Override
    protected void init() {
        super.init();
        
        // 计算列表位置和宽度
        listWidth = Math.min(400, width - 40);
        listLeft = (width - listWidth) / 2;
        
        // 添加返回上一级按钮
        parentFolderButton = Button.builder(Component.translatable("gui.material_preparer.select_litematica.parent_folder"), 
                btn -> goToParentFolder())
                .bounds(listLeft, TITLE_HEIGHT + 3, 60, 20)
                .build();
        addRenderableWidget(parentFolderButton);
        
        // 添加加载按钮
        addRenderableWidget(Button.builder(Component.translatable("gui.material_preparer.select_litematica.load"), btn -> loadSelected())
                .bounds(width / 2 - 105, height - 35, 100, 20)
                .build());
        
        // 添加取消按钮
        addRenderableWidget(Button.builder(Component.translatable("gui.material_preparer.select_litematica.cancel"), btn -> onClose())
                .bounds(width / 2 + 5, height - 35, 100, 20)
                .build());
        
        updateParentFolderButtonState();
    }
    
    /**
     * 更新返回上一级按钮的状态
     */
    private void updateParentFolderButtonState() {
        if (parentFolderButton != null) {
            parentFolderButton.active = !currentDirectory.equals(rootDirectory);
        }
    }
    
    /**
     * 进入上一级目录
     */
    private void goToParentFolder() {
        if (currentDirectory.equals(rootDirectory)) {
            return; // 已经是根目录，不能再往上
        }
        
        File parent = currentDirectory.getParentFile();
        if (parent != null) {
            currentDirectory = parent;
            loadCurrentDirectory();
            updateParentFolderButtonState();
        }
    }
    
    /**
     * 进入子文件夹
     */
    private void enterFolder(File folder) {
        if (folder.isDirectory()) {
            currentDirectory = folder;
            loadCurrentDirectory();
            updateParentFolderButtonState();
        }
    }
    
    /**
     * 获取相对于根目录的路径字符串（以 schematics 为根）
     */
    private String getRelativePath() {
        if (currentDirectory.equals(rootDirectory)) {
            return "schematics";
        }
        
        String rootPath = rootDirectory.getAbsolutePath();
        String currentPath = currentDirectory.getAbsolutePath();
        
        if (currentPath.startsWith(rootPath)) {
            String relative = currentPath.substring(rootPath.length());
            relative = relative.replace('\\', '/');
            if (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            relative = relative.replace("/", " / ");
            return "schematics / " + relative;
        }
        
        return "schematics";
    }

    /**
     * 加载选中的文件
     */
    private void loadSelected() {
        if (selectedIndex < 0 || selectedIndex >= listItems.size()) {
            return;
        }
        
        ListItem item = listItems.get(selectedIndex);
        
        if (item.type == ListItem.Type.FOLDER) {
            // 点击的是文件夹，进入该文件夹
            enterFolder(item.file);
            return;
        }
        
        File selectedFile = item.file;
        
        try {
            List<ItemEntry> newItemList = LitematicaLoader.loadItemListFromLitematica(selectedFile);
            itemList.clear();
            itemList.addAll(newItemList);
            
            // 自动保存到 CSV
            try {
                Path configDir = FabricLoader.getInstance().getConfigDir();
                Path csvPath = configDir.resolve("material_preparer/item_list.csv");
                saveToCsv(itemList, csvPath);
            } catch (Exception e) {
                LOGGER.error("Failed to save item list CSV", e);
            }
            
            // 显示消息
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("message.material_preparer.litematica_loaded", newItemList.size(), selectedFile.getName()), false);
            }
            
            // 返回父屏幕
            onClose();
        } catch (Exception e) {
            LOGGER.error("Failed to load Litematica file", e);
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable("message.material_preparer.litematica_load_failed", e.getMessage()), false);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        
        // 绘制标题
        graphics.drawCenteredString(mc.font, this.title, width / 2, 8, 0xFFFFFF);
        
        // 绘制路径
        String pathText = getRelativePath();
        int pathX = listLeft + 70; // 留出返回按钮的位置
        int pathY = TITLE_HEIGHT + 8;
        graphics.drawString(mc.font, pathText, pathX, pathY, 0xAAAAAA);
        
        // 绘制文件列表背景
        int listBottom = height - LIST_BOTTOM_OFFSET;
        graphics.fill(listLeft, LIST_TOP, listLeft + listWidth, listBottom, 0x80000000);
        graphics.fill(listLeft, LIST_TOP, listLeft + listWidth, LIST_TOP + 1, 0xFFFFFFFF);
        graphics.fill(listLeft, listBottom - 1, listLeft + listWidth, listBottom, 0xFFFFFFFF);
        graphics.fill(listLeft, LIST_TOP, listLeft + 1, listBottom, 0xFFFFFFFF);
        graphics.fill(listLeft + listWidth - 1, LIST_TOP, listLeft + listWidth, listBottom, 0xFFFFFFFF);
        
        if (listItems.isEmpty()) {
            // 显示空提示
            graphics.drawCenteredString(mc.font, 
                    Component.translatable("gui.material_preparer.select_litematica.empty"), 
                    width / 2, (LIST_TOP + listBottom) / 2 - 10, 0xAAAAAA);
            return;
        }
        
        // 绘制列表项
        int visibleCount = (listBottom - LIST_TOP) / ITEM_HEIGHT;
        
        for (int i = scrollOffset; i < listItems.size(); i++) {
            int displayIndex = i - scrollOffset;
            if (displayIndex >= visibleCount) break;
            
            int y = LIST_TOP + displayIndex * ITEM_HEIGHT;
            ListItem item = listItems.get(i);
            boolean isSelected = i == selectedIndex;
            boolean isHovered = mouseX >= listLeft && mouseX < listLeft + listWidth && 
                               mouseY >= y && mouseY < y + ITEM_HEIGHT;
            
            // 绘制背景
            int bgColor;
            if (isSelected) {
                bgColor = 0x80FFFF00;
            } else if (isHovered) {
                bgColor = 0x40FFFFFF;
            } else {
                bgColor = (displayIndex % 2 == 0) ? 0x20000000 : 0x10000000;
            }
            graphics.fill(listLeft + 2, y, listLeft + listWidth - 2, y + ITEM_HEIGHT - 1, bgColor);
            
            // 绘制图标和名称
            String prefix = item.type == ListItem.Type.FOLDER ? "📂 " : "";
            String displayName = prefix + item.displayName;
            
            // 如果名称太长，截断
            int maxWidth = listWidth - 20;
            if (mc.font.width(displayName) > maxWidth) {
                displayName = mc.font.plainSubstrByWidth(displayName, maxWidth - 10) + "...";
            }
            
            int textColor;
            if (isSelected) {
                textColor = 0x000000;
            } else if (item.type == ListItem.Type.FOLDER) {
                textColor = 0x55FFFF; // 文件夹用青色
            } else {
                textColor = 0xFFFFFF; // 文件用白色
            }
            
            graphics.drawString(mc.font, displayName, listLeft + 10, y + 6, textColor);
        }
        
        // 绘制滚动条（如果需要）
        int maxScroll = Math.max(0, listItems.size() - visibleCount);
        if (maxScroll > 0) {
            int scrollbarX = listLeft + listWidth - 6;
            int scrollbarHeight = listBottom - LIST_TOP;
            int thumbHeight = Math.max(20, scrollbarHeight * visibleCount / listItems.size());
            int thumbY = LIST_TOP + (scrollOffset * (scrollbarHeight - thumbHeight) / maxScroll);
            graphics.fill(scrollbarX, LIST_TOP, scrollbarX + 4, listBottom, 0x40000000);
            graphics.fill(scrollbarX, thumbY, scrollbarX + 4, thumbY + thumbHeight, 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listLeft && mouseX <= listLeft + listWidth && 
            mouseY >= LIST_TOP && mouseY <= height - LIST_BOTTOM_OFFSET) {
            int visibleCount = (height - LIST_BOTTOM_OFFSET - LIST_TOP) / ITEM_HEIGHT;
            int maxScroll = Math.max(0, listItems.size() - visibleCount);
            scrollOffset = (int) Math.clamp(scrollOffset - scrollY, 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listBottom = height - LIST_BOTTOM_OFFSET;
            if (mouseX >= listLeft && mouseX <= listLeft + listWidth && 
                mouseY >= LIST_TOP && mouseY <= listBottom) {
                
                int visibleCount = (listBottom - LIST_TOP) / ITEM_HEIGHT;
                int clickedIndex = scrollOffset + (int) ((mouseY - LIST_TOP) / ITEM_HEIGHT);
                
                if (clickedIndex >= 0 && clickedIndex < listItems.size() && 
                    (clickedIndex - scrollOffset) < visibleCount) {
                    
                    ListItem item = listItems.get(clickedIndex);
                    
                    if (item.type == ListItem.Type.FOLDER) {
                        // 双击文件夹进入？或者单击？
                        // 先单击选中，再点击加载按钮进入？
                        // 或者直接单击进入？
                        // 为了方便，单击文件夹直接进入
                        enterFolder(item.file);
                    } else {
                        // 文件单击选中
                        selectedIndex = clickedIndex;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseDoubleClick(double mouseX, double mouseY, int button) {
        // 双击文件直接加载
        if (button == 0) {
            int listBottom = height - LIST_BOTTOM_OFFSET;
            if (mouseX >= listLeft && mouseX <= listLeft + listWidth && 
                mouseY >= LIST_TOP && mouseY <= listBottom) {
                
                int visibleCount = (listBottom - LIST_TOP) / ITEM_HEIGHT;
                int clickedIndex = scrollOffset + (int) ((mouseY - LIST_TOP) / ITEM_HEIGHT);
                
                if (clickedIndex >= 0 && clickedIndex < listItems.size() && 
                    (clickedIndex - scrollOffset) < visibleCount) {
                    
                    ListItem item = listItems.get(clickedIndex);
                    
                    if (item.type == ListItem.Type.FILE) {
                        selectedIndex = clickedIndex;
                        loadSelected();
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
